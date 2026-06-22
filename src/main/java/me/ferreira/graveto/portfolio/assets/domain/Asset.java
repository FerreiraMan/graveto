package me.ferreira.graveto.portfolio.assets.domain;

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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "assets")
public class Asset extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asset_id_seq")
  @SequenceGenerator(name = "asset_id_seq", sequenceName = "asset_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @Column(nullable = false)
  private String ticker;

  @Column(nullable = false)
  private String name;

  @Column(name = "asset_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AssetType assetType;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stock_exchange_id", nullable = false)
  private StockExchange stockExchange;

  @Column
  private String isin;

  @Column(name = "current_price")
  private BigDecimal currentPrice;

}
