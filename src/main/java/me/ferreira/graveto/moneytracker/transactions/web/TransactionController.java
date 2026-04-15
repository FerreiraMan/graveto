package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.CreateTransactionRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.TransactionFilterRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.UpdateTransactionRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.TransactionResponseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(value = "/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final String TRANSACTION_SID_PATH = "/{sid}";

    private final TransactionService transactionService;

    @PostMapping(produces = "application/json")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody final CreateTransactionRequestDTO requestDTO,
            @RequestHeader("X-User-Sid") final UUID userSid) {

        final CreateTransactionCommand command = new CreateTransactionCommand(
                userSid,
                requestDTO.accountSid(),
                requestDTO.categorySid(),
                requestDTO.amount(),
                StringUtils.trimToNull(requestDTO.description()),
                requestDTO.transactionType(),
                Objects.isNull(requestDTO.occurredAt()) ? LocalDateTime.now() : requestDTO.occurredAt()
        );

        final Transaction createdTransaction = transactionService.createTransaction(command);

        final TransactionResponseDTO response = new TransactionResponseDTO(
                createdTransaction.getSid(),
                createdTransaction.getAmount(),
                createdTransaction.getCategory().getDisplayName(),
                createdTransaction.getDescription(),
                createdTransaction.getType().name(),
                createdTransaction.getStatus().name(),
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
    public ResponseEntity<Page<TransactionResponseDTO>> findAll(
            @Valid @ModelAttribute final TransactionFilterRequestDTO requestDTO,
            @RequestHeader("X-User-Sid") final UUID userSid,
            @PageableDefault(size = 20) @SortDefault(sort = Transaction_.OCCURRED_AT, direction = Sort.Direction.DESC) final Pageable pageable) {

        final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
                userSid,
                requestDTO.accountSid(),
                requestDTO.categorySid(),
                requestDTO.startDate(),
                requestDTO.endDate(),
                requestDTO.type(),
                requestDTO.status(),
                pageable
        );

        final Page<Transaction> transactions = transactionService.findAll(command);

        final Page<TransactionResponseDTO> responseDTO = transactions.map(
                t -> new TransactionResponseDTO(
                    t.getSid(),
                    t.getAmount(),
                    t.getCategory().getDisplayName(),
                    t.getDescription(),
                    t.getType().name(),
                    t.getStatus().name(),
                    t.getOccurredAt()
                ));

        return ResponseEntity.ok().body(responseDTO);
    }

    @DeleteMapping(path = TRANSACTION_SID_PATH, produces = "application/json")
    public ResponseEntity<TransactionResponseDTO> deleteTransaction(
            @RequestHeader("X-User-Sid") final UUID userSid,
            @PathVariable final UUID sid) {

        final DeleteTransactionCommand command = new DeleteTransactionCommand(userSid, sid);

        final Transaction transaction = transactionService.deleteTransaction(command);

        final TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                transaction.getSid(),
                null,
                null,
                null,
                null,
                transaction.getStatus().name(),
                null
        );

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping(path = TRANSACTION_SID_PATH, produces = "application/json")
    public ResponseEntity<TransactionResponseDTO> updateTransaction(
            @RequestHeader("X-User-Sid") final UUID userSid,
            @PathVariable final UUID sid,
            @RequestBody final UpdateTransactionRequestDTO requestDTO) {

        final UpdateTransactionCommand command = new UpdateTransactionCommand(
                userSid,
                sid,
                requestDTO.transactionType(),
                requestDTO.categorySid(),
                requestDTO.amount(),
                StringUtils.trimToNull(requestDTO.description())
        );

        final Transaction transaction = transactionService.updateTransaction(command);

        final TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                transaction.getSid(),
                transaction.getAmount(),
                transaction.getCategory().getDisplayName(),
                transaction.getDescription(),
                transaction.getType().name(),
                null,
                null
        );

        return ResponseEntity.ok().body(responseDTO);
    }

}
