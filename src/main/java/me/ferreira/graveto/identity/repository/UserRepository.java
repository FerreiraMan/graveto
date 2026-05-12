package me.ferreira.graveto.identity.repository;

import java.util.Optional;
import me.ferreira.graveto.identity.domain.User;

public interface UserRepository {

  User save(User user);

  Optional<User> fetchUserCredentials(String email);

}
