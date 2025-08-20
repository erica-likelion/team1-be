package cosacosa.medimate.dto;

import lombok.Getter;

@Getter
public class ChatRoomRequest {
    private String type;
    private String language;
    private Long precheckId;
    private Long prescriptionId;
}
