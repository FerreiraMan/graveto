package me.ferreira.graveto.portfolio.brokers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.BrokerNotFoundException;
import me.ferreira.graveto.identity.api.UserApi;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.impl.BrokerServiceImpl;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class FetchBrokerServiceImplTest {

  @InjectMocks
  private BrokerServiceImpl brokerService;
  @Mock
  private UserApi userApi;
  @Mock
  private BrokerRepository brokerRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  void shouldFetchBroker() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final Broker broker = buildBroker(brokerSid, userSid);

    when(brokerRepository.findBySidAndUserSid(brokerSid, userSid)).thenReturn(Optional.of(broker));
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    // Act
    final BrokerDetails result = brokerService.fetchBroker(new FetchBrokerCommand(userSid, brokerSid));

    // Assert
    assertThat(result.sid()).isEqualTo(brokerSid);
    assertThat(result.name()).isEqualTo("DEGIRO");
    assertThat(result.users()).hasSize(1);
    assertThat(result.users().getFirst().email()).isEqualTo("user@example.com");
    assertThat(result.users().getFirst().role()).isEqualTo("OWNER");
  }

  @Test
  void shouldThrowWhenBrokerIsNotFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    when(brokerRepository.findBySidAndUserSid(brokerSid, userSid)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> brokerService.fetchBroker(new FetchBrokerCommand(userSid, brokerSid)))
        .isInstanceOf(BrokerNotFoundException.class)
        .hasMessage("Broker with SID [" + brokerSid + "] was not found or you do not have permission to view it.");
  }

  private static Broker buildBroker(final UUID sid, final UUID ownerUserSid) {
    final Broker broker = new Broker();
    broker.setSid(sid);
    broker.setName("DEGIRO");
    broker.setCurrency(Currency.EUR);
    broker.setStatus(BrokerStatus.ACTIVE);

    final BrokerMembership membership = new BrokerMembership();
    membership.setUserSid(ownerUserSid);
    membership.setRole(BrokerMembershipRole.OWNER);
    membership.setBroker(broker);
    broker.getMemberships().add(membership);

    return broker;
  }

}
