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
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class RecurringTransferScheduler {

  private final TransferService transferService;
  private final RecurringTransferRepository recurringTransferRepository;
  private final Clock clock;

  @Scheduled(cron = "${scheduled.cron.recurring-transfer}", zone = "Europe/Lisbon")
  public void registerRecurringTransfer() {

    final LocalDate today = LocalDate.now(clock);

    final List<RecurringTransfer> transfersToExecute =
        recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, today);

    if (transfersToExecute.isEmpty()) {
      return;
    }

    final List<RecurringTransfer> processedRecurringTransfers = new ArrayList<>();

    transfersToExecute.forEach(rt -> {
      try {
        final boolean isProcessed = processTransfer(rt, today);
        if (isProcessed) {
          processedRecurringTransfers.add(rt);
        }
      } catch (final Exception e) {
        log.error("Failed to process recurring transfer [{}].", rt.getSid(), e);
      }
    });

    recurringTransferRepository.saveAll(processedRecurringTransfers);
  }

  private boolean processTransfer(final RecurringTransfer rt, final LocalDate today) {

    final boolean isTodayBusinessDay = today.getDayOfWeek().getValue() <= 5;

    if (rt.getAdjustToBusinessDay() && !isTodayBusinessDay) {
      log.info("Recurring transfer [{}] skipped during weekend.", rt.getSid());
      return false;
    }

    final CreateTransferCommand command = new CreateTransferCommand(
        rt.getUserSid(),
        rt.getSourceAccount().getSid(),
        rt.getDestinationAccount().getSid(),
        rt.getAmount(),
        rt.getDescription(),
        LocalDateTime.now()
    );

    transferService.createTransfer(command);
    updateNextOccurrence(rt);
    return true;
  }

  private void updateNextOccurrence(final RecurringTransfer recurringTransfer) {

    switch (recurringTransfer.getFrequency()) {
      case DAILY -> recurringTransfer.scheduleNextExecutionDate(1L, ChronoUnit.DAYS);
      case WEEKLY -> recurringTransfer.scheduleNextExecutionDate(1L, ChronoUnit.WEEKS);
      case BI_WEEKLY -> recurringTransfer.scheduleNextExecutionDate(2L, ChronoUnit.WEEKS);
      case MONTHLY -> recurringTransfer.scheduleNextExecutionDate(1L, ChronoUnit.MONTHS);
      case ANNUALLY -> recurringTransfer.scheduleNextExecutionDate(1L, ChronoUnit.YEARS);
      default -> throw new IllegalStateException("Unhandled frequency: " + recurringTransfer.getFrequency());
    }
  }

}
