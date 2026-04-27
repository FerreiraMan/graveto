package me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponseDTO(
    UUID sourceAccountSid,
    UUID destinationAccountSid,
    BigDecimal amount,
    UUID correlationId,
    TransactionStatus transferStatus
) {}
