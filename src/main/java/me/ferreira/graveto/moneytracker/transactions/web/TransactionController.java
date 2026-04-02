package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.CreateTransactionRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.TransactionResponseDTO;
import org.apache.commons.lang3.StringUtils;
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
                createdTransaction.getStatus().name()
        );

        final URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(TRANSACTION_SID_PATH)
                .buildAndExpand(createdTransaction.getSid())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

}
