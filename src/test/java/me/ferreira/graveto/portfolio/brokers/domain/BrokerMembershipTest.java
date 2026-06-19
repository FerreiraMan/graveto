package me.ferreira.graveto.portfolio.brokers.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class BrokerMembershipTest {

  @Test
  void shouldCreateMembershipWithUserSidAndRole() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    // Act
    final BrokerMembership membership = BrokerMembership.create(userSid, BrokerMembershipRole.OWNER);

    // Assert
    assertThat(membership.getUserSid()).isEqualTo(userSid);
    assertThat(membership.getRole()).isEqualTo(BrokerMembershipRole.OWNER);
    assertThat(membership.getBroker()).isNull();
  }

  @Test
  void shouldAllowOwnerToCreateOrders() {
    assertThat(BrokerMembershipRole.OWNER.canCreateOrders()).isTrue();
  }

  @Test
  void shouldNotAllowViewerToCreateOrders() {
    assertThat(BrokerMembershipRole.VIEWER.canCreateOrders()).isFalse();
  }

}
