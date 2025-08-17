package cosacosa.medimate.service;

import cosacosa.medimate.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessageSendingOperations messagingTemplate;


    // roomId를 구독 중인 클라이언트들에게 메세지를 전송
    public void processMessage(ChatMessage chatMessage) {
        messagingTemplate.convertAndSend("/sub/chat/rooms/"+chatMessage.getRoomId(), chatMessage);
    }
}
