package me.ferreira.graveto.moneytracker.transactions.service.transfer;

import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;

public interface TransferService {

  TransferResult fetchTransfer(FetchTransferCommand command);

  TransferResult createTransfer(CreateTransferCommand command);

  TransferResult deleteTransfer(DeleteTransferCommand command);

  TransferResult updateTransfer(UpdateTransferCommand command);

}
