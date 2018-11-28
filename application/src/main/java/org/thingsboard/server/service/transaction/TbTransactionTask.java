package org.thingsboard.server.service.transaction;

import lombok.Data;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.function.Consumer;

@Data
public final class TbTransactionTask {

    private final TbMsg msg;
    private final Consumer<TbMsg> onStart;
    private final Consumer<TbMsg> onEnd;
    private final Consumer<Throwable> onFailure;

}
