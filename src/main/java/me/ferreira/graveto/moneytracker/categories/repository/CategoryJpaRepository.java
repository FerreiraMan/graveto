package me.ferreira.graveto.moneytracker.categories.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

  @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.sid = ?1")
  Optional<Category> findBySid(final UUID sid);

  @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent " +
      "WHERE c.sid = ?1 " +
      "AND c.isInternal IS FALSE " +
      "AND (c.accountSid IS NULL OR c.accountSid = ?2)")
  Optional<Category> findBySidOrUserSid(final UUID sid, final UUID userSid);

  @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent " +
      "WHERE c.isInternal IS FALSE " +
      "AND c.accountSid IS NULL")
  List<Category> findAllByAccountSidIsNull();

  @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent " +
      "WHERE c.isInternal IS FALSE " +
      "AND (c.accountSid IS NULL OR c.accountSid = ?1)")
  List<Category> findAllByAccountSid(final UUID accountSid);

  @Query(value = "SELECT COUNT(c) > 0 FROM Category c " +
      "WHERE c.name = ?1 AND (c.accountSid = ?2 OR c.accountSid is NULL)")
  boolean existsByNameForUserOrSystem(final String name, final UUID userSid);

}
