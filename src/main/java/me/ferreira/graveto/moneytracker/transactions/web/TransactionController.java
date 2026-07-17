package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.CreateTransactionRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.TransactionFilterRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.UpdateTransactionRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.TransactionResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
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
@RequestMapping(value = "/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private static final String TRANSACTION_SID_PATH = "/{sid}";

  private final TransactionService transactionService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<TransactionResponseDto> createTransaction(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateTransactionRequestDto requestDto) {

    final CreateTransactionCommand command = new CreateTransactionCommand(
        userSid,
        requestDto.accountSid(),
        requestDto.categorySid(),
        requestDto.amount(),
        StringUtils.trimToNull(requestDto.description()),
        requestDto.transactionType(),
        Objects.isNull(requestDto.occurredAt()) ? LocalDateTime.now() : requestDto.occurredAt()
    );

    final Transaction createdTransaction = transactionService.createTransaction(command);

    final TransactionResponseDto response = new TransactionResponseDto(
        createdTransaction.getSid(),
        createdTransaction.getAmount(),
        createdTransaction.getCategory().getDisplayName(),
        createdTransaction.getDescription(),
        createdTransaction.getType().name(),
        createdTransaction.getStatus().name(),
        createdTransaction.getCorrelationId(),
        createdTransaction.getOccurredAt()
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(TRANSACTION_SID_PATH)
        .buildAndExpand(createdTransaction.getSid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<Page<TransactionResponseDto>> findAll(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @ModelAttribute final TransactionFilterRequestDto requestDto,
      @PageableDefault(size = 20) @SortDefault(sort = Transaction_.OCCURRED_AT, direction = Sort.Direction.DESC)
      final Pageable pageable) {

    final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
        userSid,
        requestDto.accountSid(),
        requestDto.categorySid(),
        requestDto.startDate(),
        requestDto.endDate(),
        requestDto.type(),
        requestDto.status(),
        pageable
    );

    final Page<Transaction> transactions = transactionService.findAll(command);

    final Page<TransactionResponseDto> responseDto = transactions.map(
        t -> new TransactionResponseDto(
            t.getSid(),
            t.getAmount(),
            t.getCategory().getDisplayName(),
            t.getDescription(),
            t.getType().name(),
            t.getStatus().name(),
            t.getCorrelationId(),
            t.getOccurredAt()
        ));

    return ResponseEntity.ok().body(responseDto);
  }

  @DeleteMapping(path = TRANSACTION_SID_PATH, produces = "application/json")
  public ResponseEntity<TransactionResponseDto> deleteTransaction(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final DeleteTransactionCommand command = new DeleteTransactionCommand(userSid, sid);

    final Transaction transaction = transactionService.deleteTransaction(command);

    final TransactionResponseDto responseDto = new TransactionResponseDto(
        transaction.getSid(),
        null,
        null,
        null,
        null,
        transaction.getStatus().name(),
        transaction.getCorrelationId(),
        null
    );

    return ResponseEntity.ok().body(responseDto);
  }

  @PatchMapping(path = TRANSACTION_SID_PATH, produces = "application/json")
  public ResponseEntity<TransactionResponseDto> updateTransaction(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid,
      @Valid @RequestBody final UpdateTransactionRequestDto requestDto) {

    final UpdateTransactionCommand command = new UpdateTransactionCommand(
        userSid,
        sid,
        requestDto.transactionType(),
        requestDto.categorySid(),
        requestDto.amount(),
        StringUtils.trimToNull(requestDto.description()),
        requestDto.occurredAt()
    );

    final Transaction transaction = transactionService.updateTransaction(command);

    final TransactionResponseDto responseDto = new TransactionResponseDto(
        transaction.getSid(),
        transaction.getAmount(),
        transaction.getCategory().getDisplayName(),
        transaction.getDescription(),
        transaction.getType().name(),
        null,
        transaction.getCorrelationId(),
        transaction.getOccurredAt()
    );

    return ResponseEntity.ok().body(responseDto);
  }

}
