package me.ferreira.graveto.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.identity.domain.User;

public interface UserRepository {

  User save(User user);

  List<User> saveAll(List<User> users);

  Optional<User> fetchUserCredentials(String email);

  List<User> fetchListOfUsers(Set<UUID> uuidList);

}
