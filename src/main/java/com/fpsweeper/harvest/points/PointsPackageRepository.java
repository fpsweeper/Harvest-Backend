package com.fpsweeper.harvest.points;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PointsPackageRepository extends JpaRepository<PointsPackage, UUID> {
    List<PointsPackage> findByActiveTrueOrderBySortOrderAsc();
}