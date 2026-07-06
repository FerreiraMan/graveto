package me.ferreira.graveto.portfolio.orders.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.service.OrderService;
import me.ferreira.graveto.portfolio.orders.service.command.CreateOrderCommand;
import me.ferreira.graveto.portfolio.orders.service.command.UpdateOrderCommand;
import me.ferreira.graveto.portfolio.orders.web.dto.request.CreateOrderRequestDto;
import me.ferreira.graveto.portfolio.orders.web.dto.request.UpdateOrderRequestDto;
import me.ferreira.graveto.portfolio.orders.web.dto.response.OrderResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/orders")
@RequiredArgsConstructor
public class OrderController {

  private static final String ORDER_SID_PATH = "/{sid}";

  private final OrderService orderService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<OrderResponseDto> createOrder(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateOrderRequestDto requestDto) {

    final CreateOrderCommand command = new CreateOrderCommand(
        userSid,
        requestDto.brokerSid(),
        requestDto.assetSid(),
        requestDto.orderType(),
        requestDto.quantity(),
        requestDto.price(),
        requestDto.fees(),
        requestDto.currency(),
        Objects.isNull(requestDto.executedAt()) ? LocalDateTime.now() : requestDto.executedAt(),
        StringUtils.trimToNull(requestDto.notes())
    );

    final Order createdOrder = orderService.createOrder(command);

    final OrderResponseDto response = new OrderResponseDto(
        createdOrder.getSid(),
        new OrderResponseDto.EnhancedInfoObject(createdOrder.getBroker().getSid(),
            createdOrder.getBroker().getName()),
        new OrderResponseDto.EnhancedInfoObject(createdOrder.getAsset().getSid(),
            createdOrder.getAsset().getTicker()),
        createdOrder.getOrderType().name(),
        createdOrder.getQuantity(),
        createdOrder.getPricePerUnit(),
        createdOrder.getFees(),
        createdOrder.getCurrency().name(),
        createdOrder.getExecutedAt(),
        createdOrder.getNotes()
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(ORDER_SID_PATH)
        .buildAndExpand(createdOrder.getSid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @PatchMapping(produces = "application/json")
  public ResponseEntity<OrderResponseDto> updateOrder(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final UpdateOrderRequestDto requestDto) {

    final UpdateOrderCommand command = new UpdateOrderCommand(
        userSid,
        requestDto.orderSid(),
        requestDto.quantity(),
        requestDto.price(),
        requestDto.fees(),
        requestDto.executedAt(),
        requestDto.notes()
    );

    final Order updatedOrder = orderService.updateOrder(command);

    final OrderResponseDto response = new OrderResponseDto(
        updatedOrder.getSid(),
        new OrderResponseDto.EnhancedInfoObject(updatedOrder.getBroker().getSid(),
            updatedOrder.getBroker().getName()),
        new OrderResponseDto.EnhancedInfoObject(updatedOrder.getAsset().getSid(),
            updatedOrder.getAsset().getTicker()),
        updatedOrder.getOrderType().name(),
        updatedOrder.getQuantity(),
        updatedOrder.getPricePerUnit(),
        updatedOrder.getFees(),
        updatedOrder.getCurrency().name(),
        updatedOrder.getExecutedAt(),
        updatedOrder.getNotes()
    );

    return ResponseEntity.ok().body(response);
  }

}
