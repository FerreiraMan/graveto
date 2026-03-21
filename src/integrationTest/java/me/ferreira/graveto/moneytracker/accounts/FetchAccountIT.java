package me.ferreira.graveto.moneytracker.accounts;

import io.restassured.response.Response;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.config.BaseIntegrationTest;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAccountIT extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldReturnAccountOfGivenUser() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final String institution = "BCP";
        final BigDecimal balance = BigDecimal.TEN;
        final Account savedAccount = accountRepository.save(AccountTestFactory.createAccountWithOwner(userSid, institution, balance));

        // Act
        final Response response =
                given().
                        header("X-User-Sid", userSid).
                        pathParam("sid", savedAccount.getSid()).
                when().
                        get("/accounts/{sid}").
                then().
                        statusCode(200).
                        extract().
                        response();

        final String returnedAccountSid = response.path("sid");
        final String returnedAccountInstitution = response.path("institution");
        final String returnedAccountBalance = response.path("balance").toString();
        final String returnedAccountUser = response.path("users[0].sid");

        //Assert
        assertThat(returnedAccountSid).isEqualTo(savedAccount.getSid().toString());
        assertThat(returnedAccountInstitution).isEqualTo(savedAccount.getInstitution());
        assertThat(new BigDecimal(returnedAccountBalance)).isEqualByComparingTo(savedAccount.getBalance());
        assertThat(returnedAccountUser).isEqualTo(savedAccount.getMemberships().getFirst().getUserSid().toString());
    }

    @Test
    void shouldNotReturnAccountThatUserIsNotPartOf() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final String institution = "BCP";
        final BigDecimal balance = BigDecimal.TEN;
        final Account savedAccount = accountRepository.save(
                AccountTestFactory.createAccountWithOwner(userSid, institution, balance)
        );

        // Act
        final Optional<Account> account = accountRepository.findBySidAndUserSid(savedAccount.getSid(), UUID.randomUUID());

        // Assert
        assertThat(account).isEmpty();
    }

}
