package me.ferreira.graveto.moneytracker.accounts.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.AddMemberToAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.CloseAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.AddMemberToAccountRequestDto;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.CreateAccountRequestDto;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.AccountResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

  private static final String CLOSE_PATH = "/close";
  private static final String ACCOUNT_SID_PATH = "/{sid}";
  private static final String MEMBERSHIP_PATH = "/memberships";

  private final AccountService accountService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<AccountResponseDto> createAccount(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateAccountRequestDto requestDto) {

    final CreateAccountCommand command = new CreateAccountCommand(
        userSid,
        requestDto.currency(),
        requestDto.initialBalance(),
        requestDto.institution().trim()
    );

    final Account createdAccount = accountService.createAccount(command);

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(ACCOUNT_SID_PATH)
        .buildAndExpand(createdAccount.getSid())
        .toUri();

    return ResponseEntity.created(location).body(AccountResponseDto.from(createdAccount));
  }

  @GetMapping(path = ACCOUNT_SID_PATH, produces = "application/json")
  public ResponseEntity<AccountResponseDto> fetchAccount(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final FetchAccountCommand command = new FetchAccountCommand(userSid, sid);

    final AccountDetails accountDetails = accountService.fetchAccount(command);

    return ResponseEntity.ok().body(AccountResponseDto.from(accountDetails));
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<AccountResponseDto>> fetchAllAccounts(
      @AuthenticationPrincipal final UUID userSid) {

    final List<Account> accounts = accountService.fetchAllAccounts(userSid);

    return ResponseEntity.ok().body(accounts.stream().map(AccountResponseDto::from).toList());
  }

  @PatchMapping(path = ACCOUNT_SID_PATH + CLOSE_PATH, produces = "application/json")
  public ResponseEntity<AccountResponseDto> closeAccount(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid) {

    final CloseAccountCommand command = new CloseAccountCommand(userSid, sid);

    final Account account = accountService.closeAccount(command);

    return ResponseEntity.ok().body(AccountResponseDto.from(account));
  }

  @PostMapping(path = ACCOUNT_SID_PATH + MEMBERSHIP_PATH, produces = "application/json")
  public ResponseEntity<AccountResponseDto> addMemberToAccount(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID sid,
      @Valid @RequestBody final AddMemberToAccountRequestDto requestDto) {

    final AddMemberToAccountCommand command = new AddMemberToAccountCommand(
        userSid,
        sid,
        requestDto.email().trim().toLowerCase(),
        requestDto.role()
    );

    final AccountDetails accountDetails = accountService.addMember(command);

    return ResponseEntity.ok().body(AccountResponseDto.from(accountDetails));
  }

}
