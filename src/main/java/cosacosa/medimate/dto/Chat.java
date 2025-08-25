package cosacosa.medimate.dto;

import lombok.Getter;

@Getter
public class Chat {
    private String sender;
    private String message;
    private String language;
    private Long roomId;
}
