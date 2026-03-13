package me.ferreira.graveto.moneytracker.categories.repository;

import me.ferreira.graveto.moneytracker.categories.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findBySid(UUID categorySid);

    List<Category> findByUserSidIsNull();

    List<Category> findByUserSid(UUID userSid);

}
