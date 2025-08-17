package cosacosa.medimate.dto;

import lombok.Getter;

@Getter
public class ChatMessage {
    private Long roomId;
    private String sender;
    private String language;
    private String message;
}
