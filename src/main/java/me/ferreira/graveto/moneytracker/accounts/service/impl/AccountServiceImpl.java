package me.ferreira.graveto.moneytracker.accounts.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.domain.event.AccountCreatedEvent;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public Account createAccount(final CreateAccountCommand command) {

    final Account account = Account.create(
        command.initialBalance(),
        command.baseCurrency(),
        command.institution()
    );

    final AccountMembership accountMembership = AccountMembership.create(
        command.userSid(),
        MembershipRole.OWNER
    );

    account.addMembership(accountMembership);

    final Account createdAccount = accountRepository.save(account);

    eventPublisher.publishEvent(new AccountCreatedEvent(createdAccount, createdAccount.getBalance()));

    return createdAccount;
  }

  @Override
  @Transactional(readOnly = true)
  public Account fetchAccount(final FetchAccountCommand command) {

    return accountRepository.findBySidAndUserSid(command.accountSid(), command.userSid())
        .orElseThrow(() -> new AccountNotFoundException(command.accountSid()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Account> fetchAllAccounts(final UUID userSid) {

    return accountRepository.findAllByUserSid(userSid);
  }

}
