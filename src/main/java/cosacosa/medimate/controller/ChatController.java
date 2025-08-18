package cosacosa.medimate.controller;

import cosacosa.medimate.dto.ChatMessage;
import cosacosa.medimate.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/message")
    public void message(ChatMessage chatMessage) {
        System.out.println("메세지 요청을 받았습니다!");
        chatService.processMessage(chatMessage);
    }
}
