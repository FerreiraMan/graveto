package me.ferreira.graveto.portfolio.positions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.repository.PositionRepository;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPositionOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.impl.PositionServiceImpl;
import me.ferreira.graveto.portfolio.positions.service.payload.PositionValuation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GeneratePositionValuationServiceImplTest {

  @InjectMocks
  private PositionServiceImpl positionService;
  @Mock
  private PositionRepository positionRepository;
  @Mock
  private BrokerService brokerService;

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker brokerNoMembership = buildBroker(brokerSid, UUID.randomUUID(), BrokerMembershipRole.OWNER);
    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(brokerNoMembership);

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    // Act & Assert
    assertThatThrownBy(() -> positionService.generatePositionValuationOverview(command))
        .isInstanceOf(InsufficientPermissionsOnBrokerException.class);
  }

  @Test
  void shouldReturnEmptyListWhenNoPositionsExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of());

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    // Act
    final List<PositionValuation> result = positionService.generatePositionValuationOverview(command);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void shouldCalculateValuationCorrectly() {
    // Scenario: Position has 10 units, avgCost=72.50, totalInvested=727.
    //   Asset currentPrice=89.45.
    //   Expected: marketValue=894.50, unrealizedPnL=167.50, pnlPercent=23.04%
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    final Asset asset = buildAsset(new BigDecimal("89.45"));
    final Position position = buildPosition(broker, asset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("727"));

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of(position));

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    // Act
    final List<PositionValuation> result = positionService.generatePositionValuationOverview(command);

    // Assert
    assertThat(result).hasSize(1);
    final PositionValuation valuation = result.getFirst();
    assertThat(valuation.assetSid()).isEqualTo(asset.getSid());
    assertThat(valuation.ticker()).isEqualTo("IWDA");
    assertThat(valuation.quantity()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(valuation.averageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(valuation.totalInvested()).isEqualByComparingTo(new BigDecimal("727"));
    assertThat(valuation.currentPrice()).isEqualByComparingTo(new BigDecimal("89.45"));
    assertThat(valuation.marketValue()).isEqualByComparingTo(new BigDecimal("894.50"));
    assertThat(valuation.unrealizedPnL()).isEqualByComparingTo(new BigDecimal("167.50"));
    assertThat(valuation.unrealizedPnlPercent()).isEqualByComparingTo(new BigDecimal("23.04"));
  }

  @Test
  void shouldSkipPositionsWhereAssetHasNoCurrentPrice() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    final Asset pricedAsset = buildAsset(new BigDecimal("89.45"));
    final Asset unpricedAsset = buildAsset(null);

    final Position positionWithPrice = buildPosition(broker, pricedAsset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("727"));
    final Position positionWithoutPrice = buildPosition(broker, unpricedAsset,
        new BigDecimal("5"), new BigDecimal("100"), new BigDecimal("502"));

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid))
        .thenReturn(List.of(positionWithPrice, positionWithoutPrice));

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    // Act
    final List<PositionValuation> result = positionService.generatePositionValuationOverview(command);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().ticker()).isEqualTo("IWDA");
  }

  @Test
  void shouldReturnMultipleValuations() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    final Asset iwda = buildAssetWithTicker("IWDA", new BigDecimal("89.45"));
    final Asset vwce = buildAssetWithTicker("VWCE", new BigDecimal("105.20"));

    final Position pos1 = buildPosition(broker, iwda,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("727"));
    final Position pos2 = buildPosition(broker, vwce,
        new BigDecimal("5"), new BigDecimal("95"), new BigDecimal("477"));

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of(pos1, pos2));

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    // Act
    final List<PositionValuation> result = positionService.generatePositionValuationOverview(command);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(PositionValuation::ticker).containsExactly("IWDA", "VWCE");
  }

  @Test
  void shouldAllowViewerToRequestValuationOverview() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.VIEWER);

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of());

    final FetchPositionOverviewCommand command = new FetchPositionOverviewCommand(userSid, brokerSid);

    // Act
    final List<PositionValuation> result = positionService.generatePositionValuationOverview(command);

    // Assert
    assertThat(result).isEmpty();
  }

  private static Broker buildBroker(final UUID sid, final UUID ownerUserSid, final BrokerMembershipRole role) {
    final Broker broker = new Broker();
    broker.setSid(sid);
    broker.setName("DEGIRO");
    broker.setCurrency(Currency.EUR);
    broker.setStatus(BrokerStatus.ACTIVE);

    final BrokerMembership membership = new BrokerMembership();
    membership.setUserSid(ownerUserSid);
    membership.setRole(role);
    membership.setBroker(broker);
    broker.getMemberships().add(membership);

    return broker;
  }

  private static Asset buildAsset(final BigDecimal currentPrice) {
    return buildAssetWithTicker("IWDA", currentPrice);
  }

  private static Asset buildAssetWithTicker(final String ticker, final BigDecimal currentPrice) {
    final Asset asset = new Asset();
    asset.setSid(UUID.randomUUID());
    asset.setTicker(ticker);
    asset.setName("Test " + ticker);
    asset.setAssetType(AssetType.ETF);
    asset.setCurrency(Currency.EUR);
    asset.setCurrentPrice(currentPrice);
    return asset;
  }

  private static Position buildPosition(final Broker broker, final Asset asset,
                                        final BigDecimal quantity, final BigDecimal avgCost,
                                        final BigDecimal totalInvested) {
    final Position position = new Position();
    position.setSid(UUID.randomUUID());
    position.setBroker(broker);
    position.setAsset(asset);
    position.setQuantity(quantity);
    position.setAverageCost(avgCost);
    position.setTotalInvested(totalInvested);
    return position;
  }

}
