package me.ferreira.graveto.moneytracker.utils;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;

public class AccountTestFactory {

  public static Account createAccountWithOwner(UUID userSid, String institution, BigDecimal balance) {

    final Account account = Account.create(balance, Currency.EUR, institution);
    final AccountMembership ownerMembership = AccountMembership.create(userSid, MembershipRole.OWNER);
    account.addMembership(ownerMembership);
    return account;
  }

}
