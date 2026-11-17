package me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroupAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionGroupAccountJpaRepository extends JpaRepository<TransactionGroupAccount, Long> {

  @Query(value = "SELECT tga FROM TransactionGroupAccount tga JOIN FETCH tga.transactionGroup " +
      "WHERE tga.transactionGroup = ?1 AND tga.account = ?2")
  Optional<TransactionGroupAccount> findByTransactionGroupAndAccount(final TransactionGroup transactionGroup,
                                                                     final Account account);

  @Query(value = "SELECT DISTINCT tga.transactionGroup FROM TransactionGroupAccount tga " +
      "JOIN tga.account a " +
      "JOIN a.memberships m WHERE m.userSid = ?1")
  List<TransactionGroup> findAllVisibleToUser(final UUID userSid);

}
