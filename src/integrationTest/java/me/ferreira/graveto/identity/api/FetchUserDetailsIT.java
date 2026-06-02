package me.ferreira.graveto.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.identity.config.IdentityBaseIntegrationTest;
import me.ferreira.graveto.identity.domain.Role;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.utils.UserTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/identity/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchUserDetailsIT extends IdentityBaseIntegrationTest {

  @Autowired
  private UserApi userApi;
  @Autowired
  private UserRepository userRepository;

  @Test
  void shouldReturnUsers() {
    // Arrange
    final String firstUserEmail = "first@email.com";
    final String secondUserEmail = "second@email.com";
    final User firstUser = UserTestFactory.createUser(firstUserEmail, "password", Role.USER);
    final User secondUser = UserTestFactory.createUser(secondUserEmail, "password", Role.USER);
    userRepository.saveAll(List.of(firstUser, secondUser));

    // Act
    final Map<UUID, UserResponseDto> users =
        userApi.fetchUserDetailsByUserSids(Set.of(firstUser.getSid(), secondUser.getSid()));

    // Assert
    assertThat(users)
        .isNotNull()
        .hasSize(2)
        .containsOnlyKeys(firstUser.getSid(), secondUser.getSid());

    final UserResponseDto firstDto = users.get(firstUser.getSid());
    assertThat(firstDto).isNotNull();
    assertThat(firstDto.email()).isEqualTo(firstUserEmail);

    final UserResponseDto secondDto = users.get(secondUser.getSid());
    assertThat(secondDto).isNotNull();
    assertThat(secondDto.email()).isEqualTo(secondUserEmail);
  }

}
