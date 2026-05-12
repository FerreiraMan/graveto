package me.ferreira.graveto.moneytracker.accounts.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.CreateAccountRequestDto;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.AccountResponseDto;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.AccountSummaryResponseDto;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.FullAccountResponseDto;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.MembershipResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/accounts")
@RequiredArgsConstructor
public class AccountController {

  private static final String ACCOUNT_SID_PATH = "/{sid}";

  private final AccountService accountService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<AccountResponseDto> createAccount(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateAccountRequestDto requestDto) {

    final CreateAccountCommand command = new CreateAccountCommand(
        userSid,
        requestDto.currency(),
        requestDto.initialBalance(),
        requestDto.institution()
    );

    final Account createdAccount = accountService.createAccount(command);

    final AccountResponseDto response = new AccountResponseDto(
        createdAccount.getSid(),
        createdAccount.getStatus().name()
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(ACCOUNT_SID_PATH)
        .buildAndExpand(createdAccount.getSid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping(path = ACCOUNT_SID_PATH, produces = "application/json")
  public ResponseEntity<FullAccountResponseDto> fetchAccount(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final FetchAccountCommand command = new FetchAccountCommand(userSid, sid);

    final Account account = accountService.fetchAccount(command);

    final List<MembershipResponseDto> membershipResponseDto = account.getMemberships().stream()
        .map(m -> new MembershipResponseDto(m.getUserSid(), m.getRole().name()))
        .toList();

    final FullAccountResponseDto responseDto = new FullAccountResponseDto(
        account.getSid(),
        account.getBalance(),
        account.getBaseCurrency().name(),
        account.getStatus().name(),
        account.getInstitution(),
        membershipResponseDto
    );

    return ResponseEntity.ok().body(responseDto);
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<AccountSummaryResponseDto>> fetchAllAccounts(
      @AuthenticationPrincipal final UUID userSid) {

    final List<Account> accounts = accountService.fetchAllAccounts(userSid);

    final List<AccountSummaryResponseDto> responseDto = accounts.stream()
        .map(acc -> new AccountSummaryResponseDto(
            acc.getSid(),
            acc.getInstitution(),
            acc.getBalance(),
            acc.getBaseCurrency().name(),
            acc.getStatus().name())
        )
        .toList();

    return ResponseEntity.ok().body(responseDto);
  }

}
