package me.ferreira.graveto.moneytracker.accounts.repository.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountJpaRepository;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository repository;

    @Override
    public Account save(final Account account) {
        return repository.save(account);
    }

    @Override
    public Optional<Account> findBySid(final UUID sid) {
        return repository.findBySid(sid);
    }

    @Override
    public List<Account> findByStatus(final AccountStatus status) {
        return repository.findAllByStatus(status);
    }

}
