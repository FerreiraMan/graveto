package me.ferreira.graveto.identity.repository.passwordresettoken;

import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, Long> {

  void deleteAllByUser(final User user);

}
