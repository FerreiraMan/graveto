package me.ferreira.graveto.portfolio.brokers.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.web.request.CreateBrokerRequestDto;
import me.ferreira.graveto.portfolio.brokers.web.response.BrokerResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/brokers")
@RequiredArgsConstructor
public class BrokerController {

  private static final String BROKER_SID_PATH = "/{sid}";

  private final BrokerService brokerService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<BrokerResponseDto> createBroker(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateBrokerRequestDto requestDto) {

    final CreateBrokerCommand command = new CreateBrokerCommand(
        userSid,
        requestDto.accountSid(),
        requestDto.name().trim(),
        requestDto.currency()
    );

    final Broker createdBroker = brokerService.createBroker(command);

    final BrokerResponseDto response = new BrokerResponseDto(
        createdBroker.getSid(),
        createdBroker.getName(),
        createdBroker.getStatus().name()
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(BROKER_SID_PATH)
        .buildAndExpand(createdBroker.getSid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

}
