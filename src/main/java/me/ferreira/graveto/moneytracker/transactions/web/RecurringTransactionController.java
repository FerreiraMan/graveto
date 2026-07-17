package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.CreateRecurringTransactionRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.RecurringTransactionFilterRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.UpdateRecurringTransactionRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransaction.RecurringTransactionResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping(value = "/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

  private static final String RECURRING_TRANSACTION_SID_PATH = "/{sid}";

  private final RecurringTransactionService recurringTransactionService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<RecurringTransactionResponseDto> createRecurringTransaction(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateRecurringTransactionRequestDto requestDto) {

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

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(RECURRING_TRANSACTION_SID_PATH)
        .buildAndExpand(createdRecurringTransaction.getSid())
        .toUri();

    return ResponseEntity.created(location).body(buildResponse(createdRecurringTransaction));
  }

  @PatchMapping(path = RECURRING_TRANSACTION_SID_PATH, produces = "application/json")
  public ResponseEntity<RecurringTransactionResponseDto> updateRecurringTransaction(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid,
      @Valid @RequestBody final UpdateRecurringTransactionRequestDto requestDto) {

    final UpdateRecurringTransactionCommand command = new UpdateRecurringTransactionCommand(
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

    final RecurringTransaction updateRecurringTransaction =
        recurringTransactionService.updateRecurringTransaction(command);

    return ResponseEntity.ok(buildResponse(updateRecurringTransaction));
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<RecurringTransactionResponseDto>> findAll(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @ModelAttribute final RecurringTransactionFilterRequestDto requestDto) {

    final FindAllRecurringTransactionsCommand command = new FindAllRecurringTransactionsCommand(
        userSid,
        requestDto.status(),
        requestDto.accountSid()
    );

    final List<RecurringTransaction> recurringTransactions =
        recurringTransactionService.fetchAllRecurringTransactions(command);

    return ResponseEntity.ok(
        recurringTransactions.stream().map(this::buildResponse).toList()
    );
  }

  private RecurringTransactionResponseDto buildResponse(final RecurringTransaction recurringTransaction) {

    return new RecurringTransactionResponseDto(
        recurringTransaction.getSid(),
        new RecurringTransactionResponseDto.EnhancedInfoObject(recurringTransaction.getAccount().getSid(),
            recurringTransaction.getAccount().getInstitution()),
        new RecurringTransactionResponseDto.EnhancedInfoObject(recurringTransaction.getCategory().getSid(),
            recurringTransaction.getCategory().getDisplayName()),
        recurringTransaction.getUserSid(),
        recurringTransaction.getDescription(),
        recurringTransaction.getAmount(),
        recurringTransaction.getCurrency().name(),
        recurringTransaction.getType().name(),
        recurringTransaction.getFrequency().name(),
        recurringTransaction.getNextExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
        recurringTransaction.getStatus().name(),
        recurringTransaction.getEndDate() == null ? null :
            recurringTransaction.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    );
  }

}