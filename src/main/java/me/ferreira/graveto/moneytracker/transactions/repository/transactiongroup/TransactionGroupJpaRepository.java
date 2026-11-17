package me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup;

import java.util.List;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionGroupJpaRepository extends JpaRepository<TransactionGroup, Long> {

  @Query(value = "SELECT tg FROM TransactionGroup tg WHERE LOWER(tg.name) = LOWER(?1) AND tg IN ?2 " +
      "ORDER BY tg.createdAt, tg.id")
  List<TransactionGroup> findOldestByName(final String name, final List<TransactionGroup> visibleGroups);

}
