package me.ferreira.graveto.moneytracker.categories.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;

public interface CategoryRepository {

  Category save(Category category);

  List<Category> saveAll(List<Category> categoryList);

  Optional<Category> findBySid(UUID categorySid);

  Optional<Category> findBySidOrAccountSid(UUID categorySid, UUID accountSid);

  List<Category> findByAccountSidIsNull();

  List<Category> findAll();

  List<Category> findAllByAccountSid(UUID accountSid);

  boolean existsByNameForAccountOrSystem(String name, UUID accountSid);

}
