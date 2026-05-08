package me.ferreira.graveto.identity.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

    @Test
    void shouldCreateUser() {
        // Arrange
        final String email = "email@test.com";
        final String password = "password";

        // Act
        final User user = User.create(email, password);

        // Assert
        assertThat(user.getSid()).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

}
