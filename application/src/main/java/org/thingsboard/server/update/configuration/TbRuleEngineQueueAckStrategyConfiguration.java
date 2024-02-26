/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.configuration;

import lombok.Data;

@Data
@Deprecated
public class TbRuleEngineQueueAckStrategyConfiguration {

    private String type;
    private int retries;
    private double failurePercentage;
    private long pauseBetweenRetries;
    private long maxPauseBetweenRetries;

}