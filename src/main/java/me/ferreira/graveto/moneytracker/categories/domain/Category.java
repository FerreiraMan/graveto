package me.ferreira.graveto.moneytracker.categories.domain;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_id_seq")
  @SequenceGenerator(name = "categories_id_seq", sequenceName = "categories_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @Column(nullable = false)
  private String name;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "account_sid")
  private UUID accountSid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Category parent;

  @Column(name = "is_internal", nullable = false)
  private boolean isInternal = false;

  @Column(name = "transaction_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private TransactionType transactionType;

  @OneToMany(mappedBy = "parent")
  private List<Category> children = new ArrayList<>();

  public static Category create(
      final String name,
      final String displayName,
      final UUID accountSid,
      final Category parent,
      final TransactionType transactionType) {

    final Category cat = new Category();

    cat.setSid(UUID.randomUUID());

    cat.setName(name);
    cat.setDisplayName(displayName);
    cat.setParent(parent);
    cat.setAccountSid(accountSid);
    cat.setTransactionType(transactionType);

    return cat;
  }

}
