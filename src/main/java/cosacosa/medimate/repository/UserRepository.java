package cosacosa.medimate.repository;

import cosacosa.medimate.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
