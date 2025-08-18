package cosacosa.medimate.service;

import cosacosa.medimate.domain.ChatMessage;
import cosacosa.medimate.domain.ChatRoom;
import cosacosa.medimate.dto.ChatMessageResponse;
import cosacosa.medimate.dto.ChatRoomResponse;
import cosacosa.medimate.repository.ChatMessageRepository;
import cosacosa.medimate.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

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

    public List<ChatRoomResponse> readAllChatRoom() {
        List<ChatRoom> roomList = chatRoomRepository.findAll();
        return roomList.stream().map(chatRoom -> new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getRoomCode(),
                chatRoom.getLastChat(),
                chatRoom.getCreatedAt()
        )).toList();
    }
}
