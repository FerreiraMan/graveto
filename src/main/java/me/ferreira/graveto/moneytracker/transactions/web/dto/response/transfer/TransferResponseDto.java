package me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.TransactionResponseDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponseDto(
    UUID correlationId,
    TransactionResponseDto sourceTransaction,
    TransactionResponseDto destinationTransaction
) {
}
