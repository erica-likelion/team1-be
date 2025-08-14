package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                 // 닉네임

    @Column(nullable = false)
    private String password;             // 비밀번호

    @Column(nullable = false)
    private String prescriptionHistory;  // 처방 히스토리
}
