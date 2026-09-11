package me.ferreira.graveto.identity.service;

import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.service.payload.JwtPayload;

public interface JwtService {

  String createJwtToken(AuthUser authUser);

  JwtPayload verifyJwtToken(String token);

}
