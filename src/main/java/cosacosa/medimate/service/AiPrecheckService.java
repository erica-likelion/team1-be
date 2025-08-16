package cosacosa.medimate.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosacosa.medimate.config.PrecheckAiProperties;
import cosacosa.medimate.dto.PrecheckRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPrecheckService {
    private final WebClient openAiWebClient;
    private final PrecheckAiProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public AiResult generateTitleAndContent(PrecheckRequestDto req) {
        try {
            // ✅ system prompt
            String systemPrompt = """
                You are a clinical intake assistant for pre-visit triage.
                Follow these rules exactly:
                1. Output MUST be a valid JSON object with exactly two keys: "title" and "content".
                2. "title": A concise one-line title summarizing the patient's symptoms and context.
                3. "content": The patient's precheck information rewritten in Korean,
                   organized in a clear, polite, and clinically neutral style (no diagnosis or treatment).
                4. Do NOT include any keys other than "title" and "content".
                5. Do NOT include any text outside the JSON object.
                """;

            // ✅ user prompt
            String userPrompt = String.format("""
                {
                  "language": "%s",
                  "name": "%s",
                  "age": %s,
                  "nationality": "%s",
                  "gender": "%s",
                  "description": "%s"
                }
                """,
                    safe(req.getLanguage()), safe(req.getName()), req.getAge(),
                    safe(req.getNationality()), safe(req.getGender()), safe(req.getDescription())
            );

            // ✅ 요청 body를 Map으로 구성하여 안전하게 JSON 변환
            Map<String, Object> requestBody = Map.of(
                    "model", props.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", props.getTemperature()
            );

            String body = om.writeValueAsString(requestBody);
            System.out.println("📦 OpenAI 요청 Body:\n" + body);

            // ✅ OpenAI 호출
            String raw = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("📨 OpenAI 응답 Raw:\n" + raw);

            // ✅ 응답 파싱
            ChatResponse resp = om.readValue(raw, ChatResponse.class);
            String contentText = (resp != null && resp.choices != null && !resp.choices.isEmpty())
                    ? resp.choices.get(0).message.content
                    : "";

            System.out.println("📄 contentText (AI가 반환한 메시지):\n" + contentText);

            if (contentText == null || contentText.isBlank()) return new AiResult("", "");

            JsonNode json = om.readTree(contentText);
            String title = getText(json, "title");
            String content = getText(json, "content");

            System.out.println("✅ 파싱된 title: " + title);
            System.out.println("✅ 파싱된 content: " + content);

            return new AiResult(title, content);

        } catch (Exception e) {
            System.out.println("❌ AI 응답 파싱 중 오류 발생:");
            e.printStackTrace();
            return new AiResult("", "");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String getText(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull() ? node.get(key).asText("") : "";
    }

    public record AiResult(String title, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        public String content;
    }
}
