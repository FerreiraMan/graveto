package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.service.transfer.command.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.command.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.command.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.command.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;

public interface TransferService {

  TransferResult fetchTransfer(FetchTransferCommand command);

  TransferResult createTransfer(CreateTransferCommand command);

  TransferResult deleteTransfer(DeleteTransferCommand command);

  TransferResult updateTransfer(UpdateTransferCommand command);

}
