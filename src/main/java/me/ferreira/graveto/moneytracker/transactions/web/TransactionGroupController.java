package me.ferreira.graveto.moneytracker.transactions.web;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionGroupService;
import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.AssignToGroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.UngroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.payload.UngroupDetails;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup.AssignToGroupRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup.UngroupRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.response.transactiongroup.TransactionGroupResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/transaction-groups")
@RequiredArgsConstructor
public class TransactionGroupController {

  private static final String UNGROUP_PATH = "/ungroup";

  private final TransactionGroupService transactionGroupService;

  @PostMapping(produces = "application/json")
  public ResponseEntity<TransactionGroupResponseDto> assignToGroup(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final AssignToGroupRequestDto requestDto) {

    final AssignToGroupCommand command = new AssignToGroupCommand(
        userSid,
        requestDto.accountSid(),
        requestDto.groupSid(),
        requestDto.groupName(),
        requestDto.transactionSids(),
        requestDto.replaceExistingGroup()
    );

    final AssignToGroupDetails assignToGroupDetails = transactionGroupService.assignToGroup(command);

    return ResponseEntity.ok().body(TransactionGroupResponseDto.from(assignToGroupDetails));
  }

  @PostMapping(path = UNGROUP_PATH, produces = "application/json")
  public ResponseEntity<TransactionGroupResponseDto> ungroup(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final UngroupRequestDto requestDto) {

    final UngroupCommand command = new UngroupCommand(
        userSid,
        requestDto.accountSid(),
        requestDto.transactionSids()
    );

    final UngroupDetails ungroupDetails = transactionGroupService.ungroup(command);

    return ResponseEntity.ok(TransactionGroupResponseDto.from(ungroupDetails));
  }

}
