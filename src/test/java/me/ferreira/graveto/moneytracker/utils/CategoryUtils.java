package me.ferreira.graveto.moneytracker.utils;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public class CategoryUtils {

  public static Category createInitialBalanceCategory() {

    final Category category = new Category();
    category.setSid(SystemCategory.INITIAL_BALANCE.getSid());
    category.setName("INITIAL_BALANCE");
    return category;
  }

  public static Category createCategory(final String name, final UUID accountSid, final Category parent,
                                        final boolean isInternal, final TransactionType type) {

    final Category category = new Category();
    category.setDisplayName(name);
    category.setParent(parent);
    category.setAccountSid(accountSid);
    category.setSid(UUID.randomUUID());
    category.setName(name);
    category.setInternal(isInternal);
    category.setTransactionType(type);
    return category;
  }

}
