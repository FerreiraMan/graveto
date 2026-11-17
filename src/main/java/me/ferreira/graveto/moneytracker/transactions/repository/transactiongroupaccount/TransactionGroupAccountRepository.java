package me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroupAccount;

public interface TransactionGroupAccountRepository {

  TransactionGroupAccount save(TransactionGroupAccount transactionGroupAccount);

  Optional<TransactionGroupAccount> findByTransactionGroupAndAccount(TransactionGroup transactionGroup,
                                                                     Account account);

  List<TransactionGroup> findAllVisibleToUser(UUID userSid);

}
