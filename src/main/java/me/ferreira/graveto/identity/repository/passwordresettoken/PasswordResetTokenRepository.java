package me.ferreira.graveto.identity.repository.passwordresettoken;

import java.util.List;
import java.util.Optional;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;

public interface PasswordResetTokenRepository {

  PasswordResetToken save(final PasswordResetToken passwordResetToken);

  Optional<PasswordResetToken> findByTokenHash(final String tokenHash);

  List<PasswordResetToken> findAllByUser(final User user);

  void deleteAllFromUser(final User user);

}
