package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.CreateRecurringTransactionDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransaction.RecurringTransactionResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

  private static final String RECURRING_TRANSACTION_SID_PATH = "/{sid}";

  private final RecurringTransactionService recurringTransactionService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<RecurringTransactionResponseDto> createRecurringTransaction(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateRecurringTransactionDto requestDto) {

    final CreateRecurringTransactionCommand command = new CreateRecurringTransactionCommand(
        userSid,
        requestDto.accountSid(),
        requestDto.categorySid(),
        StringUtils.trimToNull(requestDto.description()),
        requestDto.amount(),
        requestDto.transactionType(),
        requestDto.frequency(),
        requestDto.dayOfMonth(),
        requestDto.dayOfWeek(),
        requestDto.adjustToBusinessDay(),
        requestDto.startDate(),
        requestDto.endDate()
    );

    final RecurringTransaction createdRecurringTransaction =
        recurringTransactionService.createRecurringTransaction(command);

    final RecurringTransactionResponseDto response = new RecurringTransactionResponseDto(
        createdRecurringTransaction.getSid(),
        new RecurringTransactionResponseDto.EnhancedInfoObject(createdRecurringTransaction.getAccount().getSid(),
            createdRecurringTransaction.getAccount().getInstitution()),
        new RecurringTransactionResponseDto.EnhancedInfoObject(createdRecurringTransaction.getCategory().getSid(),
            createdRecurringTransaction.getCategory().getDisplayName()),
        createdRecurringTransaction.getUserSid(),
        createdRecurringTransaction.getDescription(),
        createdRecurringTransaction.getAmount(),
        createdRecurringTransaction.getCurrency().name(),
        createdRecurringTransaction.getType().name(),
        createdRecurringTransaction.getFrequency().name(),
        createdRecurringTransaction.getNextExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
        createdRecurringTransaction.getStatus().name(),
        createdRecurringTransaction.getEndDate() == null ? null :
            createdRecurringTransaction.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(RECURRING_TRANSACTION_SID_PATH)
        .buildAndExpand(createdRecurringTransaction.getSid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

}