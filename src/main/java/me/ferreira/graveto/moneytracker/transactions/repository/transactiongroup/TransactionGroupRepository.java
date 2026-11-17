package me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup;

import java.util.List;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;

public interface TransactionGroupRepository {

  TransactionGroup save(TransactionGroup transactionGroup);

  List<TransactionGroup> findOldestByName(String name, List<TransactionGroup> visibleGroups);

}
