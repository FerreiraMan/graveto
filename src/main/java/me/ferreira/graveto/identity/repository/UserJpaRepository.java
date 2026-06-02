package me.ferreira.graveto.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(final String email);

  List<User> findBySidIn(final Set<UUID> uuidList);

}
