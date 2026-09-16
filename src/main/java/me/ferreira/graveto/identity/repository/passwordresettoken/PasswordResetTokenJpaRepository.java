package me.ferreira.graveto.identity.repository.passwordresettoken;

import java.util.List;
import java.util.Optional;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.PasswordResetToken_;
import me.ferreira.graveto.identity.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, Long> {

  @EntityGraph(attributePaths = PasswordResetToken_.USER)
  Optional<PasswordResetToken> findByTokenHash(final String tokenHash);

  @EntityGraph(attributePaths = PasswordResetToken_.USER)
  List<PasswordResetToken> findAllByUser(final User user);

  void deleteAllByUser(final User user);

}
