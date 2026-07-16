package me.ferreira.graveto.moneytracker.transactions.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class RecurringTransactionScheduler {

  private final TransactionService transactionService;
  private final RecurringTransactionRepository recurringTransactionRepository;
  private final Clock clock;

  @Scheduled(cron = "${scheduled.cron.recurring-transaction}", zone = "Europe/Lisbon")
  public void registerRecurringTransaction() {

    final LocalDate today = LocalDate.now(clock);

    final List<RecurringTransaction> transactionsToExecute =
        recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, today);

    if (transactionsToExecute.isEmpty()) {
      return;
    }

    final List<RecurringTransaction> processedRecurringTransactions = new ArrayList<>();

    transactionsToExecute.forEach(rt -> {
      try {
        final boolean isProcessed = processTransaction(rt, today);
        if (isProcessed) {
          processedRecurringTransactions.add(rt);
        }
      } catch (final Exception e) {
        log.error("Failed to process recurring transaction [{}].", rt.getSid(), e);
      }
    });

    recurringTransactionRepository.saveAll(processedRecurringTransactions);
  }

  private boolean processTransaction(final RecurringTransaction rt, final LocalDate today) {

    final boolean isTodayBusinessDay = today.getDayOfWeek().getValue() <= 5;

    if (rt.getAdjustToBusinessDay() && !isTodayBusinessDay) {
      log.info("Recurring transaction [{}] skipped during weekend.", rt.getSid());
      return false;
    }

    final CreateTransactionCommand command = new CreateTransactionCommand(
        rt.getUserSid(),
        rt.getAccount().getSid(),
        rt.getCategory().getSid(),
        rt.getAmount(),
        rt.getDescription(),
        rt.getType(),
        LocalDateTime.now()
    );

    transactionService.createTransaction(command);
    updateNextOccurrence(rt);
    return true;
  }

  private void updateNextOccurrence(final RecurringTransaction recurringTransaction) {

    switch (recurringTransaction.getFrequency()) {
      case DAILY -> recurringTransaction.scheduleNextExecutionDate(1L, ChronoUnit.DAYS);
      case WEEKLY -> recurringTransaction.scheduleNextExecutionDate(1L, ChronoUnit.WEEKS);
      case BI_WEEKLY -> recurringTransaction.scheduleNextExecutionDate(2L, ChronoUnit.WEEKS);
      case MONTHLY -> recurringTransaction.scheduleNextExecutionDate(1L, ChronoUnit.MONTHS);
      case ANNUALLY -> recurringTransaction.scheduleNextExecutionDate(1L, ChronoUnit.YEARS);
      default -> throw new IllegalStateException("Unhandled frequency: " + recurringTransaction.getFrequency());
    }
  }

}
