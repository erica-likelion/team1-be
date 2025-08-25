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
                    "symptoms", "Symptoms"
            ),
            "ko", Map.of(
                    "name", "이름",
                    "age", "나이",
                    "gender", "성별",
                    "nationality", "국적",
                    "symptoms", "증상"
            ),
            "zh", Map.of(
                    "name", "姓名",
                    "age", "年龄",
                    "gender", "性别",
                    "nationality", "国籍",
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

        String title = getText(json, "title");
        String symptomParagraph = getText(json, "symptomParagraph").replace("\\n", " ").replace("\n", " ").trim();
        String koreanSymptomParagraph = getText(json, "koreanSymptomParagraph").replace("\\n", " ").replace("\n", " ").trim();

        String content = String.format("%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s",
                getLabel(language, "name"), getNameByLanguage(req.getName(), language),
                getLabel(language, "age"), req.getAge(),
                getLabel(language, "gender"), getGenderByLanguage(req.getGender(), language),
                getLabel(language, "nationality"), nationality,
                getLabel(language, "symptoms"), symptomParagraph);

        String koreanContent = String.format("%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s",
                getLabel("ko", "name"), getKoreanName(req.getName()),
                getLabel("ko", "age"), req.getAge(),
                getLabel("ko", "gender"), getKoreanGender(req.getGender()),
                getLabel("ko", "nationality"), translatedNationality,
                getLabel("ko", "symptoms"), koreanSymptomParagraph);

        return new AiResultFull(title, content, koreanContent);
    }

    private String buildSystemPrompt() {
        return """
    당신은 사전 문진을 위한 임상 접수 보조자입니다.
    
    정확히 다음의 키만 가진 유효한 JSON 객체를 반환하세요(추가 키 금지):
    - "title": 환자의 증상/상황을 한 줄로 요약 (문자열, 12단어 이내, 입력 언어로)
    - "symptomParagraph": 환자가 작성한 원래 언어(입력 언어)로 된 증상 단락 (줄바꿈 없이 하나의 문단)
    - "koreanSymptomParagraph": 위 증상을 한국어로 자연스럽게 번역한 단락 (줄바꿈 없이 하나의 문단)
    - "detectedLanguage": 환자가 작성한 증상의 언어를 소문자 ISO 639-1 코드로 표기 (예: "en", "ko", "zh")
    - "nationality": 환자의 국적을 환자 입력 언어로 표기(다른 언어로 제공되었더라도 입력 언어로 정규화)
    - "translatedNationality": 환자의 국적을 한국어로 표기
    
    ======================
    언어 규칙:
    - 입력 언어를 감지하여 "title", "symptomParagraph", "nationality"는 **반드시 입력 언어**로 작성합니다.
    - "koreanSymptomParagraph"와 "translatedNationality"는 **항상 한국어**로 작성합니다.
    - "detectedLanguage"는 ISO 639-1 코드만 반환합니다(예: "en", "ko", "zh").
    
    ======================
    증상 단락 작성 규칙(가능한 경우 아래 순서를 모두 반영하여 한 문단으로 작성):
    1) 발현 시점과 지속 기간
    2) 위치
    3) 성격/특징(quality)
    4) 중증도(제공된 경우만, 임의로 만들지 말 것)
    5) 함께 보고된 연관 증상(실제로 보고된 것만)
    6) 변화를 주는 요인(시도한 약물 및 반응)
    7) 경과/진행 양상
    8) 기능적 영향(언급된 경우)
    - 제공된 모든 정보를 빠짐없이 포함하고 구체적으로 기술합니다.
    - 문단 내 줄바꿈을 넣지 말고 한 문장 흐름으로 자연스럽게 쓰세요.
    - 상대적 날짜가 있다면 가능한 범위에서 절대적 날짜/기간으로 명확히 정규화하되, 추정이나 창작은 하지 않습니다.
    
    ======================
    정규화 규칙:
    - 임상적이고 중립적인 톤을 유지하며, 진단/치료 제안은 하지 않습니다.
    - 입력에 없는 정보는 추가하지 않습니다.
    - 국적 표기가 다른 언어로 주어지면 입력 언어로 변환하여 "nationality"에 넣고, 그 한국어 번역을 "translatedNationality"에 넣습니다.
    - 국적 정보가 없거나 알 수 없으면: "nationality"에는 입력 언어로 "Not provided" (영어 입력일 때), "제공되지 않음"(한국어 입력일 때), "未提供"(중국어 입력일 때) 등 입력 언어에 맞는 부재 표기를 사용하고, "translatedNationality"에는 "제공되지 않음"을 사용합니다.
    
    ======================
    제목(title) 규칙:
    - 간결하게(12단어 이내), 주요 증상과 특이 맥락 반영, 입력 언어로 작성.
    
    ======================
    전역 규칙:
    - JSON 외의 설명, 마크다운, 코드블록 등을 출력하지 마세요.
    - 지정된 키 외 추가 키를 절대 포함하지 마세요.
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
              "nationality": "%s",
              "description": "%s"
            }
            """,
                safe(req.getName()),
                req.getAge(),
                safe(req.getGender()),
                safe(req.getNationality()),
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

    public record AiResultFull(String title, String content, String koreanContent) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse { public List<Choice> choices; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice { public Message message; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message { public String content; }
}