package cosacosa.medimate.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecheckRequestDto {
    private String name;
    private Integer age;
    private String gender;
    private String description;
    private String nationality;
    private String visitPurpose;
}