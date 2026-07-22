package me.ferreira.graveto.moneytracker.transactions.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    if (command.sourceAccountSid().equals(command.destinationAccountSid())) {
      throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
    }


    return null;
  }

}
