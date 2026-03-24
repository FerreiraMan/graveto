package me.ferreira.graveto.moneytracker.utils;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;

import java.util.UUID;

public class CategoryUtils {

    public static Category createInitialBalanceCategory() {

        final Category category = new Category();
        category.setSid(SystemCategory.INITIAL_BALANCE.getSid());
        category.setName("INITIAL_BALANCE");
        return category;
    }

    public static Category createCategory(final String name, final UUID userSid, final Category parent, final boolean isInternal) {

        final Category category = new Category();
        category.setDisplayName(name);
        category.setParent(parent);
        category.setUserSid(userSid);
        category.setSid(UUID.randomUUID());
        category.setName(name);
        category.setInternal(isInternal);
        return category;
    }

}
