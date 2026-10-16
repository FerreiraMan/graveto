package me.ferreira.graveto.portfolio.brokers.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.AddMemberToBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;
import me.ferreira.graveto.portfolio.brokers.web.dto.request.AddMemberToBrokerRequestDto;
import me.ferreira.graveto.portfolio.brokers.web.dto.request.CreateBrokerRequestDto;
import me.ferreira.graveto.portfolio.brokers.web.dto.response.BrokerResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private static final String MEMBERSHIP_PATH = "/memberships";

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

    final BrokerDetails brokerDetails = brokerService.createBroker(command);

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(BROKER_SID_PATH)
        .buildAndExpand(brokerDetails.sid())
        .toUri();

    return ResponseEntity.created(location).body(BrokerResponseDto.from(brokerDetails));
  }

  @GetMapping(value = BROKER_SID_PATH, produces = "application/json")
  public ResponseEntity<BrokerResponseDto> fetchBroker(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final FetchBrokerCommand command = new FetchBrokerCommand(userSid, sid);

    final BrokerDetails brokerDetails = brokerService.fetchBroker(command);

    return ResponseEntity.ok(BrokerResponseDto.from(brokerDetails));
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<BrokerResponseDto>> fetchAllBrokers(@AuthenticationPrincipal final UUID userSid) {

    final List<Broker> brokers = brokerService.fetchAllBrokers(userSid);

    return ResponseEntity.ok().body(brokers.stream().map(BrokerResponseDto::from).toList());
  }

  @PostMapping(path = BROKER_SID_PATH + MEMBERSHIP_PATH, produces = "application/json")
  public ResponseEntity<BrokerResponseDto> addMemberToBroker(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid,
      @Valid @RequestBody final AddMemberToBrokerRequestDto requestDto) {

    final AddMemberToBrokerCommand command = new AddMemberToBrokerCommand(
        userSid,
        sid,
        requestDto.email().trim().toLowerCase(),
        requestDto.role()
    );

    final BrokerDetails brokerDetails = brokerService.addMember(command);

    return ResponseEntity.ok().body(BrokerResponseDto.from(brokerDetails));
  }

}
