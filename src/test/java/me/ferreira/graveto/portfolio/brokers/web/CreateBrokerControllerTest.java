package me.ferreira.graveto.portfolio.brokers.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.utils.common.AuthUtils;
import me.ferreira.graveto.moneytracker.utils.common.TestSecurityConfig;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.web.request.CreateBrokerRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class CreateBrokerControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private BrokerService service;

  private static Stream<Arguments> invalidBrokerCreationRequests() {
    return Stream.of(
        Arguments.of(new CreateBrokerRequestDto(null, null, Currency.EUR), "name"),
        Arguments.of(new CreateBrokerRequestDto(null, "  ", Currency.EUR), "name"),
        Arguments.of(new CreateBrokerRequestDto(null, "DEGIRO", null), "currency")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidBrokerCreationRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnBrokerCreation(
      final CreateBrokerRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.post()
        .uri("/brokers")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

  @Test
  void shouldCreateNewBroker() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final CreateBrokerRequestDto request = new CreateBrokerRequestDto(accountSid, "DEGIRO", Currency.EUR);

    final Broker mockBroker = new Broker();
    mockBroker.setSid(brokerSid);
    mockBroker.setName("DEGIRO");
    mockBroker.setStatus(BrokerStatus.ACTIVE);

    final ArgumentCaptor<CreateBrokerCommand> commandCaptor = ArgumentCaptor.forClass(CreateBrokerCommand.class);
    when(service.createBroker(commandCaptor.capture())).thenReturn(mockBroker);

    // Act
    final MvcTestResult result = mvc.post()
        .uri("/brokers")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(result).hasHeader("Location", "http://localhost/brokers/" + brokerSid);

    final CreateBrokerCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.accountSid()).isEqualTo(accountSid);
    assertThat(captured.name()).isEqualTo("DEGIRO");
    assertThat(captured.currency()).isEqualTo(Currency.EUR);

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(brokerSid.toString());
    assertThat(result).bodyJson().extractingPath("$.name").asString().isEqualTo("DEGIRO");
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("ACTIVE");
  }

  @Test
  void shouldCreateBrokerWithoutAccountSid() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateBrokerRequestDto request = new CreateBrokerRequestDto(null, "Trading 212", Currency.EUR);

    final Broker mockBroker = new Broker();
    mockBroker.setSid(UUID.randomUUID());
    mockBroker.setName("Trading 212");
    mockBroker.setStatus(BrokerStatus.ACTIVE);

    final ArgumentCaptor<CreateBrokerCommand> commandCaptor = ArgumentCaptor.forClass(CreateBrokerCommand.class);
    when(service.createBroker(commandCaptor.capture())).thenReturn(mockBroker);

    // Act
    final MvcTestResult result = mvc.post()
        .uri("/brokers")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(commandCaptor.getValue().accountSid()).isNull();
  }

  @Test
  void shouldTrimNameBeforePassingToService() {
    // Arrange
    final CreateBrokerRequestDto request = new CreateBrokerRequestDto(null, "  DEGIRO  ", Currency.EUR);

    final Broker mockBroker = new Broker();
    mockBroker.setSid(UUID.randomUUID());
    mockBroker.setName("DEGIRO");
    mockBroker.setStatus(BrokerStatus.ACTIVE);

    final ArgumentCaptor<CreateBrokerCommand> commandCaptor = ArgumentCaptor.forClass(CreateBrokerCommand.class);
    when(service.createBroker(commandCaptor.capture())).thenReturn(mockBroker);

    // Act
    mvc.post()
        .uri("/brokers")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(commandCaptor.getValue().name()).isEqualTo("DEGIRO");
  }

}
