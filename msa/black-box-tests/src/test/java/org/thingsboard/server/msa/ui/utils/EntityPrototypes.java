package org.thingsboard.server.msa.ui.utils;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.rule.RuleChain;

public class CustomerPrototypes {

    public static Customer defaultCustomerPrototype(String entityName){
        Customer customer = new Customer();
        customer.setTitle(entityName);
        return customer;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName){
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        return ruleChain;
    }
}
