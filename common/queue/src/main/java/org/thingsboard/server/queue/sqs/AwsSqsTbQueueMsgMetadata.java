package org.thingsboard.server.queue.sqs;

import com.amazonaws.http.SdkHttpMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

@Data
@AllArgsConstructor
public class AwsSqsTbQueueMsgMetadata implements TbQueueMsgMetadata {

    private final SdkHttpMetadata metadata;
}
