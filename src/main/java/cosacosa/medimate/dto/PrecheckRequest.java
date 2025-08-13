package cosacosa.medimate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PrecheckRequest {
    private String language;    // 요청 언어: english/chinese
    private String name;
    private Integer age;
    private String nationality;
    private String gender;
    private String description;
    private Long userId;        // 테스트 중에는 1 고정 사용 가능
}
