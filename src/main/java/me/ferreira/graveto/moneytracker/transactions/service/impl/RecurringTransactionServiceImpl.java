package me.ferreira.graveto.moneytracker.transactions.service.impl;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.domain.Frequency;
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
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

  private static final String RECURRING_TX_CREATE_ACTION = "create recurring transactions";
  private static final String RECURRING_TX_UPDATE_ACTION = "update recurring transactions";

  private final AccountService accountService;
  private final CategoryService categoryService;
  private final RecurringTransactionRepository recurringTransactionRepository;

  @Override
  @Transactional
  public RecurringTransaction createRecurringTransaction(final CreateRecurringTransactionCommand command) {

    validateTemporalInputs(command);

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

    final Account account = accountService.fetchAccountEntity(command.accountSid());
    account.validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction, RECURRING_TX_UPDATE_ACTION);

    final RecurringTransaction existingRecurringTransaction =
        recurringTransactionRepository.findBySidAndBelongsToAccount(command.sid(), account.getSid())
            .orElseThrow(() -> new RecurringTransactionNotFoundException(command.sid()));

    final boolean isStatusUpdated = existingRecurringTransaction.updateStatus(command.status());
    final boolean isFrequencyUpdated = existingRecurringTransaction.updateFrequency(command.frequency());
    final boolean isScheduleUpdated =
        existingRecurringTransaction.updateSchedule(command.dayOfWeek(), command.dayOfMonth(), command.endDate());

    if (isStatusUpdated || isFrequencyUpdated || isScheduleUpdated || command.nextExecutionDate() != null) {

      validateFrequencyAndDayConfig(existingRecurringTransaction.getFrequency(),
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

  private void validateTemporalInputs(final CreateRecurringTransactionCommand command) {

    if (command.endDate() != null && command.startDate() != null
        && command.endDate().isBefore(command.startDate())) {

      throw new IllegalArgumentException("End date must be after start date.");
    }

    validateFrequencyAndDayConfig(command.frequency(), command.dayOfWeek(), command.dayOfMonth());
  }

  private void validateFrequencyAndDayConfig(final Frequency frequency, final Integer dayOfWeek,
                                             final Integer dayOfMonth) {

    if (Frequency.MONTHLY.equals(frequency) && dayOfMonth == null) {
      throw new IllegalArgumentException("Day of the month needs to be provided when selecting monthly operation.");
    }
    if ((Frequency.WEEKLY.equals(frequency) || Frequency.BI_WEEKLY.equals(frequency)) && dayOfWeek == null) {
      throw new IllegalArgumentException(
          "Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
    }
    if (Frequency.ANNUALLY.equals(frequency) && dayOfMonth == null) {
      throw new IllegalArgumentException("Day of the month needs to be provided when selecting annual operation.");
    }
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