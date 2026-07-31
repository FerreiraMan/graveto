package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;

public record AccountResponseDto(
    UUID sid,
    BigDecimal balance,
    String baseCurrency,
    String status,
    String institution,
    List<MembershipResponseDto> users
) {

  public static AccountResponseDto from(final Account account) {
    final List<MembershipResponseDto> mappedMemberships = account.getMemberships().stream()
        .map(MembershipResponseDto::from)
        .toList();

    return new AccountResponseDto(
        account.getSid(),
        account.getBalance(),
        account.getBaseCurrency().name(),
        account.getStatus().name(),
        account.getInstitution(),
        mappedMemberships
    );
  }

  public static AccountResponseDto from(final AccountDetails details) {
    final List<MembershipResponseDto> mappedMemberships = details.users().stream()
        .map(MembershipResponseDto::from)
        .toList();

    return new AccountResponseDto(
        details.sid(),
        details.balance(),
        details.currency().name(),
        details.status().name(),
        details.institution(),
        mappedMemberships
    );
  }

}
