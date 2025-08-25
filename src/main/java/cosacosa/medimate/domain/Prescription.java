package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@NoArgsConstructor
@Getter
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content = ""; // AI가 생성한 처방전 설명 글 (사용자 설정 언어)

    @Column(columnDefinition = "TEXT")
    private String koreanContent = ""; // AI가 생성한 처방전 설명 글 (한국어)

    @Column(columnDefinition = "TEXT")
    private String contentMd = "";

    @Column(columnDefinition = "TEXT")
    private String koreanContentMd = "";

    private LocalDate createdAt;

    private String images = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 처방전을 입력한 사용자

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    public Prescription(String title, String content, String koreanContent, String contentMd, String koreanContentMd) {
        this.title = title;
        this.content = content;
        this.koreanContent = koreanContent;
        this.contentMd = contentMd;
        this.koreanContentMd = koreanContentMd;
    }
}
