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

    private final PrecheckService service;

    // POST /api/precheck
    // 201 Created
    @PostMapping
    public ResponseEntity<PrecheckResponse> create(@RequestBody PrecheckRequest req) {
        Precheck saved = service.save(req);
        PrecheckResponse resp = service.toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/precheck/" + saved.getId()))
                .body(resp);
    }

    // GET /api/precheck
    // 200 OK
    @GetMapping
    public ResponseEntity<List<PrecheckListItemResponse>> list() {
        return ResponseEntity.ok(service.list()); // service에서 최소 필드 DTO로 매핑
    }

    // GET /api/precheck/{id}
    // 200 OK
    @GetMapping("/{id}")
    public ResponseEntity<PrecheckResponse> get(@PathVariable Long id) {
        Precheck precheck = service.get(id);
        return ResponseEntity.ok(service.toResponse(precheck));
    }

    // GET /api/precheck/user/{userId}
    // 200 OK
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PrecheckResponse>> listByUser(@PathVariable Long userId) {
        List<Precheck> items = service.findByUser(userId);
        return ResponseEntity.ok(service.toResponseList(items));
    }
}
