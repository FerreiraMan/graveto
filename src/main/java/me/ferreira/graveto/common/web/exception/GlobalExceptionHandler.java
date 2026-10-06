package me.ferreira.graveto.common.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String DEFAULT_TYPE = "about:blank";

  @ExceptionHandler(ApplicationException.class)
  public ProblemDetail handleApplicationException(final ApplicationException ex,
                                                  final HttpServletRequest request) {

    final LoggingEventBuilder baseEvent = log.atLevel(ex.getLogLevel());
    final LoggingEventBuilder event = ex.getStatus().is5xxServerError() ? baseEvent.setCause(ex) : baseEvent;
    event.log(ex.getMessage());
    return createBaseProblemDetail(ex.getStatus(), ex.getSafeMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex,
                                                             final HttpServletRequest request) {

    final String detailMessage = "Some of the submitted fields are invalid.";

    final ProblemDetail pd = createBaseProblemDetail(HttpStatus.BAD_REQUEST, detailMessage, request);

    final Map<String, Set<String>> messagesByField = new TreeMap<>();
    for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
      messagesByField.computeIfAbsent(error.getField(), field -> new TreeSet<>())
          .add(Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value."));
    }

    final Map<String, String> invalidParams = new LinkedHashMap<>();
    messagesByField.forEach((field, messages) -> invalidParams.put(field, String.join(" ", messages)));
    pd.setProperty("invalid_params", invalidParams);

    return pd;
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ProblemDetail handleMissingServletRequestParameterException(final MissingServletRequestParameterException ex,
                                                                     final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolationException(final ConstraintViolationException ex,
                                                          final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException ex,
                                                                 final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ProblemDetail handleHttpRequestMethodNotSupportedException(final HttpRequestMethodNotSupportedException ex,
                                                                    final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadableException(final HttpMessageNotReadableException ex,
                                                             final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ProblemDetail handleUsernameNotFoundException(final UsernameNotFoundException ex,
                                                       final HttpServletRequest request) {

    log.warn("Business rule violation: Username was not found. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentialsExceptionException(final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
  }

  @ExceptionHandler(ResourceAccessException.class)
  public ProblemDetail handleExternalApiConnectivity(final ResourceAccessException ex,
                                                     final HttpServletRequest request) {

    log.warn("External API connectivity failure while accessing {}", request.getRequestURI(), ex);
    return createBaseProblemDetail(HttpStatus.SERVICE_UNAVAILABLE,
        "External service is currently unavailable. Please try again later.", request);
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleAllUncaughtExceptions(final Exception ex, final HttpServletRequest request) {

    log.error("CRITICAL: Unhandled system exception occurred while accessing {}", request.getRequestURI(), ex);
    return createBaseProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please contact support.", request);
  }

  private ProblemDetail createBaseProblemDetail(final HttpStatus status, final String detailMessage,
                                                final HttpServletRequest request) {
    final ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detailMessage);
    pd.setTitle(status.getReasonPhrase());
    pd.setType(URI.create(DEFAULT_TYPE));
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty("timestamp", Instant.now());
    return pd;
  }

}
