package cosacosa.medimate.service;

import cosacosa.medimate.domain.ChatMessage;
import cosacosa.medimate.dto.ChatMessageResponse;
import cosacosa.medimate.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    // roomId를 구독 중인 클라이언트들에게 메세지를 전송
    public void processMessage(ChatMessageResponse chatMessage) {
        messagingTemplate.convertAndSend("/sub/chat/rooms/"+chatMessage.getRoomId(), chatMessage);
    }

    public List<ChatMessageResponse> readMessageList(Long roomId) {
        List<ChatMessage> messageList = chatMessageRepository.findByChatRoomId(roomId);
        return messageList.stream().map(chatMessage -> new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getSender(),
                chatMessage.getMessage(),
                chatMessage.getKoreanMessage(),
                chatMessage.getCreatedAt(),
                chatMessage.getChatRoom().getId()
        )).toList();
    }
}
