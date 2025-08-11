package cosacosa.medimate.service;

import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.dto.PrecheckRequest;
import cosacosa.medimate.dto.PrecheckResponse;
import cosacosa.medimate.repository.PrecheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecheckService {
    private final PrecheckRepository repository;

    // 사전문진 저장
    @Transactional
    public Precheck save(PrecheckRequest req) {
        Precheck entity = Precheck.builder()
                .language(nullToEmpty(req.getLanguage()))
                .name(nullToEmpty(req.getName()))
                .age(req.getAge() == null ? 0 : req.getAge())
                .nationality(nullToEmpty(req.getNationality()))
                .gender(nullToEmpty(req.getGender()))
                .description(nullToEmpty(req.getDescription())) // 원문 저장
                .userId(req.getUserId())
                .build();
        return repository.save(entity);
    }

    // 단건 조회
    public Precheck get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Precheck not found: " + id));
    }

    // 사용자별 목록 조회 (최근 것 먼저)
    public List<Precheck> findByUser(Long userId) {
        return repository.findByUserIdOrderByIdDesc(userId);
    }

    // 단건 → 명세 응답 DTO (추가 가공 없이 그대로 반환)
    public PrecheckResponse toResponse(Precheck p) {
        if (p == null) return null;
        return new PrecheckResponse(
                p.getId(),
                p.getLanguage(),
                p.getName(),
                safeInt(p.getAge()),
                p.getNationality(),
                p.getGender(),
                p.getDescription()
        );
    }

    // api 목록 응답용 DTO 반환(컨트롤러에서 바로 사용)
    public List<PrecheckResponse> toResponseList(List<Precheck> list) {
        List<PrecheckResponse> out = new ArrayList<>();
        for (Precheck p : list) out.add(toResponse(p));
        return out;
    }

    // api 응답 값 null-safe 처리
    private String nullToEmpty(String s) { return s == null ? "" : s; }
    private int safeInt(Integer i) { return i == null ? 0 : i; }
}
