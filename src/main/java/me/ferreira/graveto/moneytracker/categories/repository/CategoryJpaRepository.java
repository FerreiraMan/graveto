package me.ferreira.graveto.moneytracker.categories.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.Category_;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CategoryJpaRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

  @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.sid = ?1")
  Optional<Category> findBySid(final UUID sid);

  @Override
  @EntityGraph(attributePaths = {Category_.PARENT})
  List<Category> findAll(final Specification<Category> predicateSpec, final Sort sort);

  @Query(value = "SELECT c FROM Category c LEFT JOIN FETCH c.parent " +
      "WHERE c.sid = ?1 " +
      "AND c.isInternal IS FALSE " +
      "AND (c.accountSid IS NULL OR c.accountSid = ?2)")
  Optional<Category> findBySidOrAccountSid(final UUID sid, final UUID accountSid);

  @Query(value = "SELECT COUNT(c) > 0 FROM Category c " +
      "WHERE c.name = ?1 AND (c.accountSid = ?2 OR c.accountSid is NULL)")
  boolean existsByNameForAccountOrSystem(final String name, final UUID accountSid);

  @Query(value =
      "WITH RECURSIVE tree AS (" +
          "   SELECT * FROM categories c1 " +
          "   WHERE c1.sid = ?1 AND (c1.account_sid IS NULL OR c1.account_sid = ?2) " +
          "   UNION " +
          "   SELECT c2.* FROM categories c2 " +
          "   JOIN tree t ON c2.parent_id = t.id " +
          "   WHERE (c2.account_sid IS NULL OR c2.account_sid = ?2)" +
          ") " +
          "SELECT * FROM tree", nativeQuery = true)
  List<Category> findCategoryAndAllDescendants(final UUID sid, final UUID accountSid);

}
