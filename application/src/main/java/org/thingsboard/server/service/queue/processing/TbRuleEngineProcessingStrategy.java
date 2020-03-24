package org.thingsboard.server.service.queue.processing;

public interface TbRuleEngineProcessingStrategy {

    TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result);

}
