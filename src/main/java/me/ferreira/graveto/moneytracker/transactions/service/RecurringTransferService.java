package me.ferreira.graveto.moneytracker.transactions.service;

import java.util.List;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.recurringtransfer.command.CancelRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.recurringtransfer.command.CreateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.recurringtransfer.command.FindAllRecurringTransfersCommand;
import me.ferreira.graveto.moneytracker.transactions.service.recurringtransfer.command.UpdateRecurringTransferCommand;

public interface RecurringTransferService {

  RecurringTransfer createRecurringTransfer(CreateRecurringTransferCommand command);

  RecurringTransfer updateRecurringTransfer(UpdateRecurringTransferCommand command);

  List<RecurringTransfer> fetchAllRecurringTransfers(FindAllRecurringTransfersCommand command);

  RecurringTransfer cancelRecurringTransfer(CancelRecurringTransferCommand command);

}
