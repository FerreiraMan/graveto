package me.ferreira.graveto.moneytracker.transactions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "transaction_groups")
public class TransactionGroup extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_groups_id_seq")
  @SequenceGenerator(name = "transaction_groups_id_seq", sequenceName = "transaction_groups_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @Column(nullable = false)
  private String name;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  public static TransactionGroup create(final String name, final UUID user) {

    final TransactionGroup group = new TransactionGroup();
    group.setSid(UUID.randomUUID());
    group.setName(name);
    group.setCreatedBy(user);
    return group;
  }

}
