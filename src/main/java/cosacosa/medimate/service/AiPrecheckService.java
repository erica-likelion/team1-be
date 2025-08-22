package cosacosa.medimate.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosacosa.medimate.config.PrecheckAiProperties;
import cosacosa.medimate.dto.PrecheckRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPrecheckService {
    private final WebClient openAiWebClient;
    private final PrecheckAiProperties props;
    private final ObjectMapper om = new ObjectMapper();
    private static final String DEFAULT_VISIT_PURPOSE_EN = "Symptom consultation";
    private static final String DEFAULT_VISIT_PURPOSE_KR = "증상 상담";

    public AiResultFull generateTitleAndContent(PrecheckRequestDto req) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(req);
        JsonNode json = callOpenAi(systemPrompt, userPrompt);

        String title = getText(json, "title");
        String content = getText(json, "content").replace("\n", " ");
        String koreanContent = getText(json, "koreanContent").replace("\n", " ");

        // AI 응답 대신, 코드에서 직접 언어에 맞는 고정 값을 설정합니다.
        String visitPurpose = "english".equals(req.getLanguage()) ? DEFAULT_VISIT_PURPOSE_EN : DEFAULT_VISIT_PURPOSE_KR;

        return new AiResultFull(title, visitPurpose, content, koreanContent);
    }

    private String buildSystemPrompt() {
        return """
        You are a clinical intake assistant for pre-visit triage.

        Return ONLY a valid JSON object with exactly these keys:
          - "title": one-line summary of the patient's symptoms/context (string)
          - "content": patient's symptoms in a single, coherent paragraph, in the patient's original language.
          - "koreanContent": patient's symptoms in a single, coherent paragraph, in Korean.

        ======================
        SYMPTOMS PARAGRAPH COMPOSITION (both English & Korean):
        Write a single, well-structured paragraph in this order when information exists:
          1) Onset & duration
          2) Location
          3) Character/quality
          4) Severity if provided (do NOT invent)
          5) Associated symptoms actually reported
          6) Modifying factors (medications tried and response)
          7) Course/progression
          8) Functional impact if stated
        - Each symptom should be described in detail and all provided information should be included.
        - Make sure the output is as comprehensive and detailed as possible.
        - Do NOT include patient information (Name, Age, Gender, etc.) or the visit purpose in the content or koreanContent fields.
        - The entire content for "content" and "koreanContent" should be a single, continuous paragraph without any newlines.

        ======================
        NORMALIZATION / STYLE:
          - Clinical, neutral tone; no diagnosis or recommendations.
          - Convert relative dates if needed.
          - Do NOT add data not present.

        ======================
        TITLE RULES:
          - Concise (<= 12 words), reflect main symptoms + notable context.

        ======================
        GLOBAL RULES:
          - No markdown/explanations outside JSON.
          - No extra keys beyond the specified.
        """;
    }

    private String buildUserPrompt(PrecheckRequestDto req) {
        return String.format("""
            {
              "language": "%s",
              "name": "%s",
              "age": %s,
              "nationality": "%s",
              "gender": "%s",
              "description": "%s"
            }
            """,
                safe(req.getLanguage()),
                safe(req.getName()),
                req.getAge(),
                safe(req.getNationality()),
                safe(req.getGender()),
                escapeQuotes(safe(req.getDescription()))
        );
    }

    private JsonNode callOpenAi(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", props.getTemperature(),
                "response_format", Map.of(
                        "type", "json_object"
                )
        );

        try {
            String raw = openAiWebClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ChatResponse resp = om.readValue(raw, ChatResponse.class);
            if (resp == null || resp.choices == null || resp.choices.isEmpty() || resp.choices.get(0).message == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답 비어 있음");
            }
            String contentText = resp.choices.get(0).message.content;
            if (isBlank(contentText)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답 content 비어 있음");
            }
            return om.readTree(contentText);

        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI API 호출 실패: " + e.getStatusCode().value()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI 응답 처리 실패");
        }
    }

    private static String getText(JsonNode node, String key) {
        return node != null && node.has(key) && !node.get(key).isNull()
                ? node.get(key).asText("")
                : "";
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String escapeQuotes(String s) { return s.replace("\"", "\\\""); }

    public record AiResultFull(String title, String visitPurpose, String content, String koreanContent) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse { public List<Choice> choices; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice { public Message message; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message { public String content; }
}