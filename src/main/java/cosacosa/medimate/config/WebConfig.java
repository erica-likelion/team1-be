package cosacosa.medimate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API 경로 허용
                        // Boot 2/3 공통: 특정 도메인 나열
                        //.allowedOrigins("http://localhost:3000", "https://app.example.com")

                        // 서브도메인 와일드카드가 필요하거나 credentials와 함께 유연하게 쓰고 싶다면:
                        .allowedOriginPatterns("*")

                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        // 클라이언트에서 쿠키/인증정보를 보낼 계획이면 true
                        .allowCredentials(true)
                        // 프리플라이트(OPTIONS) 결과 캐시 시간(초)
                        .maxAge(3600);
            }
        };
    }
}
