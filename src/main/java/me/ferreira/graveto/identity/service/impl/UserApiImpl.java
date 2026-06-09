package me.ferreira.graveto.identity.service.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.identity.api.UserApi;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserApiImpl implements UserApi {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, UserResponseDto> fetchUserDetailsByUserSids(final Set<UUID> userSids) {

    return userRepository.fetchListOfUsers(userSids).stream()
        .collect(Collectors.toMap(
            User::getSid,
            user -> new UserResponseDto(user.getSid(), user.getEmail())
        ));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserResponseDto> fetchUserByEmail(final String email) {

    return userRepository.fetchUserCredentials(email)
        .map(u -> new UserResponseDto(u.getSid(), u.getEmail()));
  }

}
