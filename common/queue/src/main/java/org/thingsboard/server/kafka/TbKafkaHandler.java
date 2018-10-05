package org.thingsboard.server.kafka;

import java.util.function.Consumer;

/**
 * Created by ashvayka on 05.10.18.
 */
public interface TbKafkaHandler<Request, Response> {

    void handle(Request request, Consumer<Response> onSuccess, Consumer<Throwable> onFailure);

}
