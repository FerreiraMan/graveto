package me.ferreira.graveto.moneytracker.accounts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountJpaRepository extends JpaRepository<Account, Long> {

  @EntityGraph(attributePaths = {Account_.MEMBERSHIPS})
  Optional<Account> findBySid(final UUID sid);

  List<Account> findAllByStatus(final AccountStatus status);

  @Query(value = "SELECT a FROM Account a JOIN FETCH a.memberships WHERE a.sid = ?1 AND EXISTS " +
      "(SELECT 1 FROM AccountMembership am WHERE am.account = a AND am.userSid = ?2)")
  Optional<Account> findBySidAndUserSid(final UUID accountSid, final UUID userSid);

  @Query(value = "SELECT a FROM Account a LEFT JOIN FETCH a.memberships WHERE a.sid = ?1")
  Optional<Account> findBySidWithMemberships(final UUID sid);

  @Query(value = "SELECT a FROM Account a JOIN a.memberships am WHERE am.userSid = ?1")
  List<Account> findAllByUserSid(final UUID userSid);

}
