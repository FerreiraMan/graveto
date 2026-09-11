package me.ferreira.graveto.common.mail.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import me.ferreira.graveto.common.mail.EmailTemplateResolver;
import me.ferreira.graveto.common.mail.domain.EmailType;
import me.ferreira.graveto.common.mail.domain.event.PasswordResetEmailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordEmailEventListenerTest {

  @InjectMocks
  private EmailEventListener listener;
  @Mock
  private JavaMailSender mailSender;
  @Mock
  private EmailTemplateResolver templateResolver;

  @Test
  void shouldSendEmailWithResolvedSubjectAndContentOnPasswordResetEvent() {
    // Arrange
    final PasswordResetEmailEvent event = new PasswordResetEmailEvent("test@graveto.com", "raw-token", 30L);

    when(templateResolver.getSubject(EmailType.PASSWORD_RESET)).thenReturn("Password Reset Request");
    when(templateResolver.getContent(EmailType.PASSWORD_RESET, "raw-token", 30L))
        .thenReturn("Your password reset code is:\nraw-token\n\nThis code will expire in 30 minutes.");

    // Act
    listener.onPasswordReset(event);

    // Assert
    final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender, times(1)).send(captor.capture());

    final SimpleMailMessage sentMessage = captor.getValue();
    assertThat(sentMessage.getTo()).containsExactly("test@graveto.com");
    assertThat(sentMessage.getSubject()).isEqualTo("Password Reset Request");
    assertThat(sentMessage.getText())
        .isEqualTo("Your password reset code is:\nraw-token\n\nThis code will expire in 30 minutes.");
  }

  @Test
  void shouldSwallowMailExceptionWithoutPropagating() {
    // Arrange
    final PasswordResetEmailEvent event = new PasswordResetEmailEvent("test@graveto.com", "raw-token", 30L);

    when(templateResolver.getSubject(EmailType.PASSWORD_RESET)).thenReturn("Password Reset Request");
    when(templateResolver.getContent(EmailType.PASSWORD_RESET, "raw-token", 30L)).thenReturn("body");

    final MailException failure = new MailSendException("SMTP unreachable");
    doThrow(failure).when(mailSender).send(any(SimpleMailMessage.class));

    // Act & Assert
    assertThatNoException().isThrownBy(() -> listener.onPasswordReset(event));
    verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
  }

}
