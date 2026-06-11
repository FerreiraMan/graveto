package me.ferreira.graveto.moneytracker.categories.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import org.junit.jupiter.api.Test;

public class CategoryTest {

  @Test
  void shouldCreateAccountSpecificCategoryWithGeneratedSid() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final UUID parentCategorySid = UUID.randomUUID();
    final String name = "Videogames";
    final Category parent = new Category();
    parent.setSid(parentCategorySid);

    // Act
    final Category category = Category.create(name, name, accountSid, parent, TransactionType.EXPENSE);

    // Assert
    assertThat(category.getAccountSid()).isEqualTo(accountSid);
    assertThat(category.getName()).isEqualTo(name);
    assertThat(category.getParent().getSid()).isEqualTo(parent.getSid());
    assertThat(category.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
  }

}
