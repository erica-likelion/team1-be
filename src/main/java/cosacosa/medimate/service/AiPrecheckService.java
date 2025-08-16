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
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(req);
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt);
            String body = om.writeValueAsString(requestBody);

            String raw = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String contentText = extractContentText(raw);
            JsonNode json = parseContentToJson(contentText);
            String title = getText(json, "title");
            String content = getText(json, "content");

            return new AiResult(title, content);

        } catch (Exception e) {
            return new AiResult("", "");
        }
    }

    private String buildSystemPrompt() {
        return """
        You are a clinical intake assistant for pre-visit triage.
        Respond ONLY with a valid JSON object with exactly two keys: "title" and "content".
        Do NOT add any explanation, prefix, markdown, or surrounding text.
        Return just the raw JSON. Example:
        {
          "title": "...",
          "content": "..."
        }
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
                safe(req.getLanguage()), safe(req.getName()), req.getAge(),
                safe(req.getNationality()), safe(req.getGender()), safe(req.getDescription())
        );
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        return Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", props.getTemperature()
        );
    }

    private String extractContentText(String raw) throws Exception {
        ChatResponse resp = om.readValue(raw, ChatResponse.class);
        if (resp == null || resp.choices == null || resp.choices.isEmpty()) return "";
        return resp.choices.get(0).message.content;
    }

    private JsonNode parseContentToJson(String contentText) throws Exception {
        return om.readTree(contentText);
    }

    private static String getText(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull() ? node.get(key).asText("") : "";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static class AiResult {
        private final String title;
        private final String content;

        public AiResult(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

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
