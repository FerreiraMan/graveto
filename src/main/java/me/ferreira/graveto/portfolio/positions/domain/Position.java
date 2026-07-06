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
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
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

  public static Position create(final OrderType orderType, final Broker broker, final Asset asset,
                                final BigDecimal quantity, final BigDecimal pricePerUnit, final BigDecimal fees) {

    if (!orderType.isBuyOrder()) {
      throw new IllegalStateException(
          "Cannot create a position from a SELL order — position must exist before selling.");
    }

    final Position position = new Position();
    position.setSid(UUID.randomUUID());
    position.setBroker(broker);
    position.setAsset(asset);
    position.setQuantity(quantity);
    position.setAverageCost(pricePerUnit);
    position.setTotalInvested(quantity.multiply(pricePerUnit).add(fees));
    return position;
  }

  public void recalculateAverageCost(final OrderType orderType, final BigDecimal quantityFromOrder,
                                     final BigDecimal pricePerUnit) {

    if (!orderType.isBuyOrder()) {
      return;
    }
    final BigDecimal oldSum = this.averageCost.multiply(this.quantity);
    final BigDecimal newSum = oldSum.add(quantityFromOrder.multiply(pricePerUnit));
    final BigDecimal newCount = this.quantity.add(quantityFromOrder);
    this.averageCost = newSum.divide(newCount, 8, RoundingMode.HALF_UP);
  }

  public void updateQuantity(final OrderType orderType, final BigDecimal quantityFromOrder) {

    this.quantity =
        this.quantity.add(quantityFromOrder.multiply(BigDecimal.valueOf(orderType.getQuantityMultiplier())));
  }

  public void updateTotalInvested(final OrderType orderType, final BigDecimal orderQuantity,
                                  final BigDecimal pricePerUnit, final BigDecimal fees) {

    if (!orderType.isBuyOrder()) {
      return;
    }
    this.totalInvested = this.totalInvested.add(orderQuantity.multiply(pricePerUnit).add(fees));
  }

  public void reverseOrderImpact(final OrderType orderType, final BigDecimal quantityFromOldOrder,
                                 final BigDecimal priceFromOldOrder, final BigDecimal feesFromOldOrder) {

    if (orderType.isBuyOrder()) {

      final BigDecimal currentSum = this.averageCost.multiply(this.quantity);
      final BigDecimal reversedSum = currentSum.subtract(quantityFromOldOrder.multiply(priceFromOldOrder));
      final BigDecimal reversedQuantity = this.quantity.subtract(quantityFromOldOrder);
      if (reversedQuantity.compareTo(BigDecimal.ZERO) == 0) {
        this.averageCost = BigDecimal.ZERO;
      } else {
        this.averageCost = reversedSum.divide(reversedQuantity, 8, RoundingMode.HALF_UP);
      }
      this.totalInvested =
          this.totalInvested.subtract(quantityFromOldOrder.multiply(priceFromOldOrder).add(feesFromOldOrder));
    }

    this.quantity = this.quantity.subtract(
        quantityFromOldOrder.multiply(BigDecimal.valueOf(orderType.getQuantityMultiplier())));
  }

}
