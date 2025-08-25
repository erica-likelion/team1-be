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

    private static final String GENDER_M_KR = "남성";
    private static final String GENDER_F_KR = "여성";

    private static final Map<String, Map<String, String>> LABELS = Map.of(
            "en", Map.of(
                    "name", "Name",
                    "age", "Age",
                    "gender", "Gender",
                    "nationality", "Nationality",
                    "visitPurpose", "Symptom consultation",
                    "symptoms", "Symptoms"
            ),
            "ko", Map.of(
                    "name", "이름",
                    "age", "나이",
                    "gender", "성별",
                    "nationality", "국적",
                    "visitPurpose", "증상 목적",
                    "symptoms", "증상"
            ),
            "zh", Map.of(
                    "name", "姓名",
                    "age", "年龄",
                    "gender", "性别",
                    "nationality", "国籍",
                    "visitPurpose", "就诊目的",
                    "symptoms", "症状"
            )
    );

    public AiResultFull generateTitleAndContent(PrecheckRequestDto req) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(req);
        JsonNode json = callOpenAi(systemPrompt, userPrompt);

        String language = getText(json, "detectedLanguage");
        String nationality = getText(json, "nationality");
        String translatedNationality = getText(json, "translatedNationality");
        String translatedVisitPurpose = getText(json, "translatedVisitPurpose");

        String title = getText(json, "title");
        String symptomParagraph = getText(json, "symptomParagraph").replace("\\n", " ").replace("\n", " ").trim();
        String koreanSymptomParagraph = getText(json, "koreanSymptomParagraph").replace("\\n", " ").replace("\n", " ").trim();

        String content = String.format("%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s",
                getLabel(language, "name"), getNameByLanguage(req.getName(), language),
                getLabel(language, "age"), req.getAge(),
                getLabel(language, "gender"), getGenderByLanguage(req.getGender(), language),
                getLabel(language, "nationality"), nationality,
                getLabel(language, "visitPurpose"), translatedVisitPurpose,
                getLabel(language, "symptoms"), symptomParagraph);

        String koreanContent = String.format("%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s",
                getLabel("ko", "name"), getKoreanName(req.getName()),
                getLabel("ko", "age"), req.getAge(),
                getLabel("ko", "gender"), getKoreanGender(req.getGender()),
                getLabel("ko", "nationality"), translatedNationality,
                getLabel("ko", "visitPurpose"), getLabel("ko", "visitPurpose"),
                getLabel("ko", "symptoms"), koreanSymptomParagraph);

        return new AiResultFull(title, translatedVisitPurpose, content, koreanContent);
    }

    private String buildSystemPrompt() {
        return """
        You are a clinical intake assistant for pre-visit triage.

        Return ONLY a valid JSON object with exactly these keys:
          - "title": one-line summary of the patient's symptoms/context (string)
          - "symptomParagraph": patient's symptoms in a single, coherent paragraph, in the patient's original language.
          - "koreanSymptomParagraph": patient's symptoms in a single, coherent paragraph, in Korean.
          - "detectedLanguage": the language of the patient's symptoms in a lowercase ISO 639-1 code (e.g., 'en', 'ko', 'zh').
          - "nationality": the patient's nationality, translated into the patient's original language.
          - "translatedNationality": the patient's nationality, translated into Korean.
          - "translatedVisitPurpose": the visitPurpose, translated into the patient's original language.
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

    private String getLabel(String lang, String key) {
        return LABELS.getOrDefault(lang, LABELS.get("en")).get(key);
    }

    private String getNameByLanguage(String name, String lang) {
        if ("ko".equals(lang)) {
            return getKoreanName(name);
        }
        return name;
    }

    private String getGenderByLanguage(String gender, String lang) {
        if ("ko".equals(lang)) {
            return getKoreanGender(gender);
        } else if ("zh".equals(lang)) {
            if ("M".equalsIgnoreCase(gender)) return "男性";
            if ("F".equalsIgnoreCase(gender)) return "女性";
        }
        return gender;
    }

    private String getKoreanName(String name) {
        return name;
    }
    private static String getKoreanGender(String gender) {
        if ("M".equalsIgnoreCase(gender)) {
            return GENDER_M_KR;
        } else if ("F".equalsIgnoreCase(gender)) {
            return GENDER_F_KR;
        }
        return gender;
    }

    private String buildUserPrompt(PrecheckRequestDto req) {
        return String.format("""
            {
              "name": "%s",
              "age": %s,
              "gender": "%s",
              "description": "%s",
              "visitPurpose": "%s"
            }
            """,
                safe(req.getName()),
                req.getAge(),
                safe(req.getGender()),
                escapeQuotes(safe(req.getDescription())),
                safe(req.getVisitPurpose())
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