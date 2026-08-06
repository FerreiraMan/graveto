package me.ferreira.graveto.moneytracker.categories.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.Category_;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryJpaRepository;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.repository.CategorySpecs;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
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
  public List<Category> findAll(final FindAllCategoriesCommand command) {

    final PredicateSpecification<Category> predicateSpec = CategorySpecs.buildFromCommand(command);
    final Specification<Category> classicSpec = Specification.where(predicateSpec);
    final Sort sortAlphabetically = Sort.by(Sort.Order.by(Category_.DISPLAY_NAME).ignoreCase());

    return repository.findAll(classicSpec, sortAlphabetically);
  }

  @Override
  public List<Category> findAll() {
    return repository.findAll();
  }

  @Override
  public boolean existsByNameForAccountOrSystem(final String name, final UUID accountSid) {
    return repository.existsByNameForAccountOrSystem(name, accountSid);
  }

}
