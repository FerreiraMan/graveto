package me.ferreira.graveto.moneytracker.categories.repository.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryJpaRepository;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository repository;

    @Override
    public Category save(Category category) {
        return repository.save(category);
    }

    @Override
    public List<Category> saveAll(List<Category> categoryList) {
        return repository.saveAll(categoryList);
    }

    @Override
    public Optional<Category> findBySid(UUID categorySid) {
        return repository.findBySid(categorySid);
    }

    @Override
    public List<Category> findByUserSidIsNull() {
        return repository.findAllByUserSidIsNull();
    }

    @Override
    public List<Category> findAllByUserSid(UUID userSid) {
        return repository.findAllByUserSid(userSid);
    }

}
