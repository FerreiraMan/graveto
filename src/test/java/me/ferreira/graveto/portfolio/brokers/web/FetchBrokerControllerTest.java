package me.ferreira.graveto.portfolio.brokers.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.BrokerNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.moneytracker.utils.common.AuthUtils;
import me.ferreira.graveto.moneytracker.utils.common.TestSecurityConfig;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;
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

@WebMvcTest(
    controllers = BrokerController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FetchBrokerControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private BrokerService service;

  @Test
  void shouldFetchBrokerSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();

    final BrokerDetails mockDetails = new BrokerDetails(brokerSid, "DEGIRO", BrokerStatus.ACTIVE,
        Currency.EUR, null,
        List.of(new BrokerDetails.MembershipDetails(userSid, "user@example.com", "OWNER")));

    when(service.fetchBroker(org.mockito.ArgumentMatchers.any())).thenReturn(mockDetails);

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(brokerSid.toString());
    assertThat(result).bodyJson().extractingPath("$.name").asString().isEqualTo("DEGIRO");
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo(BrokerStatus.ACTIVE.name());
    assertThat(result).bodyJson().extractingPath("$.currency").asString().isEqualTo(Currency.EUR.name());
    assertThat(result).bodyJson().extractingPath("$.users[0].email").asString().isEqualTo("user@example.com");
    assertThat(result).bodyJson().extractingPath("$.users[0].role").asString().isEqualTo("OWNER");
  }

  @Test
  void shouldReturnNotFoundWhenBrokerDoesNotExist() {
    // Arrange
    final UUID brokerSid = UUID.randomUUID();
    when(service.fetchBroker(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BrokerNotFoundException(brokerSid));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfBroker() {
    // Arrange
    final UUID brokerSid = UUID.randomUUID();
    when(service.fetchBroker(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new InsufficientPermissionsOnBrokerException("view broker"));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldReturnBadRequestForInvalidBrokerSid() {
    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/not-a-uuid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

}
