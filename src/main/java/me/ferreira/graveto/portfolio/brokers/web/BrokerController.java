package me.ferreira.graveto.portfolio.brokers.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;
import me.ferreira.graveto.portfolio.brokers.web.dto.request.CreateBrokerRequestDto;
import me.ferreira.graveto.portfolio.brokers.web.dto.response.BrokerResponseDto;
import me.ferreira.graveto.portfolio.brokers.web.dto.response.BrokerSummaryResponseDto;
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

    return ResponseEntity.created(location).body(toResponse(brokerDetails));
  }

  @GetMapping(value = BROKER_SID_PATH, produces = "application/json")
  public ResponseEntity<BrokerResponseDto> fetchBroker(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final FetchBrokerCommand command = new FetchBrokerCommand(userSid, sid);

    final BrokerDetails brokerDetails = brokerService.fetchBroker(command);

    return ResponseEntity.ok(toResponse(brokerDetails));
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<BrokerSummaryResponseDto>> fetchAllBrokers(@AuthenticationPrincipal final UUID userSid) {

    final List<Broker> brokers = brokerService.fetchAllBrokers(userSid);

    final List<BrokerSummaryResponseDto> response = brokers.stream()
        .map(b -> new BrokerSummaryResponseDto(
            b.getSid(),
            b.getName(),
            b.getCurrency().name(),
            b.getAccountSid(),
            b.getStatus().name()))
        .toList();

    return ResponseEntity.ok(response);
  }

  private BrokerResponseDto toResponse(final BrokerDetails brokerDetails) {

    final List<BrokerResponseDto.MembershipResponseDto> membershipResponseDto = brokerDetails.users().stream()
        .map(at -> new BrokerResponseDto.MembershipResponseDto(
            at.sid(),
            at.email(),
            at.role()
        ))
        .toList();

    return new BrokerResponseDto(
        brokerDetails.sid(),
        brokerDetails.name(),
        brokerDetails.status().name(),
        brokerDetails.currency().name(),
        brokerDetails.accountSid(),
        membershipResponseDto
    );
  }

}
