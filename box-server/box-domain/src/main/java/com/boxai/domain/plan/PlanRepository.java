package com.boxai.domain.plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {

    Plan save(Plan plan);

    void update(Plan plan);

    Optional<Plan> findById(Long id);

    Optional<Plan> findByCode(String code);

    List<Plan> listAll();
}
