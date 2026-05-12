package me.ferreira.graveto.moneytracker.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
@Table(name = "account_membership")
public class AccountMembership extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_membership_id_seq")
  @SequenceGenerator(
      name = "account_membership_id_seq",
      sequenceName = "account_membership_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "user_sid")
  private UUID userSid;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private MembershipRole role;

  public static AccountMembership create(final UUID userSid, final MembershipRole role) {

    final AccountMembership am = new AccountMembership();

    am.setUserSid(userSid);
    am.setRole(role);

    return am;
  }

}
