package me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponseDto(
    UUID sourceAccountSid,
    UUID destinationAccountSid,
    BigDecimal amount,
    UUID correlationId,
    TransactionStatus transferStatus
) {
}
