package org.thingsboard.rule.engine.filter;

import lombok.Data;

import java.util.Set;

@Data
public class TbJsSwitchNodeConfiguration {

    private String jsScript;
    private Set<String> allowedRelations;
}
