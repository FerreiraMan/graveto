package me.ferreira.graveto.common.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.ferreira.graveto.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
    controllers = GlobalExceptionHandlerTest.StubController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import({TestSecurityConfig.class, GlobalExceptionHandlerTest.StubController.class})
public class GlobalExceptionHandlerTest {

  private static final String SAFE_MESSAGE = "SAFE-MESSAGE shown to the client.";
  private static final String LOGGABLE_MESSAGE = "LOGGABLE-DETAIL user=42 internal-state";

  @Autowired
  private MockMvcTester mvc;

  private final Logger handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void attachLogAppender() {
    logAppender = new ListAppender<>();
    logAppender.start();
    handlerLogger.addAppender(logAppender);
  }

  @AfterEach
  void detachLogAppender() {
    handlerLogger.detachAppender(logAppender);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 409, 422, 429, 500, 502, 503})
  void shouldExposeOnlyTheSafeMessageAndDeclaredStatus(final int status) {
    // Act
    final MvcTestResult result = throwApplicationException(status, Level.WARN);

    // Assert
    assertThat(result).hasStatus(status);
    assertThat(result).bodyJson().extractingPath("$.detail").asString().isEqualTo(SAFE_MESSAGE);
    assertThat(result).bodyText().doesNotContain(LOGGABLE_MESSAGE);
  }

  @Test
  void shouldReturnTheProblemDetailEnvelope() {
    // Act
    final MvcTestResult result = throwApplicationException(404, Level.WARN);

    // Assert
    assertThat(result).bodyJson().extractingPath("$.title").asString()
        .isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    assertThat(result).bodyJson().extractingPath("$.type").asString().isEqualTo("about:blank");
    assertThat(result).bodyJson().extractingPath("$.instance").asString().isEqualTo("/stub/application-exception");
    assertThat(result).bodyJson().hasPath("$.timestamp");
  }

  @ParameterizedTest
  @EnumSource(value = Level.class, names = {"INFO", "WARN", "ERROR"})
  void shouldLogTheLoggableMessageAtTheLevelDeclaredByTheException(final Level declaredLevel) {
    // Act
    throwApplicationException(404, declaredLevel);

    // Assert
    assertThat(logAppender.list).hasSize(1);
    final ILoggingEvent event = logAppender.list.getFirst();
    assertThat(event.getLevel().toString()).isEqualTo(declaredLevel.name());
    assertThat(event.getFormattedMessage()).isEqualTo(LOGGABLE_MESSAGE);
  }

  @ParameterizedTest
  @CsvSource({
      "400, false",
      "404, false",
      "409, false",
      "422, false",
      "500, true",
      "502, true",
      "503, true"
  })
  void shouldAttachTheStackTraceOnlyForServerErrors(final int status, final boolean expectStackTrace) {
    // Act
    throwApplicationException(status, Level.ERROR);

    // Assert
    assertThat(logAppender.list).hasSize(1);
    assertThat(logAppender.list.getFirst().getThrowableProxy() != null).isEqualTo(expectStackTrace);
  }

  @Test
  void shouldKeepEveryViolationMessageForTheSameField() {
    // Act
    final MvcTestResult result = postValidated("{\"name\":\"  \",\"age\":30,\"code\":\"ok\"}");

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.invalid_params.name").asString()
        .isEqualTo("Name cannot be empty. Name is too short.");
  }

  @Test
  void shouldReturnOneEntryPerInvalidFieldWithFixedDisplayableDetail() {
    // Act
    final MvcTestResult result = postValidated("{\"name\":\"valid name\",\"code\":\"too-long\"}");

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.detail").asString()
        .isEqualTo("Some of the submitted fields are invalid.");
    assertThat(result).bodyJson().extractingPath("$.invalid_params.age").asString().isEqualTo("Age is required.");
    assertThat(result).bodyJson().extractingPath("$.invalid_params.code").asString()
        .isEqualTo("Code is too long.");
    assertThat(result).bodyJson().doesNotHavePath("$.invalid_params.name");
  }

  @Test
  void shouldNeverEchoTheRejectedValueBackToTheClient() {
    // Act
    final MvcTestResult result = postValidated("{\"name\":\"valid name\",\"age\":30,\"code\":\"secret-value-xyz\"}");

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyText().doesNotContain("secret-value-xyz");
  }

  private MvcTestResult throwApplicationException(final int status, final Level level) {
    return mvc.get()
        .uri("/stub/application-exception")
        .param("status", String.valueOf(status))
        .param("level", level.name())
        .exchange();
  }

  private MvcTestResult postValidated(final String json) {
    return mvc.post()
        .uri("/stub/validated")
        .content(json)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();
  }

  static class StubApplicationException extends ApplicationException {
    StubApplicationException(final HttpStatus status, final Level level) {
      super(LOGGABLE_MESSAGE, status, SAFE_MESSAGE, level);
    }
  }

  record StubRequest(
      @NotBlank(message = "Name cannot be empty.")
      @Size(min = 3, message = "Name is too short.")
      String name,

      @NotNull(message = "Age is required.")
      Integer age,

      @Size(max = 5, message = "Code is too long.")
      String code
  ) {
  }

  @RestController
  @RequestMapping("/stub")
  static class StubController {

    @GetMapping("/application-exception")
    public void throwApplicationException(@RequestParam final int status, @RequestParam final Level level) {
      throw new StubApplicationException(HttpStatus.valueOf(status), level);
    }

    @PostMapping("/validated")
    public void validated(@Valid @RequestBody final StubRequest request) {
    }

  }

}
