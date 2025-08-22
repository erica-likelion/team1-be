package cosacosa.medimate.service;

import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.domain.User;
import cosacosa.medimate.dto.PrecheckListItemResponseDto;
import cosacosa.medimate.dto.PrecheckRequestDto;
import cosacosa.medimate.dto.PrecheckResponseDto;
import cosacosa.medimate.repository.PrecheckRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private static final String DEFAULT_VISIT_PURPOSE = "증상 상담";
    private final PrecheckRepository repository;
    private final AiPrecheckService aiService;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Precheck saveWithAi(PrecheckRequestDto req, AiPrecheckService.AiResultFull ai) {
        User userRef = em.getReference(User.class, 1L);
        Precheck entity = Precheck.builder()
                .title(ai.title())
                .content(ai.content())
                .koreanContent(ai.koreanContent())
                .visitPurpose(resolveVisitPurpose(ai.visitPurpose()))
                .name(req.getName())
                .age(req.getAge())
                .nationality(req.getNationality())
                .gender(req.getGender())
                .description(req.getDescription())
                .user(userRef)
                .build();
        return repository.save(entity);
    }

    public Precheck get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Precheck not found: " + id));
    }

    public List<PrecheckListItemResponseDto> list() {
        return repository.findAllByOrderByIdDesc().stream()
                .map(p -> PrecheckListItemResponseDto.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    public PrecheckResponseDto toCreateResponse(Precheck p) {
        return toDto(p);
    }

    @Transactional
    public PrecheckResponseDto toDetailResponse(Precheck p) {
        boolean changed = false;

        if (isBlank(p.getTitle()) || isBlank(p.getContent()) || isBlank(p.getKoreanContent())) {
            PrecheckRequestDto req = toReqFromEntity(p);
            AiPrecheckService.AiResultFull ai = aiService.generateTitleAndContent(req);

            if (isBlank(p.getTitle()))         { p.setTitle(ai.title()); changed = true; }
            if (isBlank(p.getContent()))       { p.setContent(ai.content()); changed = true; }
            if (isBlank(p.getKoreanContent())) { p.setKoreanContent(ai.koreanContent()); changed = true; }
        }

        if (isBlank(p.getVisitPurpose())) {
            p.setVisitPurpose(DEFAULT_VISIT_PURPOSE);
            changed = true;
        }

        if (changed) repository.saveAndFlush(p);
        return toDto(p);
    }

    private PrecheckResponseDto toDto(Precheck p) {
        return PrecheckResponseDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .koreanContent(p.getKoreanContent())
                .createdAt(p.getCreatedAt())
                .name(p.getName())
                .age(p.getAge())
                .nationality(p.getNationality())
                .gender(p.getGender())
                .description(p.getDescription())
                .visitPurpose(resolveVisitPurpose(p.getVisitPurpose()))
                .build();
    }

    private PrecheckRequestDto toReqFromEntity(Precheck p) {
        PrecheckRequestDto req = new PrecheckRequestDto();
        req.setLanguage(""); // 저장된 언어가 있다면 그 값을 사용
        req.setName(p.getName());
        req.setAge(p.getAge());
        req.setNationality(p.getNationality());
        req.setGender(p.getGender());
        req.setDescription(p.getDescription() == null ? "" : p.getDescription());
        return req;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String resolveVisitPurpose(String v) {
        return isBlank(v) ? DEFAULT_VISIT_PURPOSE : v;
    }
}
