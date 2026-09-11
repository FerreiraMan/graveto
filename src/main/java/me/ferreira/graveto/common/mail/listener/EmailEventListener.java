package me.ferreira.graveto.common.mail.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.mail.EmailTemplateResolver;
import me.ferreira.graveto.common.mail.domain.EmailType;
import me.ferreira.graveto.common.mail.domain.event.PasswordResetEmailEvent;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@AllArgsConstructor
public class EmailEventListener {

  private final JavaMailSender mailSender;
  private final EmailTemplateResolver templateResolver;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPasswordReset(final PasswordResetEmailEvent event) {
    send(event.destinationEmail(), EmailType.PASSWORD_RESET, event.token(), event.expiresInMinutes());
  }

  private void send(final String to, final EmailType type, final Object... args) {
    try {
      final SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(to);
      message.setSubject(templateResolver.getSubject(type));
      message.setText(templateResolver.getContent(type, args));
      mailSender.send(message);
    } catch (final MailException e) {
      log.error("Failed to send [{}] email to [{}]", type.name(), to, e);
    }
  }

}
