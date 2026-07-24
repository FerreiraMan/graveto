package me.ferreira.graveto.moneytracker.transactions.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.util.TemporalConfigValidator;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class RecurringTransferServiceImpl implements RecurringTransferService {

  private static final String RECURRING_TR_CREATE_ACTION = "create recurring transfers";

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

}
