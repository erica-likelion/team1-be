package cosacosa.medimate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomCode;

    private String lastChat;

    private LocalDate createdAt;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @PrePersist
    public void prePersist() {
        this.roomCode = UUID.randomUUID().toString();
        this.createdAt = LocalDate.now(ZoneId.of("Asia/Seoul"));
        this.lastChat = ""; // 기본값 설정
    }
}
