package me.ferreira.graveto.moneytracker.utils;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public class CategoryTestFactory {

  public static Category createCategory(
      final String name,
      final UUID userSid,
      final Category parent,
      final boolean isInternal) {

    final Category category = new Category();
    category.setSid(UUID.randomUUID());
    category.setUserSid(userSid);
    category.setInternal(isInternal);
    category.setParent(parent);
    category.setName(name);
    category.setDisplayName(name);
    category.setTransactionType(TransactionType.EXPENSE);
    return category;
  }

}
