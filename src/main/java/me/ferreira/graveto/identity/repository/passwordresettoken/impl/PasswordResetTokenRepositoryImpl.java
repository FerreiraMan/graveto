package me.ferreira.graveto.identity.repository.passwordresettoken.impl;

import java.util.List;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenJpaRepository;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

  private final PasswordResetTokenJpaRepository repository;

  @Override
  public PasswordResetToken save(final PasswordResetToken passwordResetToken) {
    return repository.save(passwordResetToken);
  }

  @Override
  public List<PasswordResetToken> findAllByUser(final User user) {
    return repository.findAllByUser(user);
  }

  @Override
  public void deleteAllFromUser(final User user) {
    repository.deleteAllByUser(user);
  }

}
