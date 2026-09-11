package me.ferreira.graveto.identity.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.identity.TokenAuthenticationException;
import me.ferreira.graveto.identity.config.properties.JwtProperties;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.payload.JwtPayload;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

  private final JwtProperties jwtProperties;
  private final Algorithm signingAlgorithm;

  public JwtServiceImpl(final JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.signingAlgorithm = Algorithm.HMAC256(jwtProperties.signingSecret());
  }

  @Override
  public String createJwtToken(final AuthUser authUser) {

    final long nowMillis = System.currentTimeMillis();
    final Date now = new Date(nowMillis);
    final long expMillis = nowMillis + jwtProperties.expiration();
    final Date exp = new Date(expMillis);
    final String primaryIssuer = jwtProperties.issuers().get(0);

    return JWT.create()
        .withClaim("role", authUser.role().name())
        .withIssuer(primaryIssuer)
        .withSubject(authUser.sid().toString())
        .withIssuedAt(now)
        .withExpiresAt(exp)
        .sign(signingAlgorithm);
  }

  @Override
  public JwtPayload verifyJwtToken(final String token) {

    try {
      final JWTVerifier verifier = JWT.require(signingAlgorithm)
          .withIssuer(jwtProperties.issuers().toArray(new String[0]))
          .build();
      final DecodedJWT decodedJwt = verifier.verify(token);

      final String role = decodedJwt.getClaim("role").asString();
      final String subjectSid = decodedJwt.getSubject();

      return new JwtPayload(UUID.fromString(subjectSid), role);
    } catch (final JWTVerificationException exception) {
      throw new TokenAuthenticationException("Invalid JWT token.");
    }
  }

}
