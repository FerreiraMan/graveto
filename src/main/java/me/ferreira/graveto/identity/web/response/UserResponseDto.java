package me.ferreira.graveto.identity.web.response;

import java.util.UUID;
import me.ferreira.graveto.identity.domain.User;

public record UserResponseDto(
    UUID sid,
    String email
) {

  public static UserResponseDto from(final User user) {
    return new UserResponseDto(
        user.getSid(),
        user.getEmail()
    );
  }

}
