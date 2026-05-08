package me.ferreira.graveto.identity.repository;

import me.ferreira.graveto.identity.domain.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> fetchUserCredentials(String email);

}
