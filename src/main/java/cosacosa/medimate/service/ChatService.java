package cosacosa.medimate.service;

import cosacosa.medimate.domain.ChatMessage;
import cosacosa.medimate.domain.ChatRoom;
import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.domain.Prescription;
import cosacosa.medimate.dto.Chat;
import cosacosa.medimate.dto.ChatMessageResponse;
import cosacosa.medimate.dto.ChatRoomRequest;
import cosacosa.medimate.dto.ChatRoomResponse;
import cosacosa.medimate.repository.ChatMessageRepository;
import cosacosa.medimate.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final WebClient openAiWebClient;
    private final PrecheckService precheckService;
    private final PrescriptionService prescriptionService;

    private record OpenAiRequest(String model, List<Message> messages, double temperature) {}
    private record Message(String role, String content) {}

    private record OpenAiResponse(List<Choice> choices) {}
    private record Choice(Message message) {}

    // roomId를 구독 중인 클라이언트들에게 메세지를 전송
    @Transactional
    public void processMessage(Chat chatMessage) {
        String promptText = """
                You are a highly skilled medical interpreter facilitating communication between a doctor and a patient.
                Your task is to accurately translate the user's message into {target_language}.
                Maintain a professional and clear tone suitable for a medical consultation.
                Do not add any extra information, explanations, or conversational remarks. Provide only the direct translation of the given text.
                
                ---
                
                Original Text: "{text_to_translate}"
                
                Translated Text in {target_language}:""";

        String finalPrompt = promptText
                .replace("{target_language}", chatMessage.getLanguage())
                .replace("{text_to_translate}", chatMessage.getMessage());


        OpenAiRequest request = new OpenAiRequest(
                "gpt-4o", // 혹은 "gpt-3.5-turbo"
                List.of(new Message("user", finalPrompt)),
                0.7 // 창의성 조절 (0.0 ~ 2.0), 번역은 낮은 값이 좋음
        );


        OpenAiResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block(); // 비동기 응답을 동기적으로 대기

        String translatedText = "번역에 실패했습니다."; // 기본값
        if (response != null && !response.choices().isEmpty()) {
            translatedText = response.choices().get(0).message().content();
        }

        ChatRoom chatRoom = chatRoomRepository.getReferenceById(chatMessage.getRoomId());
        String koreanMessage;
        String message;
        if (chatMessage.getLanguage().equals("korean")) {
            koreanMessage = translatedText;
            message = chatMessage.getMessage();
        } else {
            koreanMessage = chatMessage.getMessage();
            message = translatedText;
        }

        ChatMessage newMessage = chatMessageRepository.save(new ChatMessage(
                chatMessage.getSender(),
                message,
                koreanMessage,
                chatRoom
        ));
        
        ChatRoom room = chatRoomRepository.findById(chatMessage.getRoomId()).orElseThrow(() -> new RuntimeException("room 찾기 실패"));
        room.setLastChat(message);
        chatRoomRepository.save(room);

        ChatMessageResponse chatResponse = new ChatMessageResponse(
                newMessage.getId(),
                newMessage.getSender(),
                newMessage.getMessage(),
                newMessage.getKoreanMessage(),
                newMessage.getCreatedAt(),
                newMessage.getChatRoom().getId()
        );

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + chatMessage.getRoomId(), chatResponse);
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

    @Transactional
    public ChatRoomResponse createChatRoom(ChatRoomRequest dto) {
        ChatRoom room = chatRoomRepository.save(new ChatRoom());
        String message = "";
        String koreanMessage = "";
        String sender = "user";
        if (dto.getType().equals("precheck") && dto.getPrecheckId() != null) {
            Precheck precheck = precheckService.get(dto.getPrecheckId());
            message = precheck.getContent();
            koreanMessage = precheck.getKoreanContent();
        } else if (dto.getType().equals("prescription") && dto.getPrescriptionId() != null) {
            Prescription prescription = prescriptionService.get(dto.getPrescriptionId());
            message = prescription.getContent();
            koreanMessage = prescription.getKoreanContent();
        } else {
            koreanMessage = "안녕하세요. 무엇을 도와드릴까요?";
            if (dto.getLanguage().equals("english")) {
                message = "Hello. How can I help you?";
            } else if (dto.getLanguage().equals("chinese")) {
                message = "您好，请问有什么可以帮您的吗？";
            } else {
                message = koreanMessage;
            }
            sender = "medi";
        }
        return createMessage(message, koreanMessage, room, sender);
    }

    private ChatRoomResponse createMessage(String message, String koreanMessage, ChatRoom room, String sender) {
        ChatMessage newMessage = new ChatMessage(
                sender,
                message,
                koreanMessage,
                room
        );
        chatMessageRepository.save(newMessage);
        ChatMessageResponse chatResponse = new ChatMessageResponse(
                newMessage.getId(),
                newMessage.getSender(),
                newMessage.getMessage(),
                newMessage.getKoreanMessage(),
                newMessage.getCreatedAt(),
                newMessage.getChatRoom().getId()
        );
        messagingTemplate.convertAndSend("/sub/chat/rooms/" + room.getId(), chatResponse);
        return new ChatRoomResponse(room.getId(), room.getRoomCode());
    }
}
