package me.ferreira.graveto.moneytracker.accounts;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.AddMemberToAccountRequestDto;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AddMemberToAccountIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private AccountRepository accountRepository;

  @Test
  void shouldAddMemberToAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID newMemberUserSid = UUID.randomUUID();
    final String ownerEmail = "owner@email.com";
    final String newMemberEmail = "new-member@email.com";
    final Account account = AccountTestFactory.createAccountWithOwner(userSid, "Santander", BigDecimal.ONE);
    accountRepository.save(account);

    final AddMemberToAccountRequestDto requestDto = new AddMemberToAccountRequestDto(
        newMemberEmail, MembershipRole.CONTRIBUTOR
    );

    final UserResponseDto mockApiResult = new UserResponseDto(newMemberUserSid, newMemberEmail);
    when(userApi.fetchUserByEmail(newMemberEmail)).thenReturn(Optional.of(mockApiResult));
    when(userApi.fetchUserDetailsByUserSids(Set.of(userSid, newMemberUserSid))).thenReturn(Map.of(
        userSid, new UserResponseDto(userSid, ownerEmail),
        newMemberUserSid, new UserResponseDto(newMemberUserSid, newMemberEmail)
    ));

    // Act
    final Response response = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(requestDto)
        .pathParam("sid", account.getSid())
        .when()
        .post("/accounts/{sid}/memberships");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("users.size()", is(2))
        .body("users.sid", hasItems(userSid.toString(), newMemberUserSid.toString()))
        .body("users.email", hasItems(ownerEmail, newMemberEmail));

    final Account updatedAccount = accountRepository.findBySid(account.getSid()).get();
    assertThat(updatedAccount.getMemberships().size()).isEqualTo(2);
    assertThat(updatedAccount.getMemberships())
        .extracting(AccountMembership::getUserSid)
        .containsExactlyInAnyOrder(userSid, newMemberUserSid);
    assertThat(updatedAccount.getMemberships().stream()
        .filter(m -> m.getUserSid().equals(newMemberUserSid))
        .findFirst().get().getRole())
        .isEqualTo(MembershipRole.CONTRIBUTOR);
  }

}
