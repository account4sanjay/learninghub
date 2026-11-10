package com.learninghub.schedule.repo;

import java.util.List;
import java.util.Optional;

import com.learninghub.schedule.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, String> {
    Optional<Term> findFirstByCurrentTrue();
    List<Term> findAllByOrderByQuarterAscStartDateAsc();
}
