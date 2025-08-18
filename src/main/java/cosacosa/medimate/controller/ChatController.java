package cosacosa.medimate.controller;

import cosacosa.medimate.dto.ChatMessageResponse;
import cosacosa.medimate.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/message")
    public void message(ChatMessageResponse chatMessage) {
        System.out.println("메세지 요청을 받았습니다!");
        chatService.processMessage(chatMessage);
    }

    @GetMapping("/api/chat/rooms/{roomId}")
    public ResponseEntity<List<ChatMessageResponse>> getMessageList(@PathVariable Long roomId) {
        List<ChatMessageResponse> result = chatService.readMessageList(roomId);
        return ResponseEntity.ok(result);
    }
}
