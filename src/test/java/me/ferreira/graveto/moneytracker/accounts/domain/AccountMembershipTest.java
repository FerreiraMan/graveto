package me.ferreira.graveto.moneytracker.accounts.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
