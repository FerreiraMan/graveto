package me.ferreira.graveto.moneytracker.categories.service;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    Category getInitialBalanceCategory();

    List<Category> fetchAllCategories(UUID userSid);

    Category createCategory(CreateCategoryCommand command);

}
