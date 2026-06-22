package me.ferreira.graveto.portfolio.orders.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.orders.service.OrderService;
import me.ferreira.graveto.portfolio.orders.service.command.CreateOrderCommand;
import me.ferreira.graveto.portfolio.orders.web.dto.request.CreateOrderRequestDto;
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
    controllers = OrderController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateOrderControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private OrderService service;

  private static Stream<Arguments> invalidOrderCreationRequests() {
    return Stream.of(
        Arguments.of(new CreateOrderRequestDto(
            null, UUID.randomUUID(), OrderType.BUY,
            BigDecimal.TEN, BigDecimal.TEN, null, Currency.EUR, null, null), "brokerSid"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), null, OrderType.BUY,
            BigDecimal.TEN, BigDecimal.TEN, null, Currency.EUR, null, null), "assetSid"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), null,
            BigDecimal.TEN, BigDecimal.TEN, null, Currency.EUR, null, null), "orderType"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), OrderType.BUY,
            null, BigDecimal.TEN, null, Currency.EUR, null, null), "quantity"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), OrderType.BUY,
            BigDecimal.ZERO, BigDecimal.TEN, null, Currency.EUR, null, null), "quantity"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), OrderType.BUY,
            BigDecimal.TEN, null, null, Currency.EUR, null, null), "price"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), OrderType.BUY,
            BigDecimal.TEN, BigDecimal.ZERO, null, Currency.EUR, null, null), "price"),
        Arguments.of(new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), OrderType.BUY,
            BigDecimal.TEN, BigDecimal.TEN, null, null, null, null), "currency")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidOrderCreationRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnOrderCreation(
      final CreateOrderRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.post()
        .uri("/orders")
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
  void shouldCreateNewOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final UUID assetSid = UUID.randomUUID();
    final BigDecimal quantity = new BigDecimal("10");
    final BigDecimal price = new BigDecimal("72.50");
    final BigDecimal fees = new BigDecimal("2.00");
    final LocalDateTime executedAt = LocalDateTime.now().minusDays(1);

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        brokerSid, assetSid, OrderType.BUY,
        quantity, price, fees, Currency.EUR, executedAt, "first buy"
    );

    final Broker mockBroker = new Broker();
    mockBroker.setSid(brokerSid);
    mockBroker.setName("DEGIRO");

    final Asset mockAsset = new Asset();
    mockAsset.setSid(assetSid);
    mockAsset.setTicker("IWDA");

    final Order mockOrder = new Order();
    mockOrder.setSid(orderSid);
    mockOrder.setBroker(mockBroker);
    mockOrder.setAsset(mockAsset);
    mockOrder.setOrderType(OrderType.BUY);
    mockOrder.setQuantity(quantity);
    mockOrder.setPricePerUnit(price);
    mockOrder.setFees(fees);
    mockOrder.setCurrency(Currency.EUR);
    mockOrder.setExecutedAt(executedAt);
    mockOrder.setNotes("first buy");

    final ArgumentCaptor<CreateOrderCommand> commandCaptor = ArgumentCaptor.forClass(CreateOrderCommand.class);
    when(service.createOrder(commandCaptor.capture())).thenReturn(mockOrder);

    // Act
    final MvcTestResult result = mvc.post()
        .uri("/orders")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(result).hasHeader("Location", "http://localhost/orders/" + orderSid);

    final CreateOrderCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.brokerSid()).isEqualTo(brokerSid);
    assertThat(captured.assetSid()).isEqualTo(assetSid);
    assertThat(captured.orderType()).isEqualTo(OrderType.BUY);
    assertThat(captured.quantity()).isEqualByComparingTo(quantity);
    assertThat(captured.price()).isEqualByComparingTo(price);
    assertThat(captured.fees()).isEqualByComparingTo(fees);
    assertThat(captured.currency()).isEqualTo(Currency.EUR);
    assertThat(captured.executedAt()).isEqualTo(executedAt);
    assertThat(captured.notes()).isEqualTo("first buy");

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(orderSid.toString());
    assertThat(result).bodyJson().extractingPath("$.broker.sid").asString().isEqualTo(brokerSid.toString());
    assertThat(result).bodyJson().extractingPath("$.broker.name").asString().isEqualTo("DEGIRO");
    assertThat(result).bodyJson().extractingPath("$.asset.sid").asString().isEqualTo(assetSid.toString());
    assertThat(result).bodyJson().extractingPath("$.asset.name").asString().isEqualTo("IWDA");
    assertThat(result).bodyJson().extractingPath("$.orderType").asString().isEqualTo("BUY");
    assertThat(result).bodyJson().extractingPath("$.currency").asString().isEqualTo("EUR");
    assertThat(result).bodyJson().extractingPath("$.notes").asString().isEqualTo("first buy");
  }

  @Test
  void shouldDefaultExecutedAtIfNoValueIsGivenOnOrderCreation() {
    // Arrange
    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        UUID.randomUUID(), UUID.randomUUID(), OrderType.BUY,
        BigDecimal.TEN, BigDecimal.TEN, null, Currency.EUR, null, null
    );

    final Order mockOrder = new Order();
    mockOrder.setSid(UUID.randomUUID());
    mockOrder.setBroker(new Broker());
    mockOrder.setAsset(new Asset());
    mockOrder.setOrderType(OrderType.BUY);
    mockOrder.setQuantity(BigDecimal.TEN);
    mockOrder.setPricePerUnit(BigDecimal.TEN);
    mockOrder.setFees(BigDecimal.ZERO);
    mockOrder.setCurrency(Currency.EUR);
    mockOrder.setExecutedAt(LocalDateTime.now());

    final ArgumentCaptor<CreateOrderCommand> commandCaptor = ArgumentCaptor.forClass(CreateOrderCommand.class);
    when(service.createOrder(commandCaptor.capture())).thenReturn(mockOrder);

    // Act
    mvc.post()
        .uri("/orders")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    final CreateOrderCommand captured = commandCaptor.getValue();
    assertThat(captured.executedAt()).isNotNull();
    assertThat(captured.executedAt().truncatedTo(ChronoUnit.MINUTES))
        .isEqualTo(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
  }

}
