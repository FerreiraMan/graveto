package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.CreateTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.UpdateTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer.TransferResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/transfers")
@RequiredArgsConstructor
public class TransferController {

  private static final String TRANSFER_SID_PATH = "/{correlationId}";

  private final TransferService transferService;

  @GetMapping(path = TRANSFER_SID_PATH, produces = "application/json")
  public ResponseEntity<TransferResponseDto> fetchTransfer(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID correlationId) {

    final FetchTransferCommand command = new FetchTransferCommand(
        userSid,
        correlationId
    );

    final TransferResult transfer = transferService.fetchTransfer(command);

    final TransferResponseDto response = new TransferResponseDto(
        transfer.expense().getAccount().getSid(),
        transfer.income().getAccount().getSid(),
        transfer.expense().getAmount(),
        transfer.expense().getCorrelationId(),
        transfer.expense().getStatus()
    );

    return ResponseEntity.ok().body(response);
  }

  @PostMapping(produces = "application/json")
  public ResponseEntity<TransferResponseDto> createTransfer(
      @Valid @RequestBody final CreateTransferRequestDto requestDto,
      @AuthenticationPrincipal final UUID userSid) {

    final CreateTransferCommand command = new CreateTransferCommand(
        userSid,
        requestDto.sourceAccountSid(),
        requestDto.destinationAccountSid(),
        requestDto.amount(),
        StringUtils.trimToNull(requestDto.description()),
        Objects.isNull(requestDto.occurredAt()) ? LocalDateTime.now() : requestDto.occurredAt()
    );

    final TransferResult createdTransfer = transferService.createTransfer(command);

    final TransferResponseDto response = new TransferResponseDto(
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
  public ResponseEntity<TransferResponseDto> deleteTransfer(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID correlationId) {

    final DeleteTransferCommand command = new DeleteTransferCommand(userSid, correlationId);

    final TransferResult deletedTransfer = transferService.deleteTransfer(command);

    final TransferResponseDto responseDto = new TransferResponseDto(
        deletedTransfer.expense().getAccount().getSid(),
        deletedTransfer.income().getAccount().getSid(),
        deletedTransfer.expense().getAmount(),
        deletedTransfer.expense().getCorrelationId(),
        TransactionStatus.DELETED
    );

    return ResponseEntity.ok().body(responseDto);
  }

  @PatchMapping(path = TRANSFER_SID_PATH, produces = "application/json")
  public ResponseEntity<TransferResponseDto> updateTransfer(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID correlationId,
      @Valid @RequestBody final UpdateTransferRequestDto requestDto) {

    final UpdateTransferCommand command = new UpdateTransferCommand(
        userSid,
        correlationId,
        requestDto.amount(),
        StringUtils.trimToNull(requestDto.description()),
        requestDto.occurredAt()
    );

    final TransferResult updatedTransfer = transferService.updateTransfer(command);

    final TransferResponseDto responseDto = new TransferResponseDto(
        updatedTransfer.expense().getAccount().getSid(),
        updatedTransfer.income().getAccount().getSid(),
        updatedTransfer.expense().getAmount(),
        updatedTransfer.expense().getCorrelationId(),
        null
    );

    return ResponseEntity.ok().body(responseDto);
  }

}
