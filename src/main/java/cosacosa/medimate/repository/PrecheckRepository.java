package cosacosa.medimate.repository;

import cosacosa.medimate.domain.Precheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrecheckRepository extends JpaRepository<Precheck, Long> {
    List<Precheck> findAllByOrderByIdDesc();    // 전체 문진 목록 (최신순)
}
