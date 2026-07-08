package me.ferreira.graveto.portfolio.brokers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.brokers.web.dto.response.BrokerSummaryResponseDto;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import me.ferreira.graveto.portfolio.utils.BrokerTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAllBrokersIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;

  @Test
  void shouldFetchAllBrokers() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = BrokerTestFactory.createBrokerWithOwner(userSid, "DEGIRO", null);
    final Broker secondBroker = BrokerTestFactory.createBrokerWithOwner(userSid, "TRADING212", null);
    final List<Broker> brokers = List.of(broker, secondBroker);

    final List<Broker> savedBrokers = brokerRepository.saveAll(brokers);

    // Act
    final List<BrokerSummaryResponseDto> responseDto = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .jsonPath().getList(".", BrokerSummaryResponseDto.class);

    // Assert
    assertThat(responseDto)
        .hasSize(2)
        .extracting(
            BrokerSummaryResponseDto::sid,
            BrokerSummaryResponseDto::name
        )
        .containsExactlyInAnyOrder(
            tuple(savedBrokers.get(0).getSid(), savedBrokers.get(0).getName()),
            tuple(savedBrokers.get(1).getSid(), savedBrokers.get(1).getName())
        );
  }

  @Test
  void shouldReturnEmptyListIfUserHasNoBrokers() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = BrokerTestFactory.createBrokerWithOwner(userSid, "DEGIRO", null);
    final Broker secondBroker = BrokerTestFactory.createBrokerWithOwner(userSid, "TRADING212", null);
    final List<Broker> brokers = List.of(broker, secondBroker);

    brokerRepository.saveAll(brokers);

    // Act
    final List<BrokerSummaryResponseDto> responseDto = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + UUID.randomUUID())
        .when()
        .get("/brokers")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .jsonPath().getList(".", BrokerSummaryResponseDto.class);

    // Assert
    assertThat(responseDto)
        .isEmpty();
  }

}
