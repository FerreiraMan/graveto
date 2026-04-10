package me.ferreira.graveto.moneytracker.accounts.repository;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    List<Account> saveAll(List<Account> accounts);

    Optional<Account> findBySid(UUID sid);

    Optional<Account> findBySidAndUserSid(UUID sid, UUID userSid);

    Optional<Account> findBySidWithMemberships(UUID sid);

    List<Account> findAllByUserSid(UUID userSid);

    List<Account> findByStatus(AccountStatus status);

}