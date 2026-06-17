package me.ferreira.graveto.portfolio.positions.domain;

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
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Table(name = "positions")
public class Position extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "positions_id_seq")
  @SequenceGenerator(name = "positions_id_seq", sequenceName = "positions_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "broker_id", nullable = false)
  private Broker broker;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "average_cost", nullable = false)
  private BigDecimal averageCost;

  @Column(name = "total_invested", nullable = false)
  private BigDecimal totalInvested;

}
