package me.ferreira.graveto.moneytracker.utils;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;

public class AccountUtils {

  public static Account createAccount(final UUID sid, final UUID userSid, final MembershipRole role) {

    final Account account = new Account();
    account.setSid(sid);
    account.setInstitution("Santander");
    account.setBaseCurrency(Currency.EUR);
    account.setStatus(AccountStatus.ACTIVE);
    account.setBalance(BigDecimal.TEN);

    final AccountMembership membership = new AccountMembership();
    membership.setUserSid(userSid);
    membership.setRole(role);

    account.addMembership(membership);
    return account;
  }

  public static Account createAccount(final UUID sid, final UUID userSid, final BigDecimal amount,
                                      final MembershipRole role) {

    final Account account = new Account();
    account.setSid(sid);
    account.setInstitution("Santander");
    account.setBaseCurrency(Currency.EUR);
    account.setStatus(AccountStatus.ACTIVE);
    account.setBalance(amount);

    final AccountMembership membership = new AccountMembership();
    membership.setUserSid(userSid);
    membership.setRole(role);

    account.addMembership(membership);
    return account;
  }

  public static Account createAccount(final BigDecimal balance) {

    final Account account = new Account();
    account.setSid(UUID.randomUUID());
    account.setInstitution("Santander");
    account.setBaseCurrency(Currency.EUR);
    account.setStatus(AccountStatus.ACTIVE);
    account.setBalance(balance);
    return account;
  }

}
