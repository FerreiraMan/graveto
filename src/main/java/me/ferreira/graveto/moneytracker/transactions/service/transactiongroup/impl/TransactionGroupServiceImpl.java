package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.impl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.common.BusinessRuleViolationException;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionGroupNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.GroupAssignmentOutcome;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroupAccount;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.TransactionGroupRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.TransactionGroupAccountRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionGroupService;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command.AssignToGroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command.UngroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.GroupConflict;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.UngroupDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TransactionGroupServiceImpl implements TransactionGroupService {

  private final AccountService accountService;
  private final TransactionService transactionService;
  private final TransactionGroupRepository transactionGroupRepository;
  private final TransactionGroupAccountRepository transactionGroupAccountRepository;

  @Override
  @Transactional
  public AssignToGroupDetails assignToGroup(final AssignToGroupCommand command) {

    final UUID userSid = command.userSid();

    final Account account =
        requireUpdatePermission(userSid, command.accountSid(), "create/update transaction groups");

    final List<Transaction> transactionList =
        loadActiveTransactions(userSid, command.accountSid(), command.transactionSids());

    final Optional<TransactionGroup> existingGroup = findExistingTargetGroup(command);

    rejectIfInOtherGroups(userSid, transactionList, existingGroup.map(TransactionGroup::getSid).orElse(null),
        command.replaceExistingGroup());

    final TransactionGroup transactionGroup = existingGroup
        .orElseGet(() -> transactionGroupRepository.save(TransactionGroup.create(command.groupName().trim(), userSid)));

    linkToAccount(transactionGroup, account);
    final Map<GroupAssignmentOutcome, Integer> outcomeCounts =
        applyToAll(transactionList, t -> t.assignToGroup(transactionGroup));

    final Long totalTransactions =
        transactionService.countByTransactionGroup(transactionGroup);

    return new AssignToGroupDetails(
        transactionGroup.getSid(),
        transactionGroup.getName(),
        totalTransactions,
        outcomeCounts.getOrDefault(GroupAssignmentOutcome.ADDED, 0),
        outcomeCounts.getOrDefault(GroupAssignmentOutcome.MOVED, 0),
        outcomeCounts.getOrDefault(GroupAssignmentOutcome.UNCHANGED, 0)
    );
  }

  @Override
  @Transactional
  public UngroupDetails ungroup(final UngroupCommand command) {

    requireUpdatePermission(command.userSid(), command.accountSid(), "ungroup transactions");

    final List<Transaction> transactionList =
        loadActiveTransactions(command.userSid(), command.accountSid(), command.transactionSids());

    final Map<GroupAssignmentOutcome, Integer> outcomeCounts =
        applyToAll(transactionList, Transaction::removeFromGroup);

    return new UngroupDetails(
        outcomeCounts.getOrDefault(GroupAssignmentOutcome.REMOVED, 0),
        outcomeCounts.getOrDefault(GroupAssignmentOutcome.UNCHANGED, 0)
    );
  }

  private Account requireUpdatePermission(final UUID userSid, final UUID accountSid, final String actionName) {
    final Account account = accountService.fetchAccountEntity(accountSid);
    account.validateUserPermission(userSid, MembershipRole::canUpdateTransaction, actionName);
    return account;
  }

  private List<Transaction> loadActiveTransactions(final UUID userSid, final UUID accountSid,
                                                   final Set<UUID> transactionSids) {

    final List<Transaction> transactionList = transactionService
        .findAllBySidFromAccountAndStatus(transactionSids, accountSid, TransactionStatus.ACTIVE);

    if (transactionList.size() < transactionSids.size()) {
      throw new BusinessRuleViolationException(
          "Unable to find all the requested transactions in account [%s] for user [%s]."
              .formatted(accountSid, userSid),
          "Unable to process the request due to missing or deleted transactions.");
    }
    return transactionList;
  }

  private Optional<TransactionGroup> findExistingTargetGroup(final AssignToGroupCommand command) {

    final List<TransactionGroup> visibleGroups = transactionGroupAccountRepository
        .findAllVisibleToUser(command.userSid());

    if (command.groupSid() != null) {
      return Optional.of(visibleGroups.stream()
          .filter(tg -> command.groupSid().equals(tg.getSid()))
          .findFirst()
          .orElseThrow(() -> new TransactionGroupNotFoundException(
              "Transaction group [%s] was not found or the requester [%s] has no permission to view it."
                  .formatted(command.groupSid(), command.userSid()))));
    }

    return transactionGroupRepository
        .findOldestByName(command.groupName().trim(), visibleGroups)
        .stream()
        .findFirst();
  }

  private void rejectIfInOtherGroups(final UUID userSid, final List<Transaction> transactionList,
                                     final UUID targetGroupSid,
                                     final boolean replaceExistingGroup) {

    if (replaceExistingGroup) {
      return;
    }

    final List<GroupConflict> groupConflicts = transactionList.stream()
        .filter(t -> t.getTransactionGroup() != null)
        .filter(t -> !t.getTransactionGroup().getSid().equals(targetGroupSid))
        .collect(Collectors.groupingBy(t -> t.getTransactionGroup().getSid()))
        .values().stream()
        .map(inGroup -> new GroupConflict(
            inGroup.get(0).getTransactionGroup().getSid(),
            inGroup.get(0).getTransactionGroup().getName(),
            inGroup.stream().map(Transaction::getSid).toList()))
        .toList();

    if (groupConflicts.isEmpty()) {
      return;
    }

    final int conflictingTransactions = groupConflicts.stream()
        .mapToInt(conflict -> conflict.transactionSids().size())
        .sum();

    throw new BusinessRuleViolationException(
        ("User [%s] tried to move [%d] transactions out of [%d] other group(s) into group [%s] " +
            "without replaceExistingGroup.")
            .formatted(userSid, conflictingTransactions, groupConflicts.size(), targetGroupSid),
        "Some of the selected transactions already belong to another group. " +
            "Remove them from their current group first or confirm that they should be moved before retrying.",
        Map.of("group_conflicts", groupConflicts));
  }

  private void linkToAccount(final TransactionGroup transactionGroup, final Account account) {
    transactionGroupAccountRepository.findByTransactionGroupAndAccount(transactionGroup, account)
        .orElseGet(() -> {
          final TransactionGroupAccount transactionGroupAccount =
              TransactionGroupAccount.create(transactionGroup, account);
          return transactionGroupAccountRepository.save(transactionGroupAccount);
        });
  }

  private Map<GroupAssignmentOutcome, Integer> applyToAll(final List<Transaction> transactions,
                                                          final Function<Transaction, GroupAssignmentOutcome> change) {

    final Map<GroupAssignmentOutcome, Integer> counts = new EnumMap<>(GroupAssignmentOutcome.class);

    transactions.forEach(t -> counts.merge(change.apply(t), 1, Integer::sum));
    return counts;
  }

}
