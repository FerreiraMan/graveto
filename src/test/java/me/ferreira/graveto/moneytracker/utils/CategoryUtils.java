package me.ferreira.graveto.moneytracker.utils;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;

public class CategoryUtils {

    public static Category createInitialBalanceCategory() {

        final Category category = new Category();
        category.setSid(SystemCategory.INITIAL_BALANCE);
        category.setName("INITIAL_BALANCE");
        return category;
    }

}
