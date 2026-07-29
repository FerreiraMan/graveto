package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CancelRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.FindAllRecurringTransfersCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.UpdateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.CreateRecurringTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.RecurringTransferFilterRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.UpdateRecurringTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransfer.RecurringTransferResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @PatchMapping(path = RECURRING_TRANSFER_SID_PATH, produces = "application/json")
  public ResponseEntity<RecurringTransferResponseDto> updateRecurringTransfer(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid,
      @Valid @RequestBody final UpdateRecurringTransferRequestDto requestDto) {

    final UpdateRecurringTransferCommand command = new UpdateRecurringTransferCommand(
        userSid,
        sid,
        StringUtils.trimToNull(requestDto.description()),
        requestDto.amount(),
        requestDto.frequency(),
        requestDto.dayOfMonth(),
        requestDto.dayOfWeek(),
        requestDto.adjustToBusinessDay(),
        requestDto.status(),
        requestDto.nextExecutionDate(),
        requestDto.endDate()
    );

    final RecurringTransfer updateRecurringTransfer =
        recurringTransferService.updateRecurringTransfer(command);

    return ResponseEntity.ok(buildResponse(updateRecurringTransfer));
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<RecurringTransferResponseDto>> findAll(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @ModelAttribute final RecurringTransferFilterRequestDto requestDto) {

    final FindAllRecurringTransfersCommand command = new FindAllRecurringTransfersCommand(
        userSid,
        requestDto.status(),
        requestDto.sourceAccountSid(),
        requestDto.destinationAccountSid()
    );

    final List<RecurringTransfer> recurringTransfers =
        recurringTransferService.fetchAllRecurringTransfers(command);

    return ResponseEntity.ok(
        recurringTransfers.stream().map(this::buildResponse).toList()
    );
  }

  @DeleteMapping(path = RECURRING_TRANSFER_SID_PATH, produces = "application/json")
  public ResponseEntity<RecurringTransferResponseDto> cancelRecurringTransfer(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final CancelRecurringTransferCommand command = new CancelRecurringTransferCommand(userSid, sid);

    final RecurringTransfer canceledRecurringTransfer =
        recurringTransferService.cancelRecurringTransfer(command);

    return ResponseEntity.ok(buildResponse(canceledRecurringTransfer));
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
