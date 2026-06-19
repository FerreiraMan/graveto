package me.ferreira.graveto.portfolio.brokers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.domain.event.BrokerCreatedEvent;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.impl.BrokerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class CreateBrokerServiceImplTest {

  @InjectMocks
  private BrokerServiceImpl brokerService;
  @Mock
  private BrokerRepository brokerRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  void shouldCreateBroker() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final CreateBrokerCommand command = new CreateBrokerCommand(userSid, accountSid, "DEGIRO", Currency.EUR);

    when(brokerRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Broker result = brokerService.createBroker(command);

    // Assert
    final ArgumentCaptor<Broker> brokerCaptor = ArgumentCaptor.forClass(Broker.class);
    verify(brokerRepository).save(brokerCaptor.capture());

    final Broker savedBroker = brokerCaptor.getValue();
    assertThat(savedBroker.getSid()).isNotNull();
    assertThat(savedBroker.getName()).isEqualTo("DEGIRO");
    assertThat(savedBroker.getAccountSid()).isEqualTo(accountSid);
    assertThat(savedBroker.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(savedBroker.getStatus()).isEqualTo(BrokerStatus.ACTIVE);
    assertThat(savedBroker.getMemberships()).hasSize(1);
    assertThat(savedBroker.getMemberships().getFirst().getUserSid()).isEqualTo(userSid);
    assertThat(savedBroker.getMemberships().getFirst().getRole()).isEqualTo(BrokerMembershipRole.OWNER);

    assertThat(result).isEqualTo(savedBroker);

    final ArgumentCaptor<BrokerCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BrokerCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().broker()).isEqualTo(savedBroker);
  }

  @Test
  void shouldCreateBrokerWithoutAccountSid() {
    // Arrange
    final CreateBrokerCommand command = new CreateBrokerCommand(UUID.randomUUID(), null, "Trading 212", Currency.EUR);

    when(brokerRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    brokerService.createBroker(command);

    // Assert
    final ArgumentCaptor<Broker> brokerCaptor = ArgumentCaptor.forClass(Broker.class);
    verify(brokerRepository).save(brokerCaptor.capture());
    assertThat(brokerCaptor.getValue().getAccountSid()).isNull();
  }

}
