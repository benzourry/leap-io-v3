package com.benzourry.leap.utility.audit;

import com.benzourry.leap.model.MetaTrail;
import com.benzourry.leap.repository.MetaTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MetaTrailAsyncHandler {

    private static final Logger logger = LoggerFactory.getLogger(MetaTrailAsyncHandler.class);

    @Autowired
    private MetaTrailRepository metaTrailRepository;

    @Async("asyncExec") // Run on a background thread
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Only run if main transaction committed
    public void handleMetaTrailEvent(MetaTrailEvent event) {
        try {
            MetaTrail trail = MetaTrail.builder()
                    .moduleId(event.getModuleId())
                    .module(event.getModule())
                    .scope(event.getScope())
                    .action(event.getAction())
                    .appId(event.getAppId())
                    .data(event.getData())
                    .email(event.getEmail())
                    .build();

            metaTrailRepository.save(trail);
        } catch (Exception e) {
            // Log the error, but this will NOT roll back the main transaction
            // since it runs in a separate thread after commit.
            logger.error("Failed to save MetaTrail for module [{}] id [{}]", event.getModule(), event.getModuleId(), e);
        }
    }
}