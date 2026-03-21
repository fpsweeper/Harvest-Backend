package com.fpsweeper.harvest.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BotIndicatorConditionRepository extends JpaRepository<BotIndicatorCondition, UUID> {

    // Find all conditions for a bot
    List<BotIndicatorCondition> findByBotIdOrderByConditionOrder(UUID botId);

    // Find entry conditions
    List<BotIndicatorCondition> findByBotIdAndConditionTypeOrderByConditionOrder(UUID botId, ConditionType conditionType);

    // Delete all conditions for a bot
    void deleteByBotId(UUID botId);
}