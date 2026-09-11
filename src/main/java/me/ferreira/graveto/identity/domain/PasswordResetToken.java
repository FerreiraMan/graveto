package me.ferreira.graveto.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.jpa.BaseEntity;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {

  private static final String LOCAL_ZONE_ID = "Europe/Lisbon";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_reset_tokens_id_seq")
  @SequenceGenerator(name = "password_reset_tokens_id_seq", sequenceName = "password_reset_tokens_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private LocalDateTime expiresAt;

  public static PasswordResetToken create(final User user, final String tokenHash, final Long expirationTime) {

    final PasswordResetToken passwordResetToken = new PasswordResetToken();
    passwordResetToken.setSid(UUID.randomUUID());
    passwordResetToken.setUser(user);
    passwordResetToken.setTokenHash(tokenHash);

    final LocalDateTime now = LocalDateTime.now(ZoneId.of(LOCAL_ZONE_ID));
    passwordResetToken.setExpiresAt(now.plus(expirationTime, ChronoUnit.MILLIS));

    return passwordResetToken;
  }

}
