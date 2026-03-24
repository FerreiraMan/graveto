package me.ferreira.graveto.moneytracker.categories.repository;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySid(final UUID sid);

    List<Category> findAllByUserSidIsNull();

    @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent " +
                    "WHERE c.isInternal IS FALSE " +
                    "AND (c.userSid IS NULL OR c.userSid = ?1)")
    List<Category> findAllByUserSid(final UUID userSid);

}
