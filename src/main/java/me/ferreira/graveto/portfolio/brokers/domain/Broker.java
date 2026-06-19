package me.ferreira.graveto.portfolio.brokers.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.common.web.exception.portfolio.UserAlreadyBrokerMemberException;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "brokers")
public class Broker extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "broker_id_seq")
  @SequenceGenerator(name = "broker_id_seq", sequenceName = "broker_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @Column(name = "account_sid")
  private UUID accountSid;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private BrokerStatus status;

  @OneToMany(mappedBy = "broker", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private List<BrokerMembership> memberships = new ArrayList<>();

  public static Broker create(final String name, final UUID accountSid, final Currency currency) {

    final Broker broker = new Broker();
    broker.setSid(UUID.randomUUID());
    broker.setAccountSid(accountSid);
    broker.setName(name);
    broker.setCurrency(currency);
    broker.setStatus(BrokerStatus.ACTIVE);
    return broker;
  }

  public void addMembership(final BrokerMembership membership) {

    final boolean alreadyExists = this.memberships.stream()
        .anyMatch(m -> m.getUserSid().equals(membership.getUserSid()));

    if (alreadyExists) {
      throw new UserAlreadyBrokerMemberException(membership.getUserSid());
    }

    memberships.add(membership);
    membership.setBroker(this);
  }

  public void validateUserPermission(final UUID userSid,
                                     final Predicate<BrokerMembershipRole> permissionCheck,
                                     final String actionName) {

    final boolean isAuthorized = this.memberships.stream()
        .filter(m -> userSid.equals(m.getUserSid()))
        .findFirst()
        .map(BrokerMembership::getRole)
        .filter(permissionCheck)
        .isPresent();

    if (!isAuthorized) {
      throw new InsufficientPermissionsOnBrokerException(actionName);
    }
  }

}
