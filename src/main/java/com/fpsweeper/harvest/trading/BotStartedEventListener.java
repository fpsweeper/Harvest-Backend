// BotStartedEventListener.java
package com.fpsweeper.harvest.trading;

import com.fpsweeper.harvest.trading.scheduler.BotExecutionScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BotStartedEventListener {

    private static final Logger log = LoggerFactory.getLogger(BotStartedEventListener.class);

    @Autowired
    private BotExecutionScheduler botExecutionScheduler;

    @Async  // runs in a separate thread so HTTP response returns immediately
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // ↑ This is the key — fires ONLY after the transaction fully commits
    // No more stale reads on Render/Supabase
    public void onBotStarted(BotStartedEvent event) {
        try {
            log.info("⚡ Post-commit execution triggered for bot: {}", event.getBotId());
            // Small safety delay — let connection pool settle after commit
            Thread.sleep(1000);
            botExecutionScheduler.executeSingleBot(event.getBotId());
        } catch (Exception e) {
            log.error("❌ Post-commit execution failed for bot {}: {}", event.getBotId(), e.getMessage());
            // Safe to fail silently — scheduler will pick it up in max 5 minutes
        }
    }
}