package me.ferreira.graveto.common.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import me.ferreira.graveto.common.web.exception.identity.TokenAuthenticationException;
import me.ferreira.graveto.common.web.exception.identity.UserAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.IllegalCategoryHierarchyException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String DEFAULT_TYPE = "about:blank";

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ProblemDetail handleMissingRequestHeaderException(final MissingRequestHeaderException ex,
                                                           final HttpServletRequest request) {

    final String detailMessage = "Request validation failed. Please check the 'missing_param' property for details.";
    final ProblemDetail pd = createBaseProblemDetail(HttpStatus.BAD_REQUEST, detailMessage, request);

    final Map<String, String> missingParam = new HashMap<>();
    missingParam.put("header_name", ex.getHeaderName());
    missingParam.put("header_type", ex.getParameter().getParameterType().getSimpleName());
    pd.setProperty("missing_param", missingParam);

    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex,
                                                             final HttpServletRequest request) {

    final String detailMessage = "Request validation failed. Please check the 'invalid_params' property for details.";

    final ProblemDetail pd = createBaseProblemDetail(HttpStatus.BAD_REQUEST, detailMessage, request);

    final Map<String, String> invalidParams = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      invalidParams.put(error.getField(), error.getDefaultMessage());
    }
    pd.setProperty("invalid_params", invalidParams);

    return pd;
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

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalStateException(final IllegalStateException ex, final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgumentException(final IllegalArgumentException ex,
                                                      final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ProblemDetail handleAccountNotFoundException(final AccountNotFoundException ex,
                                                      final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(CategoryAlreadyExistsException.class)
  public ProblemDetail handleCategoryAlreadyExistsException(final CategoryAlreadyExistsException ex,
                                                            final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(CategoryNotFoundException.class)
  public ProblemDetail handleCategoryNotFoundException(final CategoryNotFoundException ex,
                                                       final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(IllegalCategoryHierarchyException.class)
  public ProblemDetail handleIllegalCategoryHierarchyException(final IllegalCategoryHierarchyException ex,
                                                               final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
  }

  @ExceptionHandler(InsufficientPermissionsException.class)
  public ProblemDetail handleInsufficientPermissionsException(final InsufficientPermissionsException ex,
                                                              final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ProblemDetail handleUserAlreadyExistsExceptionException(final UserAlreadyExistsException ex,
                                                                 final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(TokenAuthenticationException.class)
  public ProblemDetail handleTokenAuthenticationExceptionException(final TokenAuthenticationException ex,
                                                                   final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
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
