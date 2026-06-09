package me.ferreira.graveto.identity.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserApi {

  Map<UUID, UserResponseDto> fetchUserDetailsByUserSids(Set<UUID> userSids);

  Optional<UserResponseDto> fetchUserByEmail(String email);

}
