package me.ferreira.graveto.moneytracker.categories.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryJpaRepository;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

  private final CategoryJpaRepository repository;

  @Override
  public Category save(final Category category) {
    return repository.save(category);
  }

  @Override
  public List<Category> saveAll(final List<Category> categoryList) {
    return repository.saveAll(categoryList);
  }

  @Override
  public Optional<Category> findBySid(final UUID categorySid) {
    return repository.findBySid(categorySid);
  }

  @Override
  public Optional<Category> findBySidOrAccountSid(UUID categorySid, UUID accountSid) {
    return repository.findBySidOrAccountSid(categorySid, accountSid);
  }

  @Override
  public List<Category> findByAccountSidIsNull() {
    return repository.findAllByAccountSidIsNull();
  }

  @Override
  public List<Category> findAll() {
    return repository.findAll();
  }

  @Override
  public List<Category> findAllByAccountSid(final UUID accountSid) {
    return repository.findAllByAccountSid(accountSid);
  }

  @Override
  public boolean existsByNameForAccountOrSystem(final String name, final UUID accountSid) {
    return repository.existsByNameForAccountOrSystem(name, accountSid);
  }

}
