package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.CreateTransferRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer.TransferResponseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(value = "/transfers")
@RequiredArgsConstructor
public class TransferController {

    private static final String TRANSFER_SID_PATH = "/{correlationId}";

    private final TransferService transferService;

    @PostMapping(produces = "application/json")
    public ResponseEntity<TransferResponseDTO> createTransfer(
            @Valid @RequestBody final CreateTransferRequestDTO requestDTO,
            @RequestHeader("X-User-Sid") final UUID userSid) {

        final CreateTransferCommand command = new CreateTransferCommand(
                userSid,
                requestDTO.sourceAccountSid(),
                requestDTO.destinationAccountSid(),
                requestDTO.amount(),
                StringUtils.trimToNull(requestDTO.description()),
                Objects.isNull(requestDTO.occurredAt()) ? LocalDateTime.now() : requestDTO.occurredAt()
        );

        final TransferResult createdTransfer = transferService.createTransfer(command);

        final TransferResponseDTO response = new TransferResponseDTO(
                createdTransfer.expense().getAccount().getSid(),
                createdTransfer.income().getAccount().getSid(),
                createdTransfer.expense().getAmount(),
                createdTransfer.expense().getCorrelationId(),
                null
        );

        final URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(TRANSFER_SID_PATH)
                .buildAndExpand(createdTransfer.expense().getCorrelationId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping(path = TRANSFER_SID_PATH, produces = "application/json")
    public ResponseEntity<TransferResponseDTO> deleteTransfer(
            @RequestHeader("X-User-Sid") final UUID userSid,
            @PathVariable final UUID correlationId) {

        final DeleteTransferCommand command = new DeleteTransferCommand(userSid, correlationId);

        final TransferResult deletedTransfer = transferService.deleteTransfer(command);

        final TransferResponseDTO responseDTO = new TransferResponseDTO(
                deletedTransfer.expense().getAccount().getSid(),
                deletedTransfer.income().getAccount().getSid(),
                deletedTransfer.expense().getAmount(),
                deletedTransfer.expense().getCorrelationId(),
                TransactionStatus.DELETED
        );

        return ResponseEntity.ok().body(responseDTO);
    }

}
