package me.ferreira.graveto.portfolio.brokers.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = BrokerController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FetchAllBrokersControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private BrokerService service;

  @Test
  void shouldFetchAllBrokersSuccessfully() {
    // Arrange
    final UUID brokerSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final String name = "Degiro";

    final Broker mockBroker = new Broker();
    mockBroker.setSid(brokerSid);
    mockBroker.setAccountSid(accountSid);
    mockBroker.setName(name);
    mockBroker.setCurrency(Currency.EUR);
    mockBroker.setStatus(BrokerStatus.ACTIVE);

    when(service.fetchAllBrokers(userSid)).thenReturn(List.of(mockBroker));

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/brokers")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].sid").asString().isEqualTo(brokerSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].status").asString().isEqualTo(BrokerStatus.ACTIVE.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].name").asString().isEqualTo(name);
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].baseCurrency").asString().isEqualTo(Currency.EUR.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].accountSid").asString().isEqualTo(accountSid.toString());
  }

}
