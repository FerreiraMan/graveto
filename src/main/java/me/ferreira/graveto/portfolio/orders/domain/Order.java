package me.ferreira.graveto.portfolio.orders.domain;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

  private static final BigDecimal DEFAULT_FEE_AMOUNT = BigDecimal.ZERO;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_id_seq")
  @SequenceGenerator(name = "orders_id_seq", sequenceName = "orders_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "broker_id", nullable = false)
  private Broker broker;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @Column(name = "user_sid", nullable = false)
  private UUID userSid;

  @Column(name = "order_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderType orderType;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "price_per_unit", nullable = false)
  private BigDecimal pricePerUnit;

  @Column(nullable = false)
  private BigDecimal fees = DEFAULT_FEE_AMOUNT;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency;

  @Column(name = "executed_at", nullable = false)
  private LocalDateTime executedAt;

  @Column
  private String notes;

}
