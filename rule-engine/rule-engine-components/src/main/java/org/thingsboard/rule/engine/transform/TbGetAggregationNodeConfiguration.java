package org.thingsboard.rule.engine.transform;

/**
 * Created by mshvayka on 28.07.18.
 */
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbGetAggregationNodeConfiguration implements NodeConfiguration<TbGetAggregationNodeConfiguration> {

    private String prefixTsKeyNames;

    @Override
    public TbGetAggregationNodeConfiguration defaultConfiguration() {
        TbGetAggregationNodeConfiguration configuration = new TbGetAggregationNodeConfiguration();
        configuration.setPrefixTsKeyNames(null);
        return configuration;
    }

}


