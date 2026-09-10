// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuleNode {

    ComponentType type();

    String name();

    String nodeDescription();

    String nodeDetails();

    Class<? extends NodeConfiguration> configClazz();

    ComponentClusteringMode clusteringMode() default ComponentClusteringMode.ENABLED;

    boolean hasQueueName() default false;

    boolean inEnabled() default true;

    boolean outEnabled() default true;

    ComponentScope scope() default ComponentScope.TENANT;

    String[] relationTypes() default {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE};

    String[] uiResources() default {};

    String configDirective() default "";

    String icon() default "";

    String iconUrl() default "";

    String docUrl() default "";

    boolean customRelations() default false;

    boolean ruleChainNode() default false;

    RuleChainType[] ruleChainTypes() default {RuleChainType.CORE, RuleChainType.EDGE};

    int version() default 0;

}
