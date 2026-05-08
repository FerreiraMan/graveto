package me.ferreira.graveto.identity.service;

import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.service.command.RegisterCommand;

public interface AuthService {

    String login(LoginCommand command);

    User register(RegisterCommand command);

}
