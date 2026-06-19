package me.ferreira.graveto.portfolio.brokers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.brokers.web.request.CreateBrokerRequestDto;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateBrokerIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;

  @Test
  void shouldCreateBroker() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final CreateBrokerRequestDto request = new CreateBrokerRequestDto(accountSid, "DEGIRO", Currency.EUR);

    // Act
    final String brokerSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/brokers")
        .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("sid", notNullValue())
        .body("name", equalTo("DEGIRO"))
        .body("status", equalTo(BrokerStatus.ACTIVE.name()))
        .extract()
        .path("sid");

    // Assert
    final Optional<Broker> savedBroker =
        brokerRepository.findBySid(UUID.fromString(brokerSid));

    assertThat(savedBroker).isPresent();
    assertThat(savedBroker.get().getName()).isEqualTo("DEGIRO");
    assertThat(savedBroker.get().getCurrency()).isEqualTo(Currency.EUR);
    assertThat(savedBroker.get().getAccountSid()).isEqualTo(accountSid);
    assertThat(savedBroker.get().getStatus()).isEqualTo(BrokerStatus.ACTIVE);
    assertThat(savedBroker.get().getMemberships()).hasSize(1);
    assertThat(savedBroker.get().getMemberships().getFirst().getUserSid()).isEqualTo(userSid);
    assertThat(savedBroker.get().getMemberships().getFirst().getRole()).isEqualTo(BrokerMembershipRole.OWNER);
  }

}
