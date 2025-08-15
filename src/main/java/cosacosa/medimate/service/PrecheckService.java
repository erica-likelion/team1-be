package cosacosa.medimate.service;

import cosacosa.medimate.domain.Precheck;
import cosacosa.medimate.domain.User;
import cosacosa.medimate.dto.PrecheckListItemResponseDto;
import cosacosa.medimate.dto.PrecheckRequestDto;
import cosacosa.medimate.dto.PrecheckResponseDto;
import cosacosa.medimate.repository.PrecheckRepository;
import cosacosa.medimate.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public Precheck saveWithAi(PrecheckRequestDto req, AiPrecheckService.AiResult ai) {
        if (req.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid userId: " + req.getUserId()));

        Precheck entity = Precheck.builder()
                .title(nullToEmpty(ai.title()))
                .content(nullToEmpty(ai.content()))
                .name(nullToEmpty(req.getName()))
                .age(req.getAge())
                .nationality(nullToEmpty(req.getNationality()))
                .gender(nullToEmpty(req.getGender()))
                .description(nullToEmpty(req.getDescription()))
                .user(user)
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

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
