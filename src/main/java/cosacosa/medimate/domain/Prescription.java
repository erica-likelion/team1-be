package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caution; // 처방전 정보를 바탕으로 AI가 생성한 설명글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Long userId; // 처방전을 입력한 사용자
}
