package cosacosa.medimate.repository;

import cosacosa.medimate.domain.Precheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrecheckRepository extends JpaRepository<Precheck, Long> {
    List<Precheck> findAllByOrderByIdDesc();
    List<Precheck> findAllByOrderByIdAsc();
}
