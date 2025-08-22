package cosacosa.medimate.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class OpenAiClientConfig {

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    @Value("${OPENAI_ORG_ID:}")
    private String orgId;

    @Value("${OPENAI_PROJECT_ID:}")
    private String projectId;

    @Value("${UPSTAGE_API_KEY}")
    private String upstageApiKey;

    @Bean
    public WebClient openAiWebClient() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("환경변수 OPENAI_API_KEY가 설정되지 않았습니다.");
        }

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 타임아웃 5초
                .responseTimeout(Duration.ofSeconds(60)) // 응답 타임아웃 30초
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS));
                })
                .compress(true); // HTTP 응답 압축 허용

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE) // JSON 응답 명시
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(c -> c.defaultCodecs()
                                        .maxInMemorySize(10 * 1024 * 1024) // 버퍼 최대 크기 10MB
                                )
                                .build()
                );

        if (!orgId.isBlank()) {
            builder.defaultHeader("OpenAI-Organization", orgId);
        }
        if (!projectId.isBlank()) {
            builder.defaultHeader("OpenAI-Project", projectId);
        }
        return builder.build();
    }

    @Bean
    public WebClient upstageWebClient() {
        if (upstageApiKey == null || upstageApiKey.isBlank()) {
            throw new IllegalStateException("환경변수 UPSTAGE_API_KEY가 설정되지 않았습니다.");
        }

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(60))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS));
                })
                .compress(true);

        return WebClient.builder()
                .baseUrl("https://api.upstage.ai")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + upstageApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(c -> c.defaultCodecs()
                                        .maxInMemorySize(10 * 1024 * 1024)
                                )
                                .build()
                )
                .build();
    }
}
