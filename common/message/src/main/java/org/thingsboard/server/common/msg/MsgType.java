package org.thingsboard.server.common.msg;

/**
 * Created by ashvayka on 15.03.18.
 */
public enum MsgType {

    /**
     * ADDED/UPDATED/DELETED events for main entities.
     *
     * @See {@link org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg}
     */
    COMPONENT_LIFE_CYCLE_MSG,

    /**
     * Misc messages from the REST API/SERVICE layer to the new rule engine.
     *
     * @See {@link org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg}
     */
    SERVICE_TO_RULE_ENGINE_MSG,


    SESSION_TO_DEVICE_ACTOR_MSG,
    DEVICE_ACTOR_TO_SESSION_MSG,


    /**
     * Message that is sent by RuleChainActor to RuleActor with command to process TbMsg.
     */
    RULE_CHAIN_TO_RULE_MSG,

    /**
     * Message that is sent by RuleActor to RuleChainActor with command to process TbMsg by next nodes in chain.
     */
    RULE_TO_RULE_CHAIN_TELL_NEXT_MSG,

}
