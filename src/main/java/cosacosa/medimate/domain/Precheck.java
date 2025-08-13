package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "precheck")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Precheck {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // 고유번호

    @Column(nullable = true, length = 255)
    private String title;            // AI 생성 제목

    @Column(columnDefinition = "TEXT", nullable = true)
    private String content;          // AI 문진 번역

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt; // 문진 작성일 (YYYY-MM-DD)

    @Column(nullable = false)
    private String name;             // 환자 이름 (원문)

    @Column(nullable = false)
    private Integer age;             // 환자 나이

    @Column(nullable = false)
    private String nationality;      // 국적 (원문)

    @Column(nullable = false)
    private String gender;           // 성별 (원문)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;  // 증상 설명 (원문)

    @Column(nullable = false)
    private Long userId;             // 환자 id

    // createdAt 자동 생성
    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDate.now();
        }
    }

}
