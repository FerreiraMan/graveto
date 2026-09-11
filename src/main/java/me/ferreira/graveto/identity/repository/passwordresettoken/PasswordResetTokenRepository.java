package me.ferreira.graveto.identity.repository.passwordresettoken;

import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;

public interface PasswordResetTokenRepository {

  PasswordResetToken save(final PasswordResetToken passwordResetToken);

  void deleteAllFromUser(final User user);

}
