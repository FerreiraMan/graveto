package me.ferreira.graveto.common.mail;

import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.common.mail.domain.EmailType;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailTemplateResolver {

  private final MessageSource messageSource;

  public String getSubject(final EmailType emailType, final Object... args) {

    return messageSource.getMessage(emailType.getSubjectPropertyKey(), args, null);
  }

  public String getContent(final EmailType emailType, final Object... args) {

    return messageSource.getMessage(emailType.getContentPropertyKey(), args, null);
  }

}
