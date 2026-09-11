package me.ferreira.graveto.identity.service;

import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;

public interface PasswordResetTokenService {

  ForgotPasswordTokenDetails generateToken(User user);

}
