package me.ferreira.graveto.moneytracker.transactions.transactiongroup;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroupAccount;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.TransactionGroupRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.TransactionGroupAccountRepository;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup.AssignToGroupRequestDto;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AssignToGroupIT extends MoneyTrackerBaseIntegrationTest {

  private static final String PATH = "/transaction-groups";

  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;
  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private TransactionGroupRepository transactionGroupRepository;
  @Autowired
  private TransactionGroupAccountRepository transactionGroupAccountRepository;

  @Test
  void shouldCreateGroupByNameWhenUserHasNoVisibleGroups() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final Transaction first = createTransaction(account, null);
    final Transaction second = createTransaction(account, null);

    // Act
    final String groupSid = assign(userSid, byName(account, "Algarve", false, first, second))
        .statusCode(200)
        .body("groupName", is("Algarve"))
        .body("totalTransactions", is(2))
        .body("added", is(2))
        .body("moved", is(0))
        .body("alreadyInGroup", is(0))
        .body("removed", nullValue())
        .body("alreadyUngrouped", nullValue())
        .extract().path("groupSid");

    // Assert
    assertThat(groupSidOf(account, first)).isEqualTo(UUID.fromString(groupSid));
    assertThat(groupSidOf(account, second)).isEqualTo(UUID.fromString(groupSid));
    assertThat(visibleGroupSids(userSid)).containsExactly(UUID.fromString(groupSid));
  }

  @Test
  void shouldReuseGroupByNameAndReportTheTotalAcrossRequests() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final Transaction first = createTransaction(account, null);
    final Transaction second = createTransaction(account, null);
    final Transaction third = createTransaction(account, null);

    final String groupSid = assign(userSid, byName(account, "Algarve", false, first, second))
        .statusCode(200)
        .extract().path("groupSid");

    // Act
    assign(userSid, byName(account, "Algarve", false, third))
        .statusCode(200)
        .body("groupSid", is(groupSid))
        .body("totalTransactions", is(3))
        .body("added", is(1))
        .body("alreadyInGroup", is(0));

    // Assert
    assertThat(groupSidOf(account, third)).isEqualTo(UUID.fromString(groupSid));
    assertThat(visibleGroupSids(userSid)).hasSize(1);
  }

  @Test
  void shouldCountAddedAndAlreadyInGroupInTheSameRequest() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction alreadyIn = createTransaction(account, group);
    final Transaction ungrouped = createTransaction(account, null);

    // Act & Assert
    assign(userSid, byName(account, "Algarve", false, alreadyIn, ungrouped))
        .statusCode(200)
        .body("groupSid", is(group.getSid().toString()))
        .body("totalTransactions", is(2))
        .body("added", is(1))
        .body("moved", is(0))
        .body("alreadyInGroup", is(1));
  }

  @Test
  void shouldMatchGroupNameIgnoringCase() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction transaction = createTransaction(account, null);

    // Act & Assert
    assign(userSid, byName(account, "ALGARVE", false, transaction))
        .statusCode(200)
        .body("groupSid", is(group.getSid().toString()));

    assertThat(visibleGroupSids(userSid)).hasSize(1);
  }

  @Test
  void shouldMatchGroupNameIgnoringAccents() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Férias", userSid, account);
    final Transaction transaction = createTransaction(account, null);

    // Act & Assert
    assign(userSid, byName(account, "ferias", false, transaction))
        .statusCode(200)
        .body("groupSid", is(group.getSid().toString()));

    assertThat(visibleGroupSids(userSid)).hasSize(1);
  }

  @Test
  void shouldRejectTwoGroupsOfTheSameCreatorWhoseNamesDifferOnlyByCase() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    transactionGroupRepository.save(TransactionGroup.create("Vacations", userSid));

    // Act & Assert
    assertThatThrownBy(() -> transactionGroupRepository.save(TransactionGroup.create("vacations", userSid)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectTwoGroupsOfTheSameCreatorWhoseNamesDifferOnlyByAccents() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    transactionGroupRepository.save(TransactionGroup.create("Férias", userSid));

    // Act & Assert
    assertThatThrownBy(() -> transactionGroupRepository.save(TransactionGroup.create("Ferias", userSid)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldPickTheOldestWhenTwoVisibleGroupsShareTheSameName() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup oldest = createGroup("Algarve", UUID.randomUUID(), account);
    createGroup("Algarve", UUID.randomUUID(), account);
    final Transaction transaction = createTransaction(account, null);

    // Act & Assert
    assign(userSid, byName(account, "Algarve", false, transaction))
        .statusCode(200)
        .body("groupSid", is(oldest.getSid().toString()));
  }

  @Test
  void shouldLetAnotherMemberOfLinkedAccountAssignToTheGroup() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID contributorSid = UUID.randomUUID();
    final Account account = createAccount(ownerSid, contributorSid);
    final TransactionGroup group = createGroup("Algarve", ownerSid, account);
    final Transaction transaction = createTransaction(account, null);

    // Act & Assert
    assign(contributorSid, bySid(account, group.getSid(), false, transaction))
        .statusCode(200)
        .body("groupSid", is(group.getSid().toString()))
        .body("added", is(1));

    assertThat(visibleGroupSids(contributorSid)).containsExactly(group.getSid());
  }

  @Test
  void shouldLinkTheGroupToSecondAccountAndNotDuplicateTheLinkOnRepeat() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID secondAccountMemberSid = UUID.randomUUID();
    final Account firstAccount = createAccount(ownerSid);
    final Account secondAccount = createAccount(ownerSid, secondAccountMemberSid);
    final Transaction inFirst = createTransaction(firstAccount, null);
    final Transaction inSecond = createTransaction(secondAccount, null);

    final UUID groupSid = UUID.fromString(assign(ownerSid, byName(firstAccount, "Algarve", false, inFirst))
        .statusCode(200)
        .extract().path("groupSid"));

    assertThat(visibleGroupSids(secondAccountMemberSid)).isEmpty();

    // Act
    assign(ownerSid, bySid(secondAccount, groupSid, false, inSecond))
        .statusCode(200)
        .body("totalTransactions", is(2));
    assign(ownerSid, bySid(secondAccount, groupSid, false, inSecond))
        .statusCode(200)
        .body("alreadyInGroup", is(1));

    // Assert
    assertThat(visibleGroupSids(secondAccountMemberSid)).containsExactly(groupSid);
  }

  @Test
  void shouldRejectWithGroupConflictsAndChangeNothingWhenReplaceFlagIsFalse() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup birthday = createGroup("Birthday", userSid, account);
    final Transaction inBirthday = createTransaction(account, birthday);
    final Transaction ungrouped = createTransaction(account, null);

    // Act & Assert
    assign(userSid, byName(account, "Algarve", false, inBirthday, ungrouped))
        .statusCode(422)
        .body("timestamp", notNullValue())
        .body("group_conflicts", hasSize(1))
        .body("group_conflicts[0].groupSid", is(birthday.getSid().toString()))
        .body("group_conflicts[0].groupName", is("Birthday"))
        .body("group_conflicts[0].transactionSids", contains(inBirthday.getSid().toString()));

    assertThat(groupSidOf(account, inBirthday)).isEqualTo(birthday.getSid());
    assertThat(groupSidOf(account, ungrouped)).isNull();
    assertThat(visibleGroupSids(userSid)).containsExactly(birthday.getSid());
  }

  @Test
  void shouldMoveTransactionsWhenReplaceFlagIsTrue() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup birthday = createGroup("Birthday", userSid, account);
    final Transaction inBirthday = createTransaction(account, birthday);
    final Transaction ungrouped = createTransaction(account, null);

    // Act
    final UUID algarveSid = UUID.fromString(assign(userSid, byName(account, "Algarve", true, inBirthday, ungrouped))
        .statusCode(200)
        .body("totalTransactions", is(2))
        .body("added", is(1))
        .body("moved", is(1))
        .body("alreadyInGroup", is(0))
        .extract().path("groupSid"));

    // Assert
    assertThat(groupSidOf(account, inBirthday)).isEqualTo(algarveSid);
    assertThat(groupSidOf(account, ungrouped)).isEqualTo(algarveSid);
  }

  @Test
  void shouldReturnNotFoundForGroupVisibleOnlyToAnotherHousehold() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID strangerSid = UUID.randomUUID();
    final Account ownerAccount = createAccount(ownerSid);
    final Account strangerAccount = createAccount(strangerSid);
    final TransactionGroup group = createGroup("Algarve", ownerSid, ownerAccount);
    final Transaction transaction = createTransaction(strangerAccount, null);

    // Act & Assert
    assign(strangerSid, bySid(strangerAccount, group.getSid(), false, transaction))
        .statusCode(404)
        .body("detail", is("The specified transaction group is no longer available. "
            + "It may have been removed, or you may not have access to it."));

    assertThat(groupSidOf(strangerAccount, transaction)).isNull();
  }

  @Test
  void shouldRejectTheWholeRequestWhenSidBelongsToAnotherAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final Account otherAccount = createAccount(userSid);
    final Transaction mine = createTransaction(account, null);
    final Transaction notInThisAccount = createTransaction(otherAccount, null);

    // Act & Assert
    assign(userSid, byName(account, "Algarve", false, mine, notInThisAccount))
        .statusCode(422)
        .body("detail", is("Unable to process the request due to missing or deleted transactions."));

    assertThat(groupSidOf(account, mine)).isNull();
    assertThat(groupSidOf(otherAccount, notInThisAccount)).isNull();
    assertThat(visibleGroupSids(userSid)).isEmpty();
  }

  @Test
  void shouldRejectDeletedTransactions() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final Transaction deleted = createTransaction(account, null, TransactionStatus.DELETED);

    // Act & Assert
    assign(userSid, byName(account, "Algarve", false, deleted))
        .statusCode(422)
        .body("detail", is("Unable to process the request due to missing or deleted transactions."));

    assertThat(visibleGroupSids(userSid)).isEmpty();
  }

  private ValidatableResponse assign(final UUID userSid, final AssignToGroupRequestDto request) {
    return given()
        .header("Authorization", "Bearer " + userSid)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post(PATH)
        .then()
        .log().ifValidationFails();
  }

  private AssignToGroupRequestDto byName(final Account account, final String groupName,
                                         final boolean replaceExistingGroup, final Transaction... transactions) {
    return new AssignToGroupRequestDto(account.getSid(), null, groupName, sidsOf(transactions),
        replaceExistingGroup);
  }

  private AssignToGroupRequestDto bySid(final Account account, final UUID groupSid,
                                        final boolean replaceExistingGroup, final Transaction... transactions) {
    return new AssignToGroupRequestDto(account.getSid(), groupSid, null, sidsOf(transactions),
        replaceExistingGroup);
  }

  private Set<UUID> sidsOf(final Transaction... transactions) {
    return Arrays.stream(transactions).map(Transaction::getSid).collect(Collectors.toSet());
  }

  private Account createAccount(final UUID ownerSid) {
    final Account account = AccountTestFactory.createAccountWithOwner(ownerSid, "Santander", BigDecimal.TEN);
    accountRepository.save(account);
    return account;
  }

  private Account createAccount(final UUID ownerSid, final UUID contributorSid) {
    final Account account = AccountTestFactory.createAccountWithOwner(ownerSid, "Santander", BigDecimal.TEN);
    account.addMembership(AccountMembership.create(contributorSid, MembershipRole.CONTRIBUTOR));
    accountRepository.save(account);
    return account;
  }

  private Transaction createTransaction(final Account account, final TransactionGroup group) {
    return createTransaction(account, group, TransactionStatus.ACTIVE);
  }

  private Transaction createTransaction(final Account account, final TransactionGroup group,
                                        final TransactionStatus status) {
    final Category category = categoryRepository
        .findAll(new FindAllCategoriesCommand(UUID.randomUUID(), null, null, null, null))
        .getFirst();

    final Transaction transaction = TransactionTestFactory.createTransaction(
        account, category, TransactionType.EXPENSE, BigDecimal.TEN, status, LocalDate.now());
    transaction.setTransactionGroup(group);
    return transactionRepository.save(transaction);
  }

  private TransactionGroup createGroup(final String name, final UUID createdBy, final Account linkedAccount) {
    final TransactionGroup group = transactionGroupRepository.save(TransactionGroup.create(name, createdBy));
    transactionGroupAccountRepository.save(TransactionGroupAccount.create(group, linkedAccount));
    return group;
  }

  private UUID groupSidOf(final Account account, final Transaction transaction) {
    final Transaction reloaded = transactionRepository
        .findAllBySidFromAccountAndStatus(Set.of(transaction.getSid()), account.getSid(), transaction.getStatus())
        .getFirst();
    return reloaded.getTransactionGroup() == null ? null : reloaded.getTransactionGroup().getSid();
  }

  private List<UUID> visibleGroupSids(final UUID userSid) {
    return transactionGroupAccountRepository.findAllVisibleToUser(userSid).stream()
        .map(TransactionGroup::getSid)
        .toList();
  }

}
