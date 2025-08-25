package cosacosa.medimate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecheckResponseDto {
    private Long id;
    private String title;
    private String content;        // 사용자 언어 버전
    private String koreanContent;  // 한국어 버전

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;

    private String name;
    private Integer age;
    private String nationality;
    private String gender;
    private String description;
}
