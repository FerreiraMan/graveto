package me.ferreira.graveto.moneytracker.categories.service;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchCategoryCommand;

public interface CategoryService {

  Category fetchInternalCategory(UUID systemCategorySid);

  Category fetchCategory(FetchCategoryCommand command);

  List<Category> fetchAllCategories(FetchAllCategoriesCommand command);

  Category createCategory(CreateCategoryCommand command);

}
