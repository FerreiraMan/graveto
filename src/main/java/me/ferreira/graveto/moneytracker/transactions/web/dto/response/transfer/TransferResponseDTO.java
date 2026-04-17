package me.ferreira.graveto.moneytracker.transactions.web.dto.response.transfer;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponseDTO(
    UUID sourceAccountSid,
    UUID destinationAccountSid,
    BigDecimal amount,
    UUID correlationId
) {}
