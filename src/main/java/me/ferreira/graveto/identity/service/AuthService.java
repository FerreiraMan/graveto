package me.ferreira.graveto.identity.service;

import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.command.ForgotPasswordCommand;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.service.command.ResetPasswordCommand;

public interface AuthService {

  String login(LoginCommand command);

  User register(RegisterCommand command);

  void forgotPassword(ForgotPasswordCommand command);

  void resetPassword(ResetPasswordCommand command);

}
