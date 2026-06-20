package me.ferreira.graveto.portfolio.brokers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import me.ferreira.graveto.portfolio.utils.BrokerTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchBrokerIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;

  @Test
  void shouldFetchBroker() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final String userEmail = "owner@example.com";
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, userEmail)));

    final Broker broker = BrokerTestFactory.createBrokerWithOwner(userSid, "DEGIRO", null);
    brokerRepository.save(broker);

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid())
        .then()
        .statusCode(200)
        .body("sid", equalTo(broker.getSid().toString()))
        .body("name", equalTo("DEGIRO"))
        .body("status", equalTo(BrokerStatus.ACTIVE.name()))
        .body("currency", equalTo(Currency.EUR.name()))
        .body("users[0].sid", notNullValue())
        .body("users[0].email", equalTo(userEmail))
        .body("users[0].role", equalTo(BrokerMembershipRole.OWNER.name()));
  }

  @Test
  void shouldNotReturnBrokerThatUserIsNotPartOf() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker savedBroker = brokerRepository.save(BrokerTestFactory.createBrokerWithOwner(userSid, "DEGIRO", null));

    // Act
    final Optional<Broker> broker = brokerRepository.findBySidAndUserSid(savedBroker.getSid(), UUID.randomUUID());

    // Assert
    assertThat(broker).isEmpty();
  }

}
