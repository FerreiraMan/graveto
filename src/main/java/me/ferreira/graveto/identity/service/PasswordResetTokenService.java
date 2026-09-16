package me.ferreira.graveto.identity.service;

import java.util.Optional;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;

public interface PasswordResetTokenService {

  ForgotPasswordTokenDetails generateToken(User user);

  Optional<User> validateToken(String token);

  void invalidateToken(User user);

}
