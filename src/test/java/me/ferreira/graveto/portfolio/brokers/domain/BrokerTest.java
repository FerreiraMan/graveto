package me.ferreira.graveto.portfolio.brokers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.common.web.exception.portfolio.UserAlreadyBrokerMemberException;
import org.junit.jupiter.api.Test;

public class BrokerTest {

  @Test
  void shouldCreateActiveBrokerWithGeneratedSid() {
    // Arrange
    final String name = "DEGIRO";
    final UUID accountSid = UUID.randomUUID();
    final Currency currency = Currency.EUR;

    // Act
    final Broker broker = Broker.create(name, accountSid, currency);

    // Assert
    assertThat(broker.getSid()).isNotNull();
    assertThat(broker.getName()).isEqualTo(name);
    assertThat(broker.getAccountSid()).isEqualTo(accountSid);
    assertThat(broker.getCurrency()).isEqualTo(currency);
    assertThat(broker.getStatus()).isEqualTo(BrokerStatus.ACTIVE);
    assertThat(broker.getMemberships()).isEmpty();
  }

  @Test
  void shouldCreateActiveBrokerWithoutAccountSid() {
    // Act
    final Broker broker = Broker.create("DEGIRO", null, Currency.EUR);

    // Assert
    assertThat(broker.getSid()).isNotNull();
    assertThat(broker.getAccountSid()).isNull();
  }

  @Test
  void shouldAddMembershipToBroker() {
    // Arrange
    final Broker broker = Broker.create("DEGIRO", null, Currency.EUR);
    final BrokerMembership membership = BrokerMembership.create(UUID.randomUUID(), BrokerMembershipRole.OWNER);

    // Act
    broker.addMembership(membership);

    // Assert
    assertThat(broker.getMemberships()).hasSize(1);
    assertThat(broker.getMemberships().getFirst().getRole()).isEqualTo(BrokerMembershipRole.OWNER);
    assertThat(membership.getBroker()).isEqualTo(broker);
  }

  @Test
  void shouldThrowWhenAddingDuplicateMemberToBroker() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = Broker.create("DEGIRO", null, Currency.EUR);
    broker.addMembership(BrokerMembership.create(userSid, BrokerMembershipRole.OWNER));

    // Act & Assert
    assertThatThrownBy(() -> broker.addMembership(BrokerMembership.create(userSid, BrokerMembershipRole.VIEWER)))
        .isInstanceOf(UserAlreadyBrokerMemberException.class);
  }

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = Broker.create("DEGIRO", null, Currency.EUR);
    broker.addMembership(BrokerMembership.create(userSid, BrokerMembershipRole.VIEWER));

    // Act & Assert
    assertThatThrownBy(() ->
        broker.validateUserPermission(userSid, BrokerMembershipRole::canCreateOrders, "create orders"))
        .isInstanceOf(InsufficientPermissionsOnBrokerException.class)
        .hasMessage("User does not have the required role to create orders for this broker account.");
  }

  @Test
  void shouldThrowWhenUserIsNotMemberOfBroker() {
    // Arrange
    final Broker broker = Broker.create("DEGIRO", null, Currency.EUR);

    // Act & Assert
    assertThatThrownBy(() ->
        broker.validateUserPermission(UUID.randomUUID(), BrokerMembershipRole::canCreateOrders, "create orders"))
        .isInstanceOf(InsufficientPermissionsOnBrokerException.class);
  }

}
