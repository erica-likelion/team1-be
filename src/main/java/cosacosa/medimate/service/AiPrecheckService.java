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

    public AiResultFull generateTitleAndContent(PrecheckRequestDto req) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(req);

            JsonNode json = callOpenAi(systemPrompt, userPrompt);

            String title = getText(json, "title");
            String content = getText(json, "content");
            String koreanContent = getText(json, "koreanContent");

            return new AiResultFull(title, content, koreanContent);
        } catch (Exception e) {
            return new AiResultFull("", "", "");
        }
    }

    private String buildSystemPrompt() {
        return """
        You are a clinical intake assistant for pre-visit triage.
        Respond ONLY with a valid JSON object with exactly three keys: "title", "content", "koreanContent".
        - "title": A concise one-line summary of the patient's symptoms/context.
        - "content": Rewrite the patient's precheck info in a clear, polite, clinically neutral style in the input language.
        - "koreanContent": A faithful Korean rendition of "content".
        Do NOT add any explanation, prefix, markdown, or surrounding text.
        Return just the raw JSON. Example:
        {
          "title": "...",
          "content": "...",
          "koreanContent": "..."
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

    private JsonNode callOpenAi(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", props.getTemperature()
        );
        String raw = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(om.writeValueAsString(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        String contentText = extractContentText(raw);
        return om.readTree(contentText != null ? contentText : "{}");
    }

    private String extractContentText(String raw) throws Exception {
        ChatResponse resp = om.readValue(raw, ChatResponse.class);
        if (resp == null || resp.choices == null || resp.choices.isEmpty()) return "";
        Message msg = resp.choices.get(0).message;
        return msg != null ? msg.content : "";
    }

    private static String getText(JsonNode node, String key) {
        return node != null && node.has(key) && !node.get(key).isNull() ? node.get(key).asText("") : "";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public record AiResultFull(String title, String content, String koreanContent) {}

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
