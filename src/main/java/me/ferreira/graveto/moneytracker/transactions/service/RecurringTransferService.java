package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;

public interface RecurringTransferService {

  RecurringTransfer createRecurringTransfer(CreateRecurringTransferCommand command);

}
