package me.ferreira.graveto.common.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import me.ferreira.graveto.common.mail.domain.EmailType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
public class EmailTemplateResolverTest {

  @InjectMocks
  private EmailTemplateResolver resolver;
  @Mock
  private MessageSource messageSource;

  @Test
  void shouldResolveSubjectUsingEmailTypeSubjectKey() {
    // Arrange
    when(messageSource.getMessage(eq(EmailType.PASSWORD_RESET.getSubjectPropertyKey()), any(), eq(null)))
        .thenReturn("Action Required: Password Reset Request");

    // Act
    final String subject = resolver.getSubject(EmailType.PASSWORD_RESET);

    // Assert
    assertThat(subject).isEqualTo("Action Required: Password Reset Request");
  }

  @Test
  void shouldResolveContentUsingEmailTypeContentKeyAndArgs() {
    // Arrange
    final Object[] args = {"RAW_TOKEN", 30L};
    when(messageSource.getMessage(eq(EmailType.PASSWORD_RESET.getContentPropertyKey()), eq(args), eq(null)))
        .thenReturn("Your password reset code is:\nRAW_TOKEN\n\nThis code will expire in 30 minutes.");

    // Act
    final String content = resolver.getContent(EmailType.PASSWORD_RESET, "RAW_TOKEN", 30L);

    // Assert
    assertThat(content).isEqualTo("Your password reset code is:\nRAW_TOKEN\n\nThis code will expire in 30 minutes.");
  }

}
