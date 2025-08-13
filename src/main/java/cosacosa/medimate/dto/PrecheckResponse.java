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
public class PrecheckResponse {
    private Long id;
    private String title;      // AI 생성 제목
    private String content;    // AI 문진 번역

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt; // 문진 작성일

    private String name;
    private Integer age;
    private String nationality;
    private String gender;
    private String description;
    private Long userId;
}
