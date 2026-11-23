package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command.AssignToGroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command.UngroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.UngroupDetails;

public interface TransactionGroupService {

  AssignToGroupDetails assignToGroup(AssignToGroupCommand command);

  UngroupDetails ungroup(UngroupCommand command);

}
