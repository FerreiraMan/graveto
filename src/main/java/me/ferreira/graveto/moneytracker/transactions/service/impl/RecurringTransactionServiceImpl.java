package me.ferreira.graveto.moneytracker.transactions.service.impl;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.util.TemporalConfigValidator;
import me.ferreira.graveto.common.web.exception.moneytracker.RecurringTransactionNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchCategoryCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CancelRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

  private static final String RECURRING_TX_CREATE_ACTION = "create recurring transactions";
  private static final String RECURRING_TX_UPDATE_ACTION = "update recurring transactions";
  private static final String RECURRING_TX_CANCEL_ACTION = "cancel recurring transactions";

  private final AccountService accountService;
  private final CategoryService categoryService;
  private final RecurringTransactionRepository recurringTransactionRepository;

  @Override
  @Transactional
  public RecurringTransaction createRecurringTransaction(final CreateRecurringTransactionCommand command) {

    TemporalConfigValidator.validateTemporalInputs(command.startDate(), command.endDate());
    TemporalConfigValidator.validateFrequencyAndDayConfig(command.frequency(), command.dayOfWeek(),
        command.dayOfMonth());

    final Category category =
        categoryService.fetchCategory(new FetchCategoryCommand(command.accountSid(), command.categorySid()));

    validateSameTypeOnCategory(category.getTransactionType(), command.transactionType());

    final Account account = accountService.fetchAccountEntity(command.accountSid());

    account.validateIsActive(RECURRING_TX_CREATE_ACTION);
    account.validateUserPermission(command.userSid(), MembershipRole::canCreateTransaction, RECURRING_TX_CREATE_ACTION);

    final RecurringTransaction recurringTransaction = RecurringTransaction.create(
        account,
        category,
        command.userSid(),
        command.description(),
        command.amount(),
        command.transactionType(),
        command.frequency(),
        command.dayOfMonth(),
        command.dayOfWeek(),
        command.adjustToBusinessDay(),
        command.startDate(),
        command.endDate()
    );

    log.info("Recurring transaction created successfully. Sid: {}", recurringTransaction.getSid());
    return recurringTransactionRepository.save(recurringTransaction);
  }

  @Override
  @Transactional
  public RecurringTransaction updateRecurringTransaction(final UpdateRecurringTransactionCommand command) {

    final RecurringTransaction existingRecurringTransaction =
        recurringTransactionRepository.findBySid(command.sid())
            .orElseThrow(() -> new RecurringTransactionNotFoundException(command.sid()));

    existingRecurringTransaction
        .getAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction, RECURRING_TX_UPDATE_ACTION);

    final boolean isStatusUpdated = existingRecurringTransaction.updateStatus(command.status());
    final boolean isFrequencyUpdated = existingRecurringTransaction.updateFrequency(command.frequency());
    final boolean isScheduleUpdated =
        existingRecurringTransaction.updateSchedule(command.dayOfWeek(), command.dayOfMonth(), command.endDate());

    if (isStatusUpdated || isFrequencyUpdated || isScheduleUpdated || command.nextExecutionDate() != null) {

      TemporalConfigValidator.validateFrequencyAndDayConfig(existingRecurringTransaction.getFrequency(),
          existingRecurringTransaction.getDayOfTheWeek(), existingRecurringTransaction.getDayOfTheMonth());

      existingRecurringTransaction.updateNextExecutionDate(command.nextExecutionDate());
      log.info(
          "Execution date updated - [{}]. Status: [{}], Frequency: [{}], Schedule config: [{}]",
          existingRecurringTransaction.getNextExecutionDate(), isStatusUpdated, isFrequencyUpdated, isScheduleUpdated);
    }

    final String effectiveDescription =
        command.description() != null ? command.description() : existingRecurringTransaction.getDescription();
    final BigDecimal effectiveAmount =
        command.amount() != null ? command.amount() : existingRecurringTransaction.getAmount();
    final Boolean effectiveAdjustToBusinessDay =
        command.adjustToBusinessDay() != null ? command.adjustToBusinessDay() :
            existingRecurringTransaction.getAdjustToBusinessDay();

    existingRecurringTransaction.updateDetails(effectiveDescription, effectiveAmount, effectiveAdjustToBusinessDay);

    return recurringTransactionRepository.save(existingRecurringTransaction);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecurringTransaction> fetchAllRecurringTransactions(final FindAllRecurringTransactionsCommand command) {

    return recurringTransactionRepository.findAll(command);
  }

  @Override
  @Transactional
  public RecurringTransaction cancelRecurringTransaction(final CancelRecurringTransactionCommand command) {

    final RecurringTransaction existingRecurringTransaction =
        recurringTransactionRepository.findBySid(command.sid())
            .orElseThrow(() -> new RecurringTransactionNotFoundException(command.sid()));

    existingRecurringTransaction
        .getAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction, RECURRING_TX_CANCEL_ACTION);

    existingRecurringTransaction.markAsCanceled();
    log.info("Recurring transaction canceled successfully. Sid: {}", existingRecurringTransaction.getSid());
    return recurringTransactionRepository.save(existingRecurringTransaction);
  }

  private void validateSameTypeOnCategory(final TransactionType categoryTransactionType,
                                          final TransactionType transactionType) {

    if (categoryTransactionType != transactionType) {
      throw new IllegalArgumentException(
          String.format("Category type [%s] does not match the requested transaction type [%s].",
              categoryTransactionType.name(), transactionType.name())
      );
    }
  }

}