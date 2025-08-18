package cosacosa.medimate.dto;

import cosacosa.medimate.domain.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private Long id;
    private String sender;
    private String message;
    private String koreanMessage;
    private LocalDateTime createdAt;
    private Long roomId;

    public ChatMessageResponse(Long id, String sender, String message, String koreanMessage, LocalDateTime createdAt, Long roomId) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.koreanMessage = koreanMessage;
        this.createdAt = createdAt;
        this.roomId = roomId;
    }
}
