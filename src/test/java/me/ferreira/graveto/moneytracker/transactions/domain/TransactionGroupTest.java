package me.ferreira.graveto.moneytracker.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TransactionGroupTest {

  @Test
  void shouldCreateTransactionGroupWithGeneratedSid() {
    // Arrange
    final String name = "Algarve 2026";
    final UUID createdBy = UUID.randomUUID();

    // Act
    final TransactionGroup group = TransactionGroup.create(name, createdBy);

    // Assert
    assertThat(group.getSid()).isNotNull();
    assertThat(group.getName()).isEqualTo(name);
    assertThat(group.getCreatedBy()).isEqualTo(createdBy);
  }

  @Test
  void shouldGenerateDifferentSidForEachGroup() {
    // Arrange
    final UUID createdBy = UUID.randomUUID();

    // Act
    final TransactionGroup first = TransactionGroup.create("Algarve 2026", createdBy);
    final TransactionGroup second = TransactionGroup.create("Algarve 2026", createdBy);

    // Assert
    assertThat(first.getSid()).isNotEqualTo(second.getSid());
  }

}
