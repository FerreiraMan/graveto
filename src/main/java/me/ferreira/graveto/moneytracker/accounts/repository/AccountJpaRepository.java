package me.ferreira.graveto.moneytracker.accounts.repository;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    Optional<Account> findBySid(final UUID sid);

    List<Account> findAllByStatus(final AccountStatus status);

    @Query(value = "SELECT a FROM Account a LEFT JOIN FETCH a.memberships WHERE a.sid = ?1")
    Optional<Account> findBySidWithMemberships(final UUID sid);

}
