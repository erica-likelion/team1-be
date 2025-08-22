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
    private static final String DEFAULT_GENDER_M_KR = "남성";
    private static final String DEFAULT_GENDER_F_KR = "여성";

    public AiResultFull generateTitleAndContent(PrecheckRequestDto req) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(req);
        JsonNode json = callOpenAi(systemPrompt, userPrompt);

        String title = getText(json, "title");
        String symptomParagraph = getText(json, "symptomParagraph").replace("\\n", " ").replace("\n", " ").trim();
        String koreanSymptomParagraph = getText(json, "koreanSymptomParagraph").replace("\\n", " ").replace("\n", " ").trim();

        // visitPurpose는 이제 AI가 아닌 코드로 직접 설정
        String visitPurpose = "english".equals(req.getLanguage()) ? DEFAULT_VISIT_PURPOSE_EN : DEFAULT_VISIT_PURPOSE_KR;

        // --- content 필드 조합 ---
        String content = String.format("Name: %s\nAge: %s\nGender: %s\nNationality: %s\nVisit Purpose: %s\nSymptoms: %s",
                req.getName(), req.getAge(), req.getGender(), req.getNationality(), DEFAULT_VISIT_PURPOSE_EN, symptomParagraph);

        // --- koreanContent 필드 조합 ---
        String koreanContent = String.format("이름: %s\n나이: %s세\n성별: %s\n국적: %s\n방문 목적: %s\n증상: %s",
                req.getName(), req.getAge(), getKoreanGender(req.getGender()), getKoreanNationality(req.getNationality()), DEFAULT_VISIT_PURPOSE_KR, koreanSymptomParagraph);

        return new AiResultFull(title, visitPurpose, content, koreanContent);
    }

    private String buildSystemPrompt() {
        return """
        You are a clinical intake assistant for pre-visit triage.

        Return ONLY a valid JSON object with exactly these keys:
          - "title": one-line summary of the patient's symptoms/context (string)
          - "symptomParagraph": patient's symptoms in a single, coherent paragraph, in the patient's original language.
          - "koreanSymptomParagraph": patient's symptoms in a single, coherent paragraph, in Korean.

        ======================
        SYMPTOMS PARAGRAPH COMPOSITION:
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
        - The entire content for "symptomParagraph" and "koreanSymptomParagraph" should be a single, continuous paragraph without any newlines.

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

    // 성별을 한국어로 변환하는 헬퍼 메서드 추가
    private static String getKoreanGender(String gender) {
        if ("M".equalsIgnoreCase(gender)) {
            return DEFAULT_GENDER_M_KR;
        } else if ("F".equalsIgnoreCase(gender)) {
            return DEFAULT_GENDER_F_KR;
        }
        return gender;
    }

    // 국적을 한국어로 변환하는 헬퍼 메서드 추가
    private static String getKoreanNationality(String nationality) {
        return switch (nationality.toLowerCase()) {
            case "usa" -> "미국";
            case "korea" -> "한국";
            case "uk" -> "영국";
            case "australia" -> "호주";
            case "canada" -> "캐나다";
            case "newzealand" -> "뉴질랜드";
            case "china" -> "중국";
            case "taiwan" -> "대만";
            case "hongkong" -> "홍콩";
            default -> nationality;
        };
    }

    public record AiResultFull(String title, String visitPurpose, String content, String koreanContent) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse { public List<Choice> choices; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice { public Message message; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message { public String content; }
}