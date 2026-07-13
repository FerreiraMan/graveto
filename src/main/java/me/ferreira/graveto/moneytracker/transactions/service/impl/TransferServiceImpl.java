package me.ferreira.graveto.moneytracker.transactions.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TransferServiceImpl implements TransferService {

  private static final String TR_CREATE_ACTION = "create transfer transactions";
  private static final String TR_DELETE_ACTION = "delete transfer transactions";
  private static final String TR_UPDATE_ACTION = "update transfer transactions";

  private final AccountService accountService;
  private final CategoryService categoryService;
  private final TransactionRepository transactionRepository;

  @Override
  @Transactional(readOnly = true)
  public TransferResult fetchTransfer(final FetchTransferCommand command) {

    final List<Transaction> transferTransactions =
        transactionRepository.findAllByCorrelationId(command.correlationId());

    validateTransferIntegrity(transferTransactions);

    final Transaction out =
        transferTransactions.get(0).getType() == TransactionType.TRANSFER_OUT ? transferTransactions.get(0) :
            transferTransactions.get(1);
    final Transaction in =
        transferTransactions.get(0).getType() == TransactionType.TRANSFER_IN ? transferTransactions.get(0) :
            transferTransactions.get(1);

    out.getAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canReadTransaction, "read transfer transactions");

    return new TransferResult(out, in);
  }

  @Override
  @Transactional
  public TransferResult createTransfer(final CreateTransferCommand command) {

    if (command.sourceAccountSid().equals(command.destinationAccountSid())) {
      throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
    }

    final UUID userSid = command.userSid();
    final BigDecimal amount = command.amount();

    final Account sourceAccount = accountService.fetchAccountEntity(command.sourceAccountSid());
    final Account destinationAccount = accountService.fetchAccountEntity(command.destinationAccountSid());

    sourceAccount.validateIsActive(TR_CREATE_ACTION);
    destinationAccount.validateIsActive(TR_CREATE_ACTION);

    sourceAccount.validateUserPermission(userSid, MembershipRole::canCreateTransaction, TR_CREATE_ACTION);
    destinationAccount.validateUserPermission(userSid, MembershipRole::canCreateTransaction, TR_CREATE_ACTION);

    sourceAccount.updateBalance(amount, TransactionType.TRANSFER_OUT);
    destinationAccount.updateBalance(amount, TransactionType.TRANSFER_IN);

    final UUID correlationId = UUID.randomUUID();
    final Category transferOutCategory = categoryService.fetchInternalCategory(SystemCategory.TRANSFER_OUT.getSid());
    final Category transferInCategory = categoryService.fetchInternalCategory(SystemCategory.TRANSFER_IN.getSid());

    final Transaction out = Transaction.createTransferTransaction(
        sourceAccount,
        amount,
        command.description(),
        correlationId,
        transferOutCategory,
        TransactionType.TRANSFER_OUT,
        command.occurredAt()
    );

    final Transaction in = Transaction.createTransferTransaction(
        destinationAccount,
        amount,
        command.description(),
        correlationId,
        transferInCategory,
        TransactionType.TRANSFER_IN,
        command.occurredAt()
    );

    transactionRepository.saveAll(List.of(out, in));

    log.info("Transfer created successfully. CorrelationId: {}", correlationId);

    return new TransferResult(out, in);
  }

  @Override
  @Transactional
  public TransferResult deleteTransfer(final DeleteTransferCommand command) {

    final List<Transaction> transferTransactions =
        transactionRepository.findAllByCorrelationId(command.correlationId());

    validateTransferIntegrity(transferTransactions);

    final Transaction out =
        transferTransactions.get(0).getType() == TransactionType.TRANSFER_OUT ? transferTransactions.get(0) :
            transferTransactions.get(1);
    final Transaction in =
        transferTransactions.get(0).getType() == TransactionType.TRANSFER_IN ? transferTransactions.get(0) :
            transferTransactions.get(1);

    out.getAccount().validateIsActive(TR_DELETE_ACTION);
    in.getAccount().validateIsActive(TR_DELETE_ACTION);

    out.getAccount().validateUserPermission(command.userSid(), MembershipRole::canDeleteTransaction, TR_DELETE_ACTION);
    in.getAccount().validateUserPermission(command.userSid(), MembershipRole::canDeleteTransaction, TR_DELETE_ACTION);

    out.markAsDeleted();
    out.getAccount().reverseBalanceImpact(out.getAmount(), out.getType());

    in.markAsDeleted();
    in.getAccount().reverseBalanceImpact(in.getAmount(), in.getType());

    log.info("Transfer deleted successfully. CorrelationId: {}", command.correlationId());

    return new TransferResult(out, in);
  }

  @Override
  @Transactional
  public TransferResult updateTransfer(final UpdateTransferCommand command) {

    final List<Transaction> transferTransactions =
        transactionRepository.findAllByCorrelationId(command.correlationId());

    validateTransferIntegrity(transferTransactions);

    final Transaction out =
        transferTransactions.get(0).getType() == TransactionType.TRANSFER_OUT ? transferTransactions.get(0) :
            transferTransactions.get(1);
    final Transaction in =
        transferTransactions.get(0).getType() == TransactionType.TRANSFER_IN ? transferTransactions.get(0) :
            transferTransactions.get(1);

    out.getAccount().validateIsActive(TR_UPDATE_ACTION);
    in.getAccount().validateIsActive(TR_UPDATE_ACTION);

    out.getAccount().validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction, TR_UPDATE_ACTION);
    in.getAccount().validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction, TR_UPDATE_ACTION);

    final BigDecimal effectiveAmount = command.amount() != null ? command.amount() : out.getAmount();
    final String effectiveDescription = command.description() != null ? command.description() : out.getDescription();
    final LocalDateTime effectiveOccurredAt = command.occurredAt() != null ? command.occurredAt() : out.getOccurredAt();

    final boolean requiresBalanceCalculation = out.getAmount().compareTo(effectiveAmount) != 0;

    if (requiresBalanceCalculation) {
      out.getAccount().reverseBalanceImpact(out.getAmount(), out.getType());
      in.getAccount().reverseBalanceImpact(in.getAmount(), in.getType());
    }

    out.updateDetails(effectiveAmount, out.getCategory(), effectiveDescription, out.getType(), effectiveOccurredAt);
    in.updateDetails(effectiveAmount, in.getCategory(), effectiveDescription, in.getType(), effectiveOccurredAt);

    if (requiresBalanceCalculation) {
      out.getAccount().updateBalance(out.getAmount(), out.getType());
      in.getAccount().updateBalance(in.getAmount(), in.getType());
    }

    log.info("Transfer updated successfully. CorrelationId: {}", command.correlationId());

    return new TransferResult(out, in);
  }

  private void validateTransferIntegrity(final List<Transaction> transactions) {

    if (transactions.size() != 2) {
      throw new IllegalStateException("Transfer is associated with an incorrect amount of transactions.");
    }

    final Transaction first = transactions.get(0);
    final Transaction second = transactions.get(1);

    final boolean hasCorrectTypes =
        (first.getType() == TransactionType.TRANSFER_OUT && second.getType() == TransactionType.TRANSFER_IN)
            || (first.getType() == TransactionType.TRANSFER_IN && second.getType() == TransactionType.TRANSFER_OUT);

    if (!hasCorrectTypes) {
      throw new IllegalStateException("Corrupted transfer does not contain exactly one IN and one OUT transaction.");
    }
  }

}