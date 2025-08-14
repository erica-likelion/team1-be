package cosacosa.medimate.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosacosa.medimate.config.PrecheckAiProperties;
import cosacosa.medimate.dto.PrecheckRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AiPrecheckService {
    private final WebClient openAiWebClient;
    private final PrecheckAiProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public AiResult generateTitleAndContent(PrecheckRequestDto req) {
        Result r = generate(
                req.getLanguage(),
                req.getName(),
                req.getAge(),
                req.getNationality(),
                req.getGender(),
                req.getDescription()
        );
        return new AiResult(r.title(), r.content());
    }

    public Result generate(String language, String name, Integer age,
                           String nationality, String gender, String description) {

        String sys = """
            You are a clinical intake assistant for pre-visit triage.
        
            Follow these rules exactly:
            1. Output MUST be a valid JSON object with exactly two keys: "title" and "content".
            2. "title": A concise one-line title summarizing the patient's symptoms and context.
            3. "content": The patient's precheck information rewritten in Korean,
               organized in a clear, polite, and clinically neutral style (no diagnosis or treatment).
            4. Do NOT include any keys other than "title" and "content".
            5. Do NOT include any text outside the JSON object.
        
            Example JSON:
            {
              "title": "AI가 생성한 제목",
              "content": "AI가 사용자가 입력한 사전문진 정보를 한국어로 정리해준 글"
            }
            """;

        String lang = (language == null || language.isBlank())
                ? props.getDefaultLanguage() : language;

        String user = """
            {
              "language": "%s",
              "name": "%s",
              "age": %s,
              "nationality": "%s",
              "gender": "%s",
              "description": "%s"
            }
            """.formatted(lang, n(name), (age == null ? "\"\"" : age), n(nationality), n(gender), n(description));

        String body = """
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": %s},
                {"role": "user", "content": %s}
              ],
              "temperature": %s,
              "response_format": {"type":"json_object"}
            }
            """.formatted(props.getModel(), quote(sys), quote(user), Double.toString(props.getTemperature()));

        try {
            String raw = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("{\"choices\":[{\"message\":{\"content\":\"{\\\"title\\\":\\\"\\\",\\\"content\\\":\\\"\\\"}\"}}]}"))
                    .block();

            ChatResponse resp = om.readValue(raw, ChatResponse.class);
            String text = "";
            if (resp != null &&
                    resp.choices != null &&
                    !resp.choices.isEmpty() &&
                    resp.choices.get(0).message != null) {
                text = resp.choices.get(0).message.content;
            }
            if (text == null || text.isBlank()) return new Result("", "");

            JsonNode root = om.readTree(text);
            return new Result(asText(root, "title"), asText(root, "content"));

        } catch (Exception e) {
            return new Result("", "");
        }
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String asText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText("") : "";
    }
    private static String quote(String s) { return "\"" + s.replace("\"","\\\"") + "\""; }

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

    public record AiResult(String title, String content) {}
    public record Result(String title, String content) {}
}
