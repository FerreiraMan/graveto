package me.ferreira.graveto.moneytracker.accounts.repository;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findBySid(UUID sid);

    List<Account> findByStatus(AccountStatus status);

}