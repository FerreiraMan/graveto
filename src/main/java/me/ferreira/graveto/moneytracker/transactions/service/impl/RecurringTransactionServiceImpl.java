package me.ferreira.graveto.moneytracker.transactions.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.domain.Frequency;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

  private static final String RECURRING_TX_CREATE_ACTION = "create recurring transactions";

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

    final LocalDate resolvedStartDate = resolveStartDate(command);

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
        resolvedStartDate,
        resolvedStartDate,
        command.endDate()
    );

    log.info("Recurring transaction created successfully. Sid: {}", recurringTransaction.getSid());
    return recurringTransactionRepository.save(recurringTransaction);
  }

  private void validateTemporalInputs(final CreateRecurringTransactionCommand command) {

    if (command.endDate() != null && command.startDate() != null
        && command.endDate().isBefore(command.startDate())) {

      throw new IllegalArgumentException("End date must be after start date.");
    }

    if (Frequency.MONTHLY.equals(command.frequency()) && command.dayOfMonth() == null) {

      throw new IllegalArgumentException("Day of the month needs to be provided when selecting monthly operation.");
    }

    if ((Frequency.WEEKLY.equals(command.frequency()) || Frequency.BI_WEEKLY.equals(command.frequency()))
        && command.dayOfWeek() == null) {

      throw new IllegalArgumentException(
          "Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
    }

    if ((Frequency.ANNUALLY.equals(command.frequency()))
        && (command.dayOfMonth() == null && command.startDate() == null)) {

      throw new IllegalArgumentException(
          "Day of the month or an explicit start date needs to be provided when selecting annual operation.");
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

  private LocalDate resolveStartDate(final CreateRecurringTransactionCommand command) {

    if (command.startDate() != null) {
      return command.startDate();
    }

    final LocalDate today = LocalDate.now(ZoneId.of("Europe/Lisbon"));

    return switch (command.frequency()) {
      case DAILY -> today.plusDays(1);
      case WEEKLY ->  command.dayOfWeek() <= today.getDayOfWeek().getValue()
            ? today.plusWeeks(1).with(ChronoField.DAY_OF_WEEK, command.dayOfWeek()) :
            today.with(ChronoField.DAY_OF_WEEK, command.dayOfWeek());
      case BI_WEEKLY -> command.dayOfWeek() <= today.getDayOfWeek().getValue()
            ? today.plusWeeks(2).with(ChronoField.DAY_OF_WEEK, command.dayOfWeek()) :
            today.with(ChronoField.DAY_OF_WEEK, command.dayOfWeek());
      case MONTHLY -> {
        final int targetMonth =
            command.dayOfMonth() > today.getDayOfMonth() ? today.getMonthValue() : today.plusMonths(1).getMonthValue();
        final boolean isNextYear = command.dayOfMonth() <= today.getDayOfMonth() && targetMonth == 1;
        final int targetYear = isNextYear ? today.plusYears(1).getYear() : today.getYear();

        final YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        final boolean isValidDayInnMonth = yearMonth.isValidDay(command.dayOfMonth());

        yield LocalDate.of(targetYear, targetMonth,
            isValidDayInnMonth ? command.dayOfMonth() : yearMonth.atEndOfMonth().getDayOfMonth());
      }
      case ANNUALLY ->  command.dayOfMonth() <= today.getDayOfMonth()
            ? today.plusYears(1).withDayOfMonth(command.dayOfMonth()) :
            today.withDayOfMonth(command.dayOfMonth());
    };
  }

}