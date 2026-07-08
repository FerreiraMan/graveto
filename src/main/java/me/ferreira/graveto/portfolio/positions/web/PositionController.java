package me.ferreira.graveto.portfolio.positions.web;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPositionOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.payload.PositionValuation;
import me.ferreira.graveto.portfolio.positions.web.dto.response.PositionResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PositionController {

  private final PositionService positionService;

  private static final String POSITION_PATH = "/positions";
  private static final String BROKER_PATH = "/brokers";
  private static final String BROKER_SID_PATH = "/{brokerSid}";

  @GetMapping(value = BROKER_PATH + BROKER_SID_PATH + POSITION_PATH, produces = "application/json")
  public ResponseEntity<List<PositionResponseDto>> fetchPositionValuation(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID brokerSid) {

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    final List<PositionValuation> positionValuationList = positionService.generatePositionValuationOverview(command);

    final List<PositionResponseDto> response = positionValuationList.stream().map(position ->
        new PositionResponseDto(
            position.assetSid(),
            position.ticker(),
            position.quantity(),
            position.averageCost(),
            position.totalInvested(),
            position.currentPrice(),
            position.marketValue(),
            position.unrealizedPnL(),
            position.unrealizedPnlPercent()
        )
    ).toList();

    return ResponseEntity.ok(response);
  }

}
