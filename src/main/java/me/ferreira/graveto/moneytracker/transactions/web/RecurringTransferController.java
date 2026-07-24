package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.CreateRecurringTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransfer.RecurringTransferResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/recurring-transfers")
@RequiredArgsConstructor
public class RecurringTransferController {

  private static final String RECURRING_TRANSFER_SID_PATH = "/{sid}";

  private final RecurringTransferService recurringTransferService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<RecurringTransferResponseDto> createRecurringTransfer(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateRecurringTransferRequestDto requestDto) {

    final CreateRecurringTransferCommand command = new CreateRecurringTransferCommand(
        userSid,
        requestDto.sourceAccountSid(),
        requestDto.destinationAccountSid(),
        StringUtils.trimToNull(requestDto.description()),
        requestDto.amount(),
        requestDto.frequency(),
        requestDto.dayOfMonth(),
        requestDto.dayOfWeek(),
        requestDto.adjustToBusinessDay(),
        requestDto.startDate(),
        requestDto.endDate()
    );

    final RecurringTransfer createdRecurringTransfer =
        recurringTransferService.createRecurringTransfer(command);

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(RECURRING_TRANSFER_SID_PATH)
        .buildAndExpand(createdRecurringTransfer.getSid())
        .toUri();

    return ResponseEntity.created(location).body(buildResponse(createdRecurringTransfer));
  }

  private RecurringTransferResponseDto buildResponse(final RecurringTransfer recurringTransfer) {

    return new RecurringTransferResponseDto(
        recurringTransfer.getSid(),
        new RecurringTransferResponseDto.EnhancedInfoObject(recurringTransfer.getSourceAccount().getSid(),
            recurringTransfer.getSourceAccount().getInstitution()),
        new RecurringTransferResponseDto.EnhancedInfoObject(recurringTransfer.getDestinationAccount().getSid(),
            recurringTransfer.getDestinationAccount().getInstitution()),
        recurringTransfer.getUserSid(),
        recurringTransfer.getDescription(),
        recurringTransfer.getAmount(),
        recurringTransfer.getCurrency().name(),
        recurringTransfer.getFrequency().name(),
        recurringTransfer.getNextExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
        recurringTransfer.getStatus().name(),
        recurringTransfer.getEndDate() == null ? null :
            recurringTransfer.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    );
  }

}
