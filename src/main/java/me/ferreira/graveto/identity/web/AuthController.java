package me.ferreira.graveto.identity.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.web.request.LoginRequestDTO;
import me.ferreira.graveto.identity.web.request.RegisterRequestDTO;
import me.ferreira.graveto.identity.web.response.LoginResponseDTO;
import me.ferreira.graveto.identity.web.response.RegisterResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(value = "/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String LOGIN = "/login";
    private static final String REGISTER = "/register";
    private static final String USER_SID_PATH = "/{sid}";

    private final AuthService authService;

    @PostMapping(path = LOGIN, produces = "application/json")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody final LoginRequestDTO requestDTO) {

        final LoginCommand command = new LoginCommand(
            requestDTO.email().trim().toLowerCase(),
            requestDTO.password()
        );

        final String token = authService.login(command);

        final LoginResponseDTO response = new LoginResponseDTO(token);

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = REGISTER, produces = "application/json")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody final RegisterRequestDTO requestDTO) {

        final RegisterCommand command = new RegisterCommand(
            requestDTO.email().trim().toLowerCase(),
            requestDTO.password()
        );

        final User registeredUser = authService.register(command);

        final RegisterResponseDTO response = new RegisterResponseDTO(
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
