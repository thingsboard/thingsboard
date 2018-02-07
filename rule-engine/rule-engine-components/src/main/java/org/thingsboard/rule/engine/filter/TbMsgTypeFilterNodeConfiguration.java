package org.thingsboard.rule.engine.filter;

import lombok.Data;

import java.util.List;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
public class TbMsgTypeFilterNodeConfiguration {

    private List<String> messageTypes;

}
