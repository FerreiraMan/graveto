package me.ferreira.graveto.moneytracker.categories.repository;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySid(final UUID sid);

    List<Category> findAllByUserSidIsNull();

    List<Category> findAllByUserSid(final UUID userSid);

}
