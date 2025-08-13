package cosacosa.medimate.controller;

import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.dto.PrecheckRequest;
import cosacosa.medimate.dto.PrecheckResponse;
import cosacosa.medimate.dto.PrecheckListItemResponse; // 추가: 리스트 전용 DTO
import cosacosa.medimate.service.PrecheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/precheck")
public class PrecheckController {
}
