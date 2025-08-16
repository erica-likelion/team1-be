package cosacosa.medimate.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosacosa.medimate.config.PrecheckAiProperties;
import cosacosa.medimate.dto.PrecheckRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class AiPrecheckService {
    private final WebClient openAiWebClient;
    private final PrecheckAiProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public AiResult generateTitleAndContent(PrecheckRequestDto req) {
        try {
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

            String body = String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                  ],
                  "temperature": %s
                }
                """,
                    props.getModel(),
                    quote(systemPrompt),
                    quote(userPrompt),
                    props.getTemperature()
            );

            String raw = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ChatResponse resp = om.readValue(raw, ChatResponse.class);
            String contentText = (resp != null && resp.choices != null && !resp.choices.isEmpty())
                    ? resp.choices.get(0).message.content
                    : "";

            if (contentText == null || contentText.isBlank()) return new AiResult("", "");

            JsonNode json = om.readTree(contentText);
            String title = getText(json, "title");
            String content = getText(json, "content");

            return new AiResult(title, content);

        } catch (Exception e) {
            return new AiResult("", "");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }

    private static String getText(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull() ? node.get(key).asText("") : "";
    }

    public record AiResult(String title, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse {
        public java.util.List<Choice> choices;
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
