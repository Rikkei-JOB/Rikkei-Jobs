package com.rikkei.jobs.profile.repository.admin;

import com.rikkei.jobs.profile.entity.admin.EvaluationTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplate, UUID> {
    @EntityGraph(attributePaths = "criteria")
    @Query("select distinct t from EvaluationTemplate t order by t.name")
    List<EvaluationTemplate> findAllWithCriteria();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
