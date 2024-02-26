/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.configuration;

import lombok.Data;

@Data
@Deprecated
public class TbRuleEngineQueueConfiguration {

    private String name;
    private String topic;
    private int pollInterval;
    private int partitions;
    private boolean consumerPerPartition;
    private long packProcessingTimeout;
    private TbRuleEngineQueueSubmitStrategyConfiguration submitStrategy;
    private TbRuleEngineQueueAckStrategyConfiguration processingStrategy;

}