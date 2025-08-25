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
        return repository.findAllByOrderByIdAsc().stream()
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
        // '번역하기' 버튼을 누를 때마다 항상 AI를 호출하여 모든 내용을 새로 생성합니다.
        PrecheckRequestDto req = toReqFromEntity(p);
        AiPrecheckService.AiResultFull ai = aiService.generateTitleAndContent(req);

        // AI로부터 받은 최신 정보로 엔티티를 업데이트합니다.
        p.setTitle(ai.title());
        p.setContent(ai.content());
        p.setKoreanContent(ai.koreanContent());

        repository.saveAndFlush(p);
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
                .build();
    }

    private PrecheckRequestDto toReqFromEntity(Precheck p) {
        PrecheckRequestDto req = new PrecheckRequestDto();
        req.setName(p.getName());
        req.setAge(p.getAge());
        req.setNationality(p.getNationality());
        req.setGender(p.getGender());
        req.setDescription(p.getDescription() == null ? "" : p.getDescription());
        return req;
    }
}