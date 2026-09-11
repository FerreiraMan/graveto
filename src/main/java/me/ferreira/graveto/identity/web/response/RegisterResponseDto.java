package me.ferreira.graveto.identity.web.response;

import java.util.UUID;
import me.ferreira.graveto.identity.domain.User;

public record RegisterResponseDto(
    UUID sid,
    String email
) {

  public static RegisterResponseDto from(final User user) {
    return new RegisterResponseDto(
        user.getSid(),
        user.getEmail()
    );
  }

}
