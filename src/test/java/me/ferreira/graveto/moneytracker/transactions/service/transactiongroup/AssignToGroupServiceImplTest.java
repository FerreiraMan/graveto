package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import me.ferreira.graveto.common.web.exception.common.BusinessRuleViolationException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionGroupNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroupAccount;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.TransactionGroupRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.TransactionGroupAccountRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command.AssignToGroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.impl.TransactionGroupServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.GroupConflict;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.TransactionGroupUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class AssignToGroupServiceImplTest {

  @InjectMocks
  private TransactionGroupServiceImpl service;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionService transactionService;
  @Mock
  private TransactionGroupRepository transactionGroupRepository;
  @Mock
  private TransactionGroupAccountRepository transactionGroupAccountRepository;

  private UUID userSid;
  private UUID accountSid;
  private Account account;

  @BeforeEach
  void setUp() {
    userSid = UUID.randomUUID();
    accountSid = UUID.randomUUID();
    account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
  }

  @Test
  void shouldThrowIfAccountIsNotFoundDuringAssignToGroup() {
    // Arrange
    final AssignToGroupCommand command = commandByName("Algarve", Set.of(UUID.randomUUID()), false);
    when(accountService.fetchAccountEntity(accountSid)).thenThrow(new AccountNotFoundException("loggableMessage"));

    // Act & Assert
    assertThatThrownBy(() -> service.assignToGroup(command))
        .isInstanceOf(AccountNotFoundException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(ae.getSafeMessage()).isEqualTo(
              "The specified account is no longer available. " +
                  "It may have been removed, or you may not have access to it.");
        });

    verifyNoInteractions(transactionService, transactionGroupRepository, transactionGroupAccountRepository);
  }

  @Test
  void shouldThrowIfUserCannotUpdateTransactionsDuringAssignToGroup() {
    // Arrange
    final UUID strangerSid = UUID.randomUUID();
    final AssignToGroupCommand command = new AssignToGroupCommand(
        strangerSid, accountSid, null, "Algarve", Set.of(UUID.randomUUID()), false);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> service.assignToGroup(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(ae.getSafeMessage()).isEqualTo("You do not have the required role to perform this action.");
        });

    verifyNoInteractions(transactionService, transactionGroupRepository, transactionGroupAccountRepository);
  }

  @Test
  void shouldThrowIfSomeTransactionsAreMissingDuringAssignToGroup() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID());
    final AssignToGroupCommand command = commandByName("Algarve", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(TransactionGroupUtils.createTransaction(null)));

    // Act & Assert
    assertThatThrownBy(() -> service.assignToGroup(command))
        .isInstanceOf(BusinessRuleViolationException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          assertThat(ae.getSafeMessage())
              .isEqualTo("Unable to process the request due to missing or deleted transactions.");
        });

    verifyNoInteractions(transactionGroupRepository, transactionGroupAccountRepository);
  }

  @Test
  void shouldThrowIfGroupSidIsNotVisibleToUser() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID());
    final TransactionGroup visible = TransactionGroupUtils.createGroup("Birthday");
    final UUID invisibleGroupSid = UUID.randomUUID();
    final AssignToGroupCommand command = new AssignToGroupCommand(
        userSid, accountSid, invisibleGroupSid, null, requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(TransactionGroupUtils.createTransaction(null)));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of(visible));

    // Act & Assert
    assertThatThrownBy(() -> service.assignToGroup(command))
        .isInstanceOf(TransactionGroupNotFoundException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(ae.getSafeMessage()).isEqualTo("The specified transaction group is no longer available. " +
              "It may have been removed, or you may not have access to it.");
        });

    verify(transactionGroupRepository, never()).save(any());
    verify(transactionGroupAccountRepository, never()).save(any());
  }

  @Test
  void shouldAssignToVisibleGroupBySid() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID());
    final TransactionGroup other = TransactionGroupUtils.createGroup("Birthday");
    final TransactionGroup target = TransactionGroupUtils.createGroup("Algarve");
    final Transaction transaction = TransactionGroupUtils.createTransaction(null);
    final AssignToGroupCommand command = new AssignToGroupCommand(
        userSid, accountSid, target.getSid(), null, requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(transaction));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of(other, target));
    when(transactionGroupAccountRepository.findByTransactionGroupAndAccount(target, account))
        .thenReturn(Optional.of(new TransactionGroupAccount()));
    when(transactionService.countByTransactionGroup(target)).thenReturn(1L);

    // Act
    final AssignToGroupDetails details = service.assignToGroup(command);

    // Assert
    assertThat(details.groupSid()).isEqualTo(target.getSid());
    assertThat(details.groupName()).isEqualTo("Algarve");
    assertThat(transaction.getTransactionGroup()).isSameAs(target);
    verify(transactionGroupRepository, never()).findOldestByName(any(), any());
    verify(transactionGroupRepository, never()).save(any());
  }

  @Test
  void shouldCreateGroupAndLinkItToAccountWhenNameMatchesNoVisibleGroup() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID());
    final List<Transaction> transactions = List.of(
        TransactionGroupUtils.createTransaction(null), TransactionGroupUtils.createTransaction(null));
    final AssignToGroupCommand command = commandByName("Algarve", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(transactions);
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of());
    when(transactionGroupRepository.findOldestByName("Algarve", List.of())).thenReturn(List.of());
    when(transactionGroupRepository.save(any(TransactionGroup.class))).thenAnswer(inv -> inv.getArgument(0));
    when(transactionGroupAccountRepository.findByTransactionGroupAndAccount(any(), any()))
        .thenReturn(Optional.empty());
    when(transactionService.countByTransactionGroup(any())).thenReturn(2L);

    // Act
    final AssignToGroupDetails details = service.assignToGroup(command);

    // Assert
    final ArgumentCaptor<TransactionGroup> groupCaptor = ArgumentCaptor.forClass(TransactionGroup.class);
    verify(transactionGroupRepository).save(groupCaptor.capture());
    final TransactionGroup created = groupCaptor.getValue();
    assertThat(created.getName()).isEqualTo("Algarve");
    assertThat(created.getCreatedBy()).isEqualTo(userSid);

    final ArgumentCaptor<TransactionGroupAccount> linkCaptor = ArgumentCaptor.forClass(TransactionGroupAccount.class);
    verify(transactionGroupAccountRepository).save(linkCaptor.capture());
    assertThat(linkCaptor.getValue().getTransactionGroup()).isSameAs(created);
    assertThat(linkCaptor.getValue().getAccount()).isSameAs(account);

    assertThat(transactions).allSatisfy(t -> assertThat(t.getTransactionGroup()).isSameAs(created));
    verify(transactionService).countByTransactionGroup(created);

    assertThat(details.groupSid()).isEqualTo(created.getSid());
    assertThat(details.groupName()).isEqualTo("Algarve");
    assertThat(details.totalTransactions()).isEqualTo(2L);
    assertThat(details.added()).isEqualTo(2);
    assertThat(details.moved()).isZero();
    assertThat(details.alreadyInGroup()).isZero();
  }

  @Test
  void shouldTrimGroupNameBeforeLookupAndCreation() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID());
    final AssignToGroupCommand command = commandByName("  Algarve  ", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(TransactionGroupUtils.createTransaction(null)));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of());
    when(transactionGroupRepository.findOldestByName("Algarve", List.of())).thenReturn(List.of());
    when(transactionGroupRepository.save(any(TransactionGroup.class))).thenAnswer(inv -> inv.getArgument(0));
    when(transactionGroupAccountRepository.findByTransactionGroupAndAccount(any(), any()))
        .thenReturn(Optional.empty());

    // Act
    final AssignToGroupDetails details = service.assignToGroup(command);

    // Assert
    assertThat(details.groupName()).isEqualTo("Algarve");
    verify(transactionGroupRepository).findOldestByName("Algarve", List.of());
  }

  @Test
  void shouldReuseExistingGroupFoundByNameAndKeepExistingLink() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID());
    final TransactionGroup existing = TransactionGroupUtils.createGroup("Algarve");
    final List<TransactionGroup> visible = List.of(existing);
    final Transaction transaction = TransactionGroupUtils.createTransaction(null);
    final AssignToGroupCommand command = commandByName("Algarve", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(transaction));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(visible);
    when(transactionGroupRepository.findOldestByName("Algarve", visible)).thenReturn(List.of(existing));
    when(transactionGroupAccountRepository.findByTransactionGroupAndAccount(existing, account))
        .thenReturn(Optional.of(new TransactionGroupAccount()));
    when(transactionService.countByTransactionGroup(existing)).thenReturn(4L);

    // Act
    final AssignToGroupDetails details = service.assignToGroup(command);

    // Assert
    assertThat(details.groupSid()).isEqualTo(existing.getSid());
    assertThat(details.totalTransactions()).isEqualTo(4L);
    assertThat(transaction.getTransactionGroup()).isSameAs(existing);
    verify(transactionGroupRepository, never()).save(any());
    verify(transactionGroupAccountRepository, never()).save(any());
  }

  @Test
  void shouldCountTransactionsAlreadyInTargetGroupWithoutReportingConflict() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID());
    final TransactionGroup target = TransactionGroupUtils.createGroup("Algarve");
    final Transaction alreadyIn = TransactionGroupUtils.createTransaction(target);
    final Transaction ungrouped = TransactionGroupUtils.createTransaction(null);
    final AssignToGroupCommand command = commandByName("Algarve", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(alreadyIn, ungrouped));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of(target));
    when(transactionGroupRepository.findOldestByName("Algarve", List.of(target))).thenReturn(List.of(target));
    when(transactionGroupAccountRepository.findByTransactionGroupAndAccount(target, account))
        .thenReturn(Optional.of(new TransactionGroupAccount()));
    when(transactionService.countByTransactionGroup(target)).thenReturn(2L);

    // Act
    final AssignToGroupDetails details = service.assignToGroup(command);

    // Assert
    assertThat(details.added()).isEqualTo(1);
    assertThat(details.moved()).isZero();
    assertThat(details.alreadyInGroup()).isEqualTo(1);
    assertThat(details.totalTransactions()).isEqualTo(2L);
  }

  @Test
  void shouldRejectWithGroupConflictsAndChangeNothingWhenReplaceFlagIsFalse() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID());
    final TransactionGroup birthday = TransactionGroupUtils.createGroup("Birthday");
    final Transaction inBirthday = TransactionGroupUtils.createTransaction(birthday);
    final Transaction ungrouped = TransactionGroupUtils.createTransaction(null);
    final AssignToGroupCommand command = commandByName("Algarve", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(inBirthday, ungrouped));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of(birthday));
    when(transactionGroupRepository.findOldestByName("Algarve", List.of(birthday))).thenReturn(List.of());

    // Act & Assert
    assertThatThrownBy(() -> service.assignToGroup(command))
        .isInstanceOf(BusinessRuleViolationException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          assertThat(ae.getSafeMessage()).isEqualTo(
              "Some of the selected transactions already belong to another group. " +
                  "Remove them from their current group first or confirm that they should be moved before retrying.");
          assertThat(ae.getExtraProperties().get("group_conflicts"))
              .asInstanceOf(InstanceOfAssertFactories.list(GroupConflict.class))
              .containsExactly(new GroupConflict(birthday.getSid(), "Birthday", List.of(inBirthday.getSid())));
        });

    verify(transactionGroupRepository, never()).save(any());
    verify(transactionGroupAccountRepository, never()).save(any());
    assertThat(inBirthday.getTransactionGroup()).isSameAs(birthday);
    assertThat(ungrouped.getTransactionGroup()).isNull();
  }

  @Test
  void shouldReportOneConflictPerOtherGroup() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    final TransactionGroup birthday = TransactionGroupUtils.createGroup("Birthday");
    final TransactionGroup wedding = TransactionGroupUtils.createGroup("Wedding");
    final Transaction birthday1 = TransactionGroupUtils.createTransaction(birthday);
    final Transaction birthday2 = TransactionGroupUtils.createTransaction(birthday);
    final Transaction wedding1 = TransactionGroupUtils.createTransaction(wedding);
    final AssignToGroupCommand command = commandByName("Algarve", requested, false);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(birthday1, birthday2, wedding1));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(List.of(birthday, wedding));
    when(transactionGroupRepository.findOldestByName("Algarve", List.of(birthday, wedding)))
        .thenReturn(List.of());

    // Act & Assert
    assertThatThrownBy(() -> service.assignToGroup(command))
        .isInstanceOf(BusinessRuleViolationException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getExtraProperties().get("group_conflicts"))
              .asInstanceOf(InstanceOfAssertFactories.list(GroupConflict.class))
              .containsExactlyInAnyOrder(
                  new GroupConflict(birthday.getSid(), "Birthday", List.of(birthday1.getSid(), birthday2.getSid())),
                  new GroupConflict(wedding.getSid(), "Wedding", List.of(wedding1.getSid())));
        });
  }

  @Test
  void shouldMoveTransactionsOutOfOtherGroupsWhenReplaceFlagIsTrue() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    final TransactionGroup birthday = TransactionGroupUtils.createGroup("Birthday");
    final TransactionGroup target = TransactionGroupUtils.createGroup("Algarve");
    final Transaction inOtherGroup = TransactionGroupUtils.createTransaction(birthday);
    final Transaction alreadyIn = TransactionGroupUtils.createTransaction(target);
    final Transaction ungrouped = TransactionGroupUtils.createTransaction(null);
    final List<TransactionGroup> visible = List.of(birthday, target);
    final AssignToGroupCommand command = commandByName("Algarve", requested, true);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(inOtherGroup, alreadyIn, ungrouped));
    when(transactionGroupAccountRepository.findAllVisibleToUser(userSid)).thenReturn(visible);
    when(transactionGroupRepository.findOldestByName("Algarve", visible)).thenReturn(List.of(target));
    when(transactionGroupAccountRepository.findByTransactionGroupAndAccount(target, account))
        .thenReturn(Optional.of(new TransactionGroupAccount()));
    when(transactionService.countByTransactionGroup(target)).thenReturn(3L);

    // Act
    final AssignToGroupDetails details = service.assignToGroup(command);

    // Assert
    assertThat(details.added()).isEqualTo(1);
    assertThat(details.moved()).isEqualTo(1);
    assertThat(details.alreadyInGroup()).isEqualTo(1);
    assertThat(details.totalTransactions()).isEqualTo(3L);
    assertThat(List.of(inOtherGroup, alreadyIn, ungrouped))
        .allSatisfy(t -> assertThat(t.getTransactionGroup()).isSameAs(target));
  }

  private AssignToGroupCommand commandByName(final String groupName, final Set<UUID> transactionSids,
                                             final boolean replaceExistingGroup) {
    return new AssignToGroupCommand(userSid, accountSid, null, groupName, transactionSids, replaceExistingGroup);
  }

}
