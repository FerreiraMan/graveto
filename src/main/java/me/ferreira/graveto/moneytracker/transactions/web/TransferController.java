package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.CreateTransferRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.UpdateTransferRequestDTO;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer.TransferResponseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping(path = TRANSFER_SID_PATH, produces = "application/json")
    public ResponseEntity<TransferResponseDTO> fetchTransfer(
        @AuthenticationPrincipal final UUID userSid,
        @PathVariable final UUID correlationId) {

        final FetchTransferCommand command = new FetchTransferCommand(
                userSid,
                correlationId
        );

        final TransferResult transfer = transferService.fetchTransfer(command);

        final TransferResponseDTO response = new TransferResponseDTO(
                transfer.expense().getAccount().getSid(),
                transfer.income().getAccount().getSid(),
                transfer.expense().getAmount(),
                transfer.expense().getCorrelationId(),
                transfer.expense().getStatus()
        );

        return ResponseEntity.ok().body(response);
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<TransferResponseDTO> createTransfer(
        @Valid @RequestBody final CreateTransferRequestDTO requestDTO,
        @AuthenticationPrincipal final UUID userSid) {

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
        @AuthenticationPrincipal final UUID userSid,
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

    @PatchMapping(path = TRANSFER_SID_PATH, produces = "application/json")
    public ResponseEntity<TransferResponseDTO> updateTransfer(
        @AuthenticationPrincipal final UUID userSid,
        @PathVariable final UUID correlationId,
        @Valid @RequestBody final UpdateTransferRequestDTO requestDTO) {

        final UpdateTransferCommand command = new UpdateTransferCommand(
                userSid,
                correlationId,
                requestDTO.amount(),
                StringUtils.trimToNull(requestDTO.description()),
                requestDTO.occurredAt()
        );

        final TransferResult updatedTransfer = transferService.updateTransfer(command);

        final TransferResponseDTO responseDTO = new TransferResponseDTO(
                updatedTransfer.expense().getAccount().getSid(),
                updatedTransfer.income().getAccount().getSid(),
                updatedTransfer.expense().getAmount(),
                updatedTransfer.expense().getCorrelationId(),
                null
        );

        return ResponseEntity.ok().body(responseDTO);
    }

}
