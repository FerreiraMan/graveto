package me.ferreira.graveto.moneytracker.accounts.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.identity.api.UserApi;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.domain.event.AccountClosedEvent;
import me.ferreira.graveto.moneytracker.accounts.domain.event.AccountCreatedEvent;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CloseAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final UserApi userApi;
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

    log.info("Account created successfully. AccountSid: {}", createdAccount.getSid());
    eventPublisher.publishEvent(new AccountCreatedEvent(createdAccount, createdAccount.getBalance()));

    return createdAccount;
  }

  @Override
  @Transactional(readOnly = true)
  public Account fetchAccountEntity(final UUID accountSid) {

    return accountRepository.findBySid(accountSid)
        .orElseThrow(() -> new AccountNotFoundException(accountSid));
  }

  @Override
  @Transactional(readOnly = true)
  public AccountDetails fetchAccount(final FetchAccountCommand command) {

    final Account account = accountRepository.findBySidAndUserSid(command.accountSid(), command.userSid())
        .orElseThrow(() -> new AccountNotFoundException(command.accountSid()));

    final Set<UUID> userList = account.getMemberships().stream()
        .map(AccountMembership::getUserSid)
        .collect(Collectors.toSet());

    final Map<UUID, UserResponseDto> accountUsersInfo = userApi.fetchUserDetailsByUserSids(userList);

    final List<AccountDetails.MembershipDetails> userDetailsList = account.getMemberships().stream()
        .map(m -> new AccountDetails.MembershipDetails(
            m.getUserSid(),
            accountUsersInfo.getOrDefault(m.getUserSid(), new UserResponseDto("")).email(),
            m.getRole().name()))
        .toList();

    return new AccountDetails(
        account.getSid(),
        account.getBalance(),
        account.getBaseCurrency(),
        account.getStatus(),
        account.getInstitution(),
        userDetailsList
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<Account> fetchAllAccounts(final UUID userSid) {

    return accountRepository.findAllByUserSid(userSid);
  }

  @Override
  @Transactional
  public Account closeAccount(final CloseAccountCommand command) {

    final Account account = accountRepository.findBySid(command.accountSid())
        .orElseThrow(() -> new AccountNotFoundException(command.accountSid()));

    account.validateUserPermission(command.userSid(), MembershipRole::canCloseAccount, "request closure");
    account.close();

    log.info("Account closed successfully. AccountSid: {}", account.getSid());
    eventPublisher.publishEvent(new AccountClosedEvent(account));

    return account;
  }

}
