package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Getter
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content = ""; // AI가 생성한 처방전 설명 글 (사용자 설정 언어)

    private String koreanContent = ""; // AI가 생성한 처방전 설명 글 (한국어)

    private LocalDate createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user; // 처방전을 입력한 사용자

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDate.now();
    }
}
