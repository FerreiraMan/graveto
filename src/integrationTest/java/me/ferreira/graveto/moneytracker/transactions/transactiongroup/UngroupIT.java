package me.ferreira.graveto.moneytracker.transactions.transactiongroup;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup.UngroupRequestDto;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UngroupIT extends MoneyTrackerBaseIntegrationTest {

  private static final String UNGROUP_PATH = "/transaction-groups/ungroup";
  private static final String ASSIGN_PATH = "/transaction-groups";

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
  void shouldUngroupTransactionsAndKeepTheGroup() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction first = createTransaction(account, group);
    final Transaction second = createTransaction(account, group);

    // Act
    ungroup(userSid, request(account, first, second))
        .statusCode(200)
        .body("removed", is(2))
        .body("alreadyUngrouped", is(0))
        .body("groupSid", nullValue())
        .body("added", nullValue());

    // Assert
    assertThat(groupSidOf(account, first)).isNull();
    assertThat(groupSidOf(account, second)).isNull();
    assertThat(visibleGroupSids(userSid)).containsExactly(group.getSid());
  }

  @Test
  void shouldLeaveOtherTransactionsOfTheGroupUntouched() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction removed = createTransaction(account, group);
    final Transaction kept = createTransaction(account, group);

    // Act
    ungroup(userSid, request(account, removed)).statusCode(200).body("removed", is(1));

    // Assert
    assertThat(groupSidOf(account, removed)).isNull();
    assertThat(groupSidOf(account, kept)).isEqualTo(group.getSid());
  }

  @Test
  void shouldReflectTheUngroupInTheGroupTotalOnTheNextAssign() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction removed = createTransaction(account, group);
    final Transaction kept = createTransaction(account, group);
    final Transaction newcomer = createTransaction(account, null);

    ungroup(userSid, request(account, removed)).statusCode(200);

    // Act & Assert
    assign(userSid, new AssignToGroupRequestDto(
        account.getSid(), group.getSid(), null, sidsOf(newcomer), false))
        .statusCode(200)
        .body("totalTransactions", is(2))
        .body("added", is(1));

    assertThat(groupSidOf(account, kept)).isEqualTo(group.getSid());
    assertThat(groupSidOf(account, newcomer)).isEqualTo(group.getSid());
  }

  @Test
  void shouldLetContributorUngroup() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID contributorSid = UUID.randomUUID();
    final Account account = createAccount(ownerSid, contributorSid);
    final TransactionGroup group = createGroup("Algarve", ownerSid, account);
    final Transaction transaction = createTransaction(account, group);

    // Act & Assert
    ungroup(contributorSid, request(account, transaction))
        .statusCode(200)
        .body("removed", is(1));

    assertThat(groupSidOf(account, transaction)).isNull();
  }

  @Test
  void shouldRejectTheWholeRequestWhenSidBelongsToAnotherAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final Account otherAccount = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction mine = createTransaction(account, group);
    final Transaction notInThisAccount = createTransaction(otherAccount, group);

    // Act & Assert
    ungroup(userSid, request(account, mine, notInThisAccount))
        .statusCode(422)
        .body("detail", is("Unable to process the request due to missing or deleted transactions."))
        .body("timestamp", notNullValue());

    assertThat(groupSidOf(account, mine)).isEqualTo(group.getSid());
    assertThat(groupSidOf(otherAccount, notInThisAccount)).isEqualTo(group.getSid());
  }

  @Test
  void shouldRejectDeletedTransactions() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = createAccount(userSid);
    final TransactionGroup group = createGroup("Algarve", userSid, account);
    final Transaction active = createTransaction(account, group, TransactionStatus.ACTIVE);
    final Transaction deleted = createTransaction(account, group, TransactionStatus.DELETED);

    // Act & Assert
    ungroup(userSid, request(account, active, deleted))
        .statusCode(422)
        .body("detail", is("Unable to process the request due to missing or deleted transactions."));

    assertThat(groupSidOf(account, active)).isEqualTo(group.getSid());
    assertThat(groupSidOf(account, deleted)).isEqualTo(group.getSid());
  }

  private ValidatableResponse ungroup(final UUID userSid, final UngroupRequestDto request) {
    return given()
        .header("Authorization", "Bearer " + userSid)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post(UNGROUP_PATH)
        .then()
        .log().ifValidationFails();
  }

  private ValidatableResponse assign(final UUID userSid, final AssignToGroupRequestDto request) {
    return given()
        .header("Authorization", "Bearer " + userSid)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post(ASSIGN_PATH)
        .then()
        .log().ifValidationFails();
  }

  private UngroupRequestDto request(final Account account, final Transaction... transactions) {
    return new UngroupRequestDto(account.getSid(), sidsOf(transactions));
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
