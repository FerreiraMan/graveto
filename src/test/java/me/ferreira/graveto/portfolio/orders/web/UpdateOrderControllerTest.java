package me.ferreira.graveto.portfolio.orders.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import me.ferreira.graveto.portfolio.orders.service.command.UpdateOrderCommand;
import me.ferreira.graveto.portfolio.orders.web.dto.request.UpdateOrderRequestDto;
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
public class UpdateOrderControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private OrderService service;

  private static Stream<Arguments> invalidUpdateOrderRequests() {
    return Stream.of(
        Arguments.of(new UpdateOrderRequestDto(null, BigDecimal.TEN, BigDecimal.TEN, null, null, null), "orderSid"),
        Arguments.of(new UpdateOrderRequestDto(UUID.randomUUID(), BigDecimal.ZERO, BigDecimal.TEN, null, null, null),
            "quantity"),
        Arguments.of(
            new UpdateOrderRequestDto(UUID.randomUUID(), BigDecimal.TEN.negate(), BigDecimal.TEN, null, null, null),
            "quantity"),
        Arguments.of(new UpdateOrderRequestDto(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ZERO, null, null, null),
            "price"),
        Arguments.of(
            new UpdateOrderRequestDto(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.TEN.negate(), null, null, null),
            "price")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidUpdateOrderRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnOrderUpdate(
      final UpdateOrderRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.patch()
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
  void shouldUpdateOrderSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final UUID assetSid = UUID.randomUUID();
    final BigDecimal newQuantity = new BigDecimal("15");
    final BigDecimal newPrice = new BigDecimal("75.00");
    final BigDecimal newFees = new BigDecimal("3.00");
    final LocalDateTime newExecutedAt = LocalDateTime.now().minusDays(2);

    final UpdateOrderRequestDto request = new UpdateOrderRequestDto(
        orderSid, newQuantity, newPrice, newFees, newExecutedAt, "updated note");

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
    mockOrder.setQuantity(newQuantity);
    mockOrder.setPricePerUnit(newPrice);
    mockOrder.setFees(newFees);
    mockOrder.setCurrency(Currency.EUR);
    mockOrder.setExecutedAt(newExecutedAt);
    mockOrder.setNotes("updated note");

    final ArgumentCaptor<UpdateOrderCommand> commandCaptor = ArgumentCaptor.forClass(UpdateOrderCommand.class);
    when(service.updateOrder(commandCaptor.capture())).thenReturn(mockOrder);

    // Act
    final MvcTestResult result = mvc.patch()
        .uri("/orders")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final UpdateOrderCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.sid()).isEqualTo(orderSid);
    assertThat(captured.quantity()).isEqualByComparingTo(newQuantity);
    assertThat(captured.price()).isEqualByComparingTo(newPrice);
    assertThat(captured.fees()).isEqualByComparingTo(newFees);
    assertThat(captured.executedAt()).isEqualTo(newExecutedAt);
    assertThat(captured.notes()).isEqualTo("updated note");

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(orderSid.toString());
    assertThat(result).bodyJson().extractingPath("$.broker.sid").asString().isEqualTo(brokerSid.toString());
    assertThat(result).bodyJson().extractingPath("$.asset.sid").asString().isEqualTo(assetSid.toString());
    assertThat(result).bodyJson().extractingPath("$.orderType").asString().isEqualTo("BUY");
    assertThat(result).bodyJson().extractingPath("$.notes").asString().isEqualTo("updated note");
  }

  @Test
  void shouldAllowPartialUpdate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();

    final UpdateOrderRequestDto request = new UpdateOrderRequestDto(
        orderSid, new BigDecimal("20"), null, null, null, null);

    final Broker mockBroker = new Broker();
    mockBroker.setSid(UUID.randomUUID());
    mockBroker.setName("DEGIRO");

    final Asset mockAsset = new Asset();
    mockAsset.setSid(UUID.randomUUID());
    mockAsset.setTicker("IWDA");

    final Order mockOrder = new Order();
    mockOrder.setSid(orderSid);
    mockOrder.setBroker(mockBroker);
    mockOrder.setAsset(mockAsset);
    mockOrder.setOrderType(OrderType.BUY);
    mockOrder.setQuantity(new BigDecimal("20"));
    mockOrder.setPricePerUnit(new BigDecimal("72.50"));
    mockOrder.setFees(BigDecimal.ZERO);
    mockOrder.setCurrency(Currency.EUR);
    mockOrder.setExecutedAt(LocalDateTime.now());
    mockOrder.setNotes(null);

    final ArgumentCaptor<UpdateOrderCommand> commandCaptor = ArgumentCaptor.forClass(UpdateOrderCommand.class);
    when(service.updateOrder(commandCaptor.capture())).thenReturn(mockOrder);

    // Act
    final MvcTestResult result = mvc.patch()
        .uri("/orders")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final UpdateOrderCommand captured = commandCaptor.getValue();
    assertThat(captured.quantity()).isEqualByComparingTo(new BigDecimal("20"));
    assertThat(captured.price()).isNull();
    assertThat(captured.fees()).isNull();
    assertThat(captured.executedAt()).isNull();
    assertThat(captured.notes()).isNull();
  }

}
