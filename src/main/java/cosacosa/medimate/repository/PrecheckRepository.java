package cosacosa.medimate.repository;

import cosacosa.medimate.domain.Precheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrecheckRepository extends JpaRepository<Precheck, Long> {
    // 특정 유저의 문진 목록
    List<Precheck> findByUserIdOrderByIdDesc(Long userId);

    // 전체 문진 목록
    List<Precheck> findAllByOrderByIdAsc();
}
