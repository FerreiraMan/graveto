package me.ferreira.graveto.identity.repository.impl;

import java.util.Optional;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserJpaRepository;
import me.ferreira.graveto.identity.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository repository;

  @Override
  public User save(User user) {
    return repository.save(user);
  }

  @Override
  public Optional<User> fetchUserCredentials(final String email) {
    return repository.findByEmail(email);
  }

}
