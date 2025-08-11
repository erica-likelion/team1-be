package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "precheck")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Precheck {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // 고유번호

    @Column(nullable = false)
    private String name;             // 환자 이름 (원문 그대로)

    @Column(nullable = false)
    private Integer age;             // 환자 나이

    @Column(nullable = false)
    private String nationality;      // 국적 (원문 그대로)

    @Column(nullable = false)
    private String gender;           // 성별 (원문 그대로)

    @Column(nullable = false, columnDefinition = "text")
    private String description;      // 증상 설명 (원문: 영어/중국어)

    @Column(nullable = false)
    private Long userId;             // 환자 id

    @Column(nullable = false, length = 16)
    private String language;         // 요청 언어: english/chinese

    @Column(columnDefinition = "text")
    private String descriptionKo;    // 번역된 증상 설명(한국어)
}
