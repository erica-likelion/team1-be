package cosacosa.medimate.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecheckRequestDto {
    private String language;
    private String name;
    private Integer age;
    private String nationality;
    private String gender;
    private String description;
}
