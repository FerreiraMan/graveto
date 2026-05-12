package me.ferreira.graveto.moneytracker.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AccountMembershipTest {

  @Test
  void shouldCreateActiveAccountWithGeneratedSid() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final MembershipRole role = MembershipRole.OWNER;

    // Act
    final AccountMembership accountMembership = AccountMembership.create(userSid, role);

    // Assert
    assertThat(accountMembership.getUserSid()).isEqualTo(userSid);
    assertThat(accountMembership.getRole()).isEqualTo(role);
  }

}
