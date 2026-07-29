package me.ferreira.graveto.moneytracker.transactions.service.impl;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.util.TemporalConfigValidator;
import me.ferreira.graveto.common.web.exception.moneytracker.RecurringTransferNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CancelRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.FindAllRecurringTransfersCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.UpdateRecurringTransferCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class RecurringTransferServiceImpl implements RecurringTransferService {

  private static final String RECURRING_TR_CREATE_ACTION = "create recurring transfers";
  private static final String RECURRING_TR_UPDATE_ACTION = "update recurring transfers";
  private static final String RECURRING_TR_CANCEL_ACTION = "cancel recurring transfers";

  private final AccountService accountService;
  private final RecurringTransferRepository recurringTransferRepository;

  @Override
  @Transactional
  public RecurringTransfer createRecurringTransfer(final CreateRecurringTransferCommand command) {

    TemporalConfigValidator.validateTemporalInputs(command.startDate(), command.endDate());
    TemporalConfigValidator.validateFrequencyAndDayConfig(command.frequency(), command.dayOfWeek(),
        command.dayOfMonth());

    if (command.sourceAccountSid().equals(command.destinationAccountSid())) {
      throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
    }

    final Account sourceAccount = accountService.fetchAccountEntity(command.sourceAccountSid());
    final Account destinationAccount = accountService.fetchAccountEntity(command.destinationAccountSid());

    sourceAccount.validateIsActive(RECURRING_TR_CREATE_ACTION);
    destinationAccount.validateIsActive(RECURRING_TR_CREATE_ACTION);
    sourceAccount.validateUserPermission(command.userSid(), MembershipRole::canCreateTransaction,
        RECURRING_TR_CREATE_ACTION);
    destinationAccount.validateUserPermission(command.userSid(), MembershipRole::canCreateTransaction,
        RECURRING_TR_CREATE_ACTION);

    final RecurringTransfer recurringTransfer = RecurringTransfer.create(
        sourceAccount,
        destinationAccount,
        command.userSid(),
        command.description(),
        command.amount(),
        command.frequency(),
        command.dayOfMonth(),
        command.dayOfWeek(),
        command.adjustToBusinessDay(),
        command.startDate(),
        command.endDate()
    );

    log.info("Recurring transfer created successfully. Sid: {}", recurringTransfer.getSid());
    return recurringTransferRepository.save(recurringTransfer);
  }

  @Override
  @Transactional
  public RecurringTransfer updateRecurringTransfer(final UpdateRecurringTransferCommand command) {

    final RecurringTransfer existingRecurringTransfer =
        recurringTransferRepository.findBySid(command.sid())
            .orElseThrow(() -> new RecurringTransferNotFoundException(command.sid()));

    existingRecurringTransfer.getSourceAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction,
            RECURRING_TR_UPDATE_ACTION);
    existingRecurringTransfer.getDestinationAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction,
            RECURRING_TR_UPDATE_ACTION);

    final boolean isStatusUpdated = existingRecurringTransfer.updateStatus(command.status());
    final boolean isFrequencyUpdated = existingRecurringTransfer.updateFrequency(command.frequency());
    final boolean isScheduleUpdated =
        existingRecurringTransfer.updateSchedule(command.dayOfWeek(), command.dayOfMonth(), command.endDate());

    if (isStatusUpdated || isFrequencyUpdated || isScheduleUpdated || command.nextExecutionDate() != null) {

      TemporalConfigValidator.validateFrequencyAndDayConfig(existingRecurringTransfer.getFrequency(),
          existingRecurringTransfer.getDayOfTheWeek(), existingRecurringTransfer.getDayOfTheMonth());

      existingRecurringTransfer.updateNextExecutionDate(command.nextExecutionDate());
      log.info(
          "Execution date updated - [{}]. Status: [{}], Frequency: [{}], Schedule config: [{}]",
          existingRecurringTransfer.getNextExecutionDate(), isStatusUpdated, isFrequencyUpdated, isScheduleUpdated);
    }

    final String effectiveDescription =
        command.description() != null ? command.description() : existingRecurringTransfer.getDescription();
    final BigDecimal effectiveAmount =
        command.amount() != null ? command.amount() : existingRecurringTransfer.getAmount();
    final Boolean effectiveAdjustToBusinessDay =
        command.adjustToBusinessDay() != null ? command.adjustToBusinessDay() :
            existingRecurringTransfer.getAdjustToBusinessDay();

    existingRecurringTransfer.updateDetails(effectiveDescription, effectiveAmount, effectiveAdjustToBusinessDay);

    return recurringTransferRepository.save(existingRecurringTransfer);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecurringTransfer> fetchAllRecurringTransfers(final FindAllRecurringTransfersCommand command) {

    return recurringTransferRepository.findAll(command);
  }

  @Override
  @Transactional
  public RecurringTransfer cancelRecurringTransfer(final CancelRecurringTransferCommand command) {

    final RecurringTransfer existingRecurringTransfer =
        recurringTransferRepository.findBySid(command.sid())
            .orElseThrow(() -> new RecurringTransferNotFoundException(command.sid()));

    existingRecurringTransfer.getSourceAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction,
            RECURRING_TR_CANCEL_ACTION);
    existingRecurringTransfer.getDestinationAccount()
        .validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction,
            RECURRING_TR_CANCEL_ACTION);

    existingRecurringTransfer.markAsCanceled();
    log.info("Recurring transfer canceled successfully. Sid: {}", existingRecurringTransfer.getSid());
    return recurringTransferRepository.save(existingRecurringTransfer);
  }

}
