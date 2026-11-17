package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.AssignToGroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.UngroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.payload.UngroupDetails;

public interface TransactionGroupService {

  AssignToGroupDetails assignToGroup(AssignToGroupCommand command);

  UngroupDetails ungroup(UngroupCommand command);

}
