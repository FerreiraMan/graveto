package me.ferreira.graveto.identity.repository;

import java.util.Optional;
import me.ferreira.graveto.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserJpaRepository extends JpaRepository<User, Long> {

  @Query(value = "SELECT u FROM User u WHERE u.email = ?1")
  Optional<User> findByEmail(final String email);

}
