package me.ferreira.graveto.identity.utils;

import java.util.UUID;
import me.ferreira.graveto.identity.domain.Role;
import me.ferreira.graveto.identity.domain.User;

public class UserTestFactory {

  public static User createUser(final String email, final String password, final Role role) {

    final User user = new User();
    user.setSid(UUID.randomUUID());
    user.setEmail(email);
    user.setPassword(password);
    user.setRole(role);
    return user;
  }

}
