package org.thingsboard.rule.engine.api;

import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 19.01.18.
 */
public interface TbNode {

    void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException;

    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    void destroy();

}
