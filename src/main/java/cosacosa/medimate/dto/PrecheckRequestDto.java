package cosacosa.medimate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PrecheckRequestDto {
    private String language;    // 요청 언어: english/chinese
    private String name;
    private Integer age;
    private String nationality;
    private String gender;
    private String description;
    private Long userId;
}