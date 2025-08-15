package cosacosa.medimate.controller;

import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.dto.PrecheckRequestDto;
import cosacosa.medimate.dto.PrecheckResponseDto;
import cosacosa.medimate.dto.PrecheckListItemResponseDto;
import cosacosa.medimate.service.AiPrecheckService;
import cosacosa.medimate.service.PrecheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/precheck")
public class PrecheckController {
    private final PrecheckService service;
    private final AiPrecheckService aiService;

    // POST /api/precheck
    // 저장된 번역 내용 반환
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrecheckResponseDto> create(@RequestBody PrecheckRequestDto req) {
        AiPrecheckService.AiResult ai = aiService.generateTitleAndContent(req);
        Precheck saved = service.saveWithAi(req, ai);
        return ResponseEntity.ok(service.toCreateResponse(saved));
    }

    // GET /api/precheck
    // 사전문진 데이터 전체 조회
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PrecheckListItemResponseDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    // GET /api/precheck/{precheckId}
    // 사전문진 정보 조회 (id)
    @GetMapping(value = "/{precheckId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrecheckResponseDto> get(@PathVariable("precheckId") Long precheckId) {
        Precheck precheck = service.get(precheckId);
        return ResponseEntity.ok(service.toDetailResponse(precheck));
    }
}
