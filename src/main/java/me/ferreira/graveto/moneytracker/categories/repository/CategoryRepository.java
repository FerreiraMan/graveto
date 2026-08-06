package me.ferreira.graveto.moneytracker.categories.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;

public interface CategoryRepository {

  Category save(Category category);

  List<Category> saveAll(List<Category> categoryList);

  Optional<Category> findBySid(UUID categorySid);

  Optional<Category> findBySidOrAccountSid(UUID categorySid, UUID accountSid);

  List<Category> findAll(FindAllCategoriesCommand command);

  List<Category> findAll();

  boolean existsByNameForAccountOrSystem(String name, UUID accountSid);

}
