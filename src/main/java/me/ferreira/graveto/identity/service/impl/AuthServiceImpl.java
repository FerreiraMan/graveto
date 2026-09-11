package me.ferreira.graveto.identity.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.mail.domain.event.PasswordResetEmailEvent;
import me.ferreira.graveto.common.web.exception.identity.UserAlreadyExistsException;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.PasswordResetTokenService;
import me.ferreira.graveto.identity.service.command.ForgotPasswordCommand;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final PasswordResetTokenService passwordResetTokenService;
  private final ApplicationEventPublisher eventPublisher;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public String login(final LoginCommand command) {

    final Authentication authenticationRequest =
        UsernamePasswordAuthenticationToken.unauthenticated(command.email(), command.password());
    final Authentication authenticationResponse = authenticationManager.authenticate(authenticationRequest);

    final AuthUser authUser = (AuthUser) authenticationResponse.getPrincipal();
    return jwtService.createJwtToken(authUser);
  }

  @Override
  @Transactional
  public User register(final RegisterCommand command) {

    userRepository.fetchUserCredentials(command.email()).ifPresent(user -> {
      throw new UserAlreadyExistsException();
    });

    final String passwordHash = passwordEncoder.encode(command.password());
    final User user = User.create(command.email(), passwordHash);

    return userRepository.save(user);
  }

  @Override
  @Transactional
  public void forgotPassword(final ForgotPasswordCommand command) {

    userRepository.fetchUserCredentials(command.email())
        .ifPresent(user -> {
          final ForgotPasswordTokenDetails tokenDetails = passwordResetTokenService.generateToken(user);
          eventPublisher.publishEvent(
              new PasswordResetEmailEvent(user.getEmail(), tokenDetails.token(), tokenDetails.expiresIn()));
        });
  }

}
