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

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Precheck saveWithAi(PrecheckRequestDto req, AiPrecheckService.AiResult ai) {
        User userRef = em.getReference(User.class, 1L);
        Precheck entity = Precheck.builder()
                .title(ai.title())
                .content(ai.content())
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
        return PrecheckResponseDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .createdAt(p.getCreatedAt())
                .name(p.getName())
                .age(p.getAge())
                .nationality(p.getNationality())
                .gender(p.getGender())
                .description(p.getDescription())
                .build();
    }

    public PrecheckResponseDto toDetailResponse(Precheck p) {
        return toCreateResponse(p);
    }
}
