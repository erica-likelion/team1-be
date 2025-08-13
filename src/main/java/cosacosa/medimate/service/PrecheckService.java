package cosacosa.medimate.service;

import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.dto.PrecheckRequest;
import cosacosa.medimate.dto.PrecheckResponse;
import cosacosa.medimate.dto.PrecheckListItemResponse;
import cosacosa.medimate.repository.PrecheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecheckService {
    private final PrecheckRepository repository;

    @Transactional
    public Precheck save(PrecheckRequest req) {
        Precheck entity = Precheck.builder()
                .title(null)                         // AI가 나중에 채움
                .content(null)                       // AI가 나중에 채움
                .name(nullToEmpty(req.getName()))
                .age(req.getAge() == null ? 0 : req.getAge())
                .nationality(nullToEmpty(req.getNationality()))
                .gender(nullToEmpty(req.getGender()))
                .description(nullToEmpty(req.getDescription())) // 원문 저장
                .userId(req.getUserId())
                .build();
        return repository.save(entity); // createdAt은 @PrePersist로 자동 세팅
    }

    public Precheck get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Precheck not found: " + id));
    }

    public List<Precheck> findByUser(Long userId) {
        return repository.findByUserIdOrderByIdDesc(userId);
    }

    public List<PrecheckListItemResponse> list() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(p -> PrecheckListItemResponse.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    public PrecheckResponse toResponse(Precheck p) {
        if (p == null) return null;
        return PrecheckResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .createdAt(p.getCreatedAt())
                .name(p.getName())
                .age(p.getAge())
                .nationality(p.getNationality())
                .gender(p.getGender())
                .description(p.getDescription())
                .userId(p.getUserId())
                .build();
    }

    public List<PrecheckResponse> toResponseList(List<Precheck> list) {
        return list.stream().map(this::toResponse).toList();
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
