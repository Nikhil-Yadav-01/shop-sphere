package com.rudraksha.shopsphere.shared.db;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JpaSpecificationBuilder<T> {

    private final List<Specification<T>> specifications = new ArrayList<>();

    public JpaSpecificationBuilder<T> addEquals(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    public JpaSpecificationBuilder<T> addLike(String field, String value) {
        if (value != null && !value.isBlank()) {
            specifications.add((root, query, cb) ->
                    cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
        }
        return this;
    }

    public <Y extends Comparable<? super Y>> JpaSpecificationBuilder<T> addBetween(
            String field, Y start, Y end) {
        if (start != null && end != null) {
            specifications.add((root, query, cb) -> cb.between(root.get(field), start, end));
        } else if (start != null) {
            specifications.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), start));
        } else if (end != null) {
            specifications.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), end));
        }
        return this;
    }

    public JpaSpecificationBuilder<T> addIn(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            specifications.add((root, query, cb) -> root.get(field).in(values));
        }
        return this;
    }

    public JpaSpecificationBuilder<T> addNotDeleted() {
        specifications.add((root, query, cb) -> cb.equal(root.get("deleted"), false));
        return this;
    }

    public JpaSpecificationBuilder<T> addIsNull(String field) {
        specifications.add((root, query, cb) -> cb.isNull(root.get(field)));
        return this;
    }

    public JpaSpecificationBuilder<T> addIsNotNull(String field) {
        specifications.add((root, query, cb) -> cb.isNotNull(root.get(field)));
        return this;
    }

    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return Specification.where(null);
        }

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<T> spec : specifications) {
                Predicate predicate = spec.toPredicate(root, query, cb);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
