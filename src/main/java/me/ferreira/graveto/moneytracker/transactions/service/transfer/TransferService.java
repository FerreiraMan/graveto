package me.ferreira.graveto.moneytracker.transactions.service.transfer;

import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;

public interface TransferService {

    TransferResult createTransfer(CreateTransferCommand command);

}
