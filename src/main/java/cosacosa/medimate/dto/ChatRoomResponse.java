package cosacosa.medimate.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ChatRoomResponse {
    private Long id;
    private String roomCode;
    private String lastChat;
    private LocalDate createdAt;

    public ChatRoomResponse(Long id, String roomCode, String lastChat, LocalDate createdAt) {
        this.id = id;
        this.roomCode = roomCode;
        this.lastChat = lastChat;
        this.createdAt = createdAt;
    }
}
