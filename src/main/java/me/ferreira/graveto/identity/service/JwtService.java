package me.ferreira.graveto.identity.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.identity.TokenAuthenticationException;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.service.payload.JwtPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final Algorithm signingAlgorithm;
  private final long expirationMillis;
  private final List<String> validIssuers;

  public JwtService(
      @Value("${jwt.signing-secret}") final String signingSecret,
      @Value("${jwt.expiration-ms}") final long expirationMillis,
      @Value("${jwt.issuer}") final List<String> validIssuers) {

    this.signingAlgorithm = Algorithm.HMAC256(signingSecret);
    this.expirationMillis = expirationMillis;
    this.validIssuers = validIssuers;
  }

  public String createJwtToken(final AuthUser authUser) {

    final long nowMillis = System.currentTimeMillis();
    final Date now = new Date(nowMillis);
    final long expMillis = nowMillis + expirationMillis;
    final Date exp = new Date(expMillis);
    final String primaryIssuer = validIssuers.get(0);

    return JWT.create()
        .withClaim("role", authUser.role().name())
        .withIssuer(primaryIssuer)
        .withSubject(authUser.sid().toString())
        .withIssuedAt(now)
        .withExpiresAt(exp)
        .sign(signingAlgorithm);
  }

  public JwtPayload verifyJwtToken(final String token) {

    try {
      final JWTVerifier verifier = JWT.require(signingAlgorithm)
          .withIssuer(validIssuers.toArray(new String[0]))
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
