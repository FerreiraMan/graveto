package me.ferreira.graveto.identity.web;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.web.request.LoginRequestDto;
import me.ferreira.graveto.identity.web.request.RegisterRequestDto;
import me.ferreira.graveto.identity.web.response.LoginResponseDto;
import me.ferreira.graveto.identity.web.response.RegisterResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final String LOGIN = "/login";
  private static final String REGISTER = "/register";
  private static final String USER_SID_PATH = "/{sid}";

  private final AuthService authService;

  @PostMapping(path = LOGIN, produces = "application/json")
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody final LoginRequestDto requestDto) {

    final LoginCommand command = new LoginCommand(
        requestDto.email().trim().toLowerCase(),
        requestDto.password()
    );

    final String token = authService.login(command);

    final LoginResponseDto response = new LoginResponseDto(token);

    return ResponseEntity.ok(response);
  }

  @PostMapping(path = REGISTER, produces = "application/json")
  public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody final RegisterRequestDto requestDto) {

    final RegisterCommand command = new RegisterCommand(
        requestDto.email().trim().toLowerCase(),
        requestDto.password()
    );

    final User registeredUser = authService.register(command);

    final RegisterResponseDto response = new RegisterResponseDto(
        registeredUser.getSid(),
        registeredUser.getEmail()
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(USER_SID_PATH)
        .buildAndExpand(response.sid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

}
