package me.ferreira.graveto.common.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.web.exception.identity.TokenAuthenticationException;
import me.ferreira.graveto.common.web.exception.identity.UserAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.IllegalCategoryHierarchyException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.common.web.exception.moneytracker.MemberNotRegisteredException;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.UserAlreadyAccountMemberException;
import me.ferreira.graveto.common.web.exception.moneytracker.UserNotMemberOfAccountException;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.common.web.exception.portfolio.UserAlreadyBrokerMemberException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
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

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgumentException(final IllegalArgumentException ex,
                                                      final HttpServletRequest request) {

    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ProblemDetail handleAccountNotFoundException(final AccountNotFoundException ex,
                                                      final HttpServletRequest request) {

    log.warn("Resource not found or lack of permission to view it. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(TransactionNotFoundException.class)
  public ProblemDetail handleTransactionNotFoundException(final TransactionNotFoundException ex,
                                                          final HttpServletRequest request) {

    log.warn("Resource not found or lack of permission to view it. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(CategoryAlreadyExistsException.class)
  public ProblemDetail handleCategoryAlreadyExistsException(final CategoryAlreadyExistsException ex,
                                                            final HttpServletRequest request) {

    log.warn("Business rule violation: Category already exists. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(CategoryNotFoundException.class)
  public ProblemDetail handleCategoryNotFoundException(final CategoryNotFoundException ex,
                                                       final HttpServletRequest request) {

    log.warn("Resource not found or lack of permission to view it. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(IllegalCategoryHierarchyException.class)
  public ProblemDetail handleIllegalCategoryHierarchyException(final IllegalCategoryHierarchyException ex,
                                                               final HttpServletRequest request) {

    log.warn("Business rule violation: Parent category does not belong to user. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
  }

  @ExceptionHandler(InsufficientPermissionsOnAccountException.class)
  public ProblemDetail handleInsufficientPermissionsException(final InsufficientPermissionsOnAccountException ex,
                                                              final HttpServletRequest request) {

    log.warn("Business rule violation: User does not have required permission. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }

  @ExceptionHandler(InsufficientPermissionsOnBrokerException.class)
  public ProblemDetail handleInsufficientPermissionsOnBrokerException(final InsufficientPermissionsOnBrokerException ex,
                                                                      final HttpServletRequest request) {

    log.warn("Business rule violation: User does not have required permission. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }

  @ExceptionHandler(MemberNotRegisteredException.class)
  public ProblemDetail handleMemberNotRegisteredException(final MemberNotRegisteredException ex,
                                                          final HttpServletRequest request) {

    log.warn("Business rule violation: Invitation on user that is not registered yet. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
  }

  @ExceptionHandler(UserNotMemberOfAccountException.class)
  public ProblemDetail handleUserIsNotMemberOfAccountException(final UserNotMemberOfAccountException ex,
                                                               final HttpServletRequest request) {

    log.warn("Business rule violation: User is not a member of the account. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }

  @ExceptionHandler(UserAlreadyAccountMemberException.class)
  public ProblemDetail handleUserAlreadyMemberException(final UserAlreadyAccountMemberException ex,
                                                        final HttpServletRequest request) {

    log.warn("Business rule violation: User is already member of account. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
  }

  @ExceptionHandler(UserAlreadyBrokerMemberException.class)
  public ProblemDetail handleUserAlreadyBrokerMemberException(final UserAlreadyBrokerMemberException ex,
                                                              final HttpServletRequest request) {

    log.warn("Business rule violation: User is already member of broker account. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ProblemDetail handleUserAlreadyExistsException(final UserAlreadyExistsException ex,
                                                        final HttpServletRequest request) {

    log.warn("Business rule violation: User already exists. Message: {}", ex.getMessage());
    return createBaseProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
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

  @ExceptionHandler(TokenAuthenticationException.class)
  public ProblemDetail handleTokenAuthenticationExceptionException(final TokenAuthenticationException ex,
                                                                   final HttpServletRequest request) {

    log.error("Error with Jwt verification.", ex);
    return createBaseProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
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
