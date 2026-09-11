package me.ferreira.graveto.common.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageSourceConfiguration {

  @Bean
  public MessageSource messageSource() {
    final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("message");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setDefaultLocale(Locale.ENGLISH);
    return messageSource;
  }

}
