package me.ferreira.graveto.moneytracker.categories.repository;

import me.ferreira.graveto.moneytracker.categories.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Category save(Category category);

    List<Category> saveAll(List<Category> categoryList);

    Optional<Category> findBySid(UUID categorySid);

    List<Category> findByUserSidIsNull();

    List<Category> findAllByUserSid(UUID userSid);

    boolean existsByNameForUserOrSystem(String name, UUID userSid);

}
