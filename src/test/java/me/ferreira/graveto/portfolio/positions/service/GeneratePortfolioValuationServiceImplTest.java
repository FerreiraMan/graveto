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
import me.ferreira.graveto.portfolio.positions.service.command.FetchPortfolioOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.impl.PositionServiceImpl;
import me.ferreira.graveto.portfolio.positions.service.payload.PortfolioSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GeneratePortfolioValuationServiceImplTest {

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

    final FetchPortfolioOverviewCommand command = new FetchPortfolioOverviewCommand(userSid, brokerSid);

    // Act & Assert
    assertThatThrownBy(() -> positionService.generatePortfolioValuationOverview(command))
        .isInstanceOf(InsufficientPermissionsOnBrokerException.class);
  }

  @Test
  void shouldReturnZeroedSummaryWhenNoPositionsExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of());

    final FetchPortfolioOverviewCommand command = new FetchPortfolioOverviewCommand(userSid, brokerSid);

    // Act
    final PortfolioSummary result = positionService.generatePortfolioValuationOverview(command);

    // Assert
    assertThat(result.totalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.totalMarketValue()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.totalUnrealizedPnL()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.totalUnrealizedPnlPercent()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void shouldReturnZeroedSummaryWhenAllAssetsLackPrice() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    final Asset unpricedAsset = buildAsset(null);
    final Position position = buildPosition(broker, unpricedAsset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("727"));

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of(position));

    final FetchPortfolioOverviewCommand command = new FetchPortfolioOverviewCommand(userSid, brokerSid);

    // Act
    final PortfolioSummary result = positionService.generatePortfolioValuationOverview(command);

    // Assert
    assertThat(result.totalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.totalMarketValue()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void shouldCalculatePortfolioSummaryCorrectly() {
    // Scenario: Two positions:
    //   Position 1: 10 units, avgCost=50, invested=500, currentPrice=60 → marketValue=600, pnl=100
    //   Position 2: 10 units, avgCost=80, invested=800, currentPrice=100 → marketValue=1000, pnl=200
    //
    // Expected totals: invested=1300, marketValue=1600, pnl=300, pnl%=(300/1300)×100=23.08
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    final Asset iwda = buildAssetWithTicker("IWDA", new BigDecimal("60"));
    final Asset vwce = buildAssetWithTicker("VWCE", new BigDecimal("100"));

    final Position pos1 = buildPosition(broker, iwda,
        new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("500"));
    final Position pos2 = buildPosition(broker, vwce,
        new BigDecimal("10"), new BigDecimal("80"), new BigDecimal("800"));

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of(pos1, pos2));

    final FetchPortfolioOverviewCommand command = new FetchPortfolioOverviewCommand(userSid, brokerSid);

    // Act
    final PortfolioSummary result = positionService.generatePortfolioValuationOverview(command);

    // Assert
    assertThat(result.totalInvested()).isEqualByComparingTo(new BigDecimal("1300"));
    assertThat(result.totalMarketValue()).isEqualByComparingTo(new BigDecimal("1600"));
    assertThat(result.totalUnrealizedPnL()).isEqualByComparingTo(new BigDecimal("300"));
    assertThat(result.totalUnrealizedPnlPercent()).isEqualByComparingTo(new BigDecimal("23.08"));
  }

  @Test
  void shouldOnlySumPositionsWithValidPriceInSummary() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid, BrokerMembershipRole.OWNER);

    final Asset pricedAsset = buildAssetWithTicker("IWDA", new BigDecimal("89.45"));
    final Asset unpricedAsset = buildAssetWithTicker("FAKE", null);

    final Position pos1 = buildPosition(broker, pricedAsset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("727"));
    final Position pos2 = buildPosition(broker, unpricedAsset,
        new BigDecimal("5"), new BigDecimal("100"), new BigDecimal("502"));

    when(brokerService.fetchBrokerEntity(brokerSid)).thenReturn(broker);
    when(positionRepository.fetchAllByBrokerSidWithAsset(brokerSid)).thenReturn(List.of(pos1, pos2));

    final FetchPortfolioOverviewCommand command = new FetchPortfolioOverviewCommand(userSid, brokerSid);

    // Act
    final PortfolioSummary result = positionService.generatePortfolioValuationOverview(command);

    // Assert
    assertThat(result.totalInvested()).isEqualByComparingTo(new BigDecimal("727"));
    assertThat(result.totalMarketValue()).isEqualByComparingTo(new BigDecimal("894.50"));
    assertThat(result.totalUnrealizedPnL()).isEqualByComparingTo(new BigDecimal("167.50"));
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
