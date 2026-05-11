package me.ferreira.graveto.moneytracker.accounts.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.CreateAccountRequestDTO;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.AccountResponseDTO;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.AccountSummaryResponseDTO;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.FullAccountResponseDTO;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.MembershipResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String ACCOUNT_SID_PATH = "/{sid}";

    private final AccountService accountService;

    @PostMapping(produces = "application/json")
    public ResponseEntity<AccountResponseDTO> createAccount(
        @AuthenticationPrincipal final UUID userSid,
        @Valid @RequestBody final CreateAccountRequestDTO requestDTO) {

        final CreateAccountCommand command = new CreateAccountCommand(
                userSid,
                requestDTO.currency(),
                requestDTO.initialBalance(),
                requestDTO.institution()
        );

        final Account createdAccount = accountService.createAccount(command);

        final AccountResponseDTO response = new AccountResponseDTO(
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
    public ResponseEntity<FullAccountResponseDTO> fetchAccount(
        @AuthenticationPrincipal final UUID userSid,
        @PathVariable final UUID sid) {

        final FetchAccountCommand command = new FetchAccountCommand(userSid, sid);

        final Account account = accountService.fetchAccount(command);

        final List<MembershipResponseDTO> membershipResponseDTO = account.getMemberships().stream()
                .map(m -> new MembershipResponseDTO(m.getUserSid(), m.getRole().name()))
                .toList();

        final FullAccountResponseDTO responseDTO = new FullAccountResponseDTO(
                account.getSid(),
                account.getBalance(),
                account.getBaseCurrency().name(),
                account.getStatus().name(),
                account.getInstitution(),
                membershipResponseDTO
        );

        return ResponseEntity.ok().body(responseDTO);
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<AccountSummaryResponseDTO>> fetchAllAccounts(
        @AuthenticationPrincipal final UUID userSid) {

        final List<Account> accounts = accountService.fetchAllAccounts(userSid);

        final List<AccountSummaryResponseDTO> responseDTO = accounts.stream()
                .map(acc -> new AccountSummaryResponseDTO(
                    acc.getSid(),
                    acc.getInstitution(),
                    acc.getBalance(),
                    acc.getBaseCurrency().name(),
                    acc.getStatus().name())
                )
                .toList();

        return ResponseEntity.ok().body(responseDTO);
    }

}
