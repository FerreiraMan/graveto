package me.ferreira.graveto.identity.web;

import me.ferreira.graveto.common.web.exception.identity.UserAlreadyExistsException;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.web.request.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = AuthController.class)
public class RegisterControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthService service;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnCreatedWithLocationHeaderWhenRegistrationIsSuccessful() {
        // Arrange
        final UUID generatedSid = UUID.randomUUID();
        final String email = "test@graveto.com";
        final RegisterRequestDTO request = new RegisterRequestDTO(email, "password123");

        final User mockUser = mock(User.class);
        when(mockUser.getSid()).thenReturn(generatedSid);
        when(mockUser.getEmail()).thenReturn(email);
        when(service.register(any(RegisterCommand.class))).thenReturn(mockUser);

        // Act & Assert
        final MvcTestResult testResult = mvc.post()
                .uri("/auth/register")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/auth/register/" + generatedSid)
                .bodyJson()
                .extractingPath("$.sid").asString().isEqualTo(generatedSid.toString());
    }

    @Test
    void shouldTrimAndLowerCaseEmailBeforePassingToRegisterService() {
        // Arrange
        final RegisterRequestDTO request = new RegisterRequestDTO("WeIrDCaSe@GraVeTo.com", "password123");

        final User mockUser = mock(User.class);
        when(mockUser.getSid()).thenReturn(UUID.randomUUID());
        when(mockUser.getEmail()).thenReturn("weirdcase@graveto.com");
        when(service.register(any(RegisterCommand.class))).thenReturn(mockUser);

        // Act
        mvc.post()
                .uri("/auth/register")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        final ArgumentCaptor<RegisterCommand> commandCaptor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(service).register(commandCaptor.capture());

        final RegisterCommand capturedCommand = commandCaptor.getValue();

        assertThat(capturedCommand.email()).isEqualTo("weirdcase@graveto.com");
        assertThat(capturedCommand.password()).isEqualTo("password123");
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyExists() {
        // Arrange
        final RegisterRequestDTO request = new RegisterRequestDTO("test@graveto.com", "password123");

        // Simulate the DB rejecting the duplicate email
        when(service.register(any(RegisterCommand.class)))
                .thenThrow(new UserAlreadyExistsException()); // Assuming this is your custom exception

        // Act & Assert
        final MvcTestResult testResult = mvc.post()
                .uri("/auth/register")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assuming your @RestControllerAdvice maps this specific exception to a 409
        assertThat(testResult).hasStatus(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @MethodSource("invalidRegisterRequests")
    void shouldReturnBadRequestForInvalidPayloadsOnRegisterRequest(
            final RegisterRequestDTO request,
            final String expectedErrorField) {

        final MvcTestResult testResult = mvc.post()
                .uri("/auth/register")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.invalid_params." + expectedErrorField);
    }

    private static Stream<Arguments> invalidRegisterRequests() {
        return Stream.of(
                // Email constraints
                Arguments.of(new RegisterRequestDTO("  ", "password123"), "email"),
                Arguments.of(new RegisterRequestDTO("", "password123"), "email"),
                Arguments.of(new RegisterRequestDTO(null, "password123"), "email"),
                Arguments.of(new RegisterRequestDTO("not-an-email", "password123"), "email"),
                Arguments.of(new RegisterRequestDTO("email@email@", "password123"), "email"),

                // Password constraints
                Arguments.of(new RegisterRequestDTO("test@graveto.com", "    "), "password"),
                Arguments.of(new RegisterRequestDTO("test@graveto.com", ""), "password"),
                Arguments.of(new RegisterRequestDTO("test@graveto.com", null), "password"),
                Arguments.of(new RegisterRequestDTO("test@graveto.com", "1234"), "password")
        );
    }

}
