package com.fpsweeper.harvest.points;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportedChainRepository extends JpaRepository<SupportedChain, UUID> {

    Optional<SupportedChain> findByChainName(String chainName);

    List<SupportedChain> findByIsActive(Boolean isActive);

    Optional<SupportedChain> findByChainNameAndIsActive(String chainName, Boolean isActive);
}