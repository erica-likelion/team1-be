package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;

@Entity
@Table(
        name = "precheck",
        indexes = {
                @Index(name = "idx_precheck_user", columnList = "userId")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Precheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;                   // AI 생성 제목

    @Column(columnDefinition = "TEXT")
    private String content;                 // AI 문진 번역

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDate createdAt;            // 문진 작성일

    @Column(nullable = false, length = 100)
    private String name;                    // 환자 이름

    @Column(nullable = false)
    private Integer age;                    // 환자 나이

    @Column(nullable = false, length = 50)
    private String nationality;             // 국적

    @Column(nullable = false, length = 20)
    private String gender;                  // 성별

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;             // 증상 설명 (원문)

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "userId",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_precheck_user")
    )
    private User user;  // FK: precheck.userId → user.id

    @Column(name = "korean_content", columnDefinition = "TEXT")
    private String koreanContent;   // 한국어 버전 AI 생성글
}
