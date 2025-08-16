package cosacosa.medimate.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecheckRequestDto {
    private String language;    // 요청 언어: english/chinese
    private String name;
    private Integer age;
    private String nationality;
    private String gender;
    private String description;
}
