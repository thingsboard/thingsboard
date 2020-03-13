package org.thingsboard.server.service.edge.rpc.init;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.ruleChain.RuleChainMetadataConstructor;

@Service
@Slf4j
public class DefaultInitEdgeService implements InitEdgeService {

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RuleChainMetadataConstructor ruleChainMetadataConstructor;

    @Override
    public void init(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        initRuleChains(edge, outputStream);
    }

    private void initRuleChains(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            TimePageLink pageLink = new TimePageLink(100);
            TimePageData<RuleChain> pageData;
            do {
                pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), pageLink).get();
                if (!pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (RuleChain ruleChain : pageData.getData()) {
                        RuleChainUpdateMsg ruleChainUpdateMsg =
                                ruleChainMetadataConstructor.constructRuleChainUpdatedMsg(
                                        edge,
                                        UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                        ruleChain);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setRuleChainUpdateMsg(ruleChainUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());

                        RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edge.getTenantId(), ruleChain.getId());
                        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                                ruleChainMetadataConstructor.constructRuleChainMetadataUpdatedMsg(
                                        UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                        ruleChainMetaData);
                        if (ruleChainMetadataUpdateMsg != null) {
                            entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                    .setRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                                    .build();
                            outputStream.onNext(ResponseMsg.newBuilder()
                                    .setEntityUpdateMsg(entityUpdateMsg)
                                    .build());
                        }
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge rule chains on init!");
        }

    }
}
