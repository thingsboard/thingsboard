/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

@ExtendWith(MockitoExtension.class)
class TbRabbitMqConsumerTemplateTest {

  private static final String TOPIC = "some-topic";

  @Mock
  private TbQueueAdmin admin;

  @Mock
  private ConnectionFactory connectionFactory;

  @Mock
  private TbQueueMsgDecoder<DefaultTbQueueMsg> decoder;

  @Mock
  private Connection connection;

  @Mock
  private Channel channel;

  @Mock
  private TopicPartitionInfo partition;

  @Mock
  private GetResponse getResponse;

  private TbRabbitMqConsumerTemplate<DefaultTbQueueMsg> consumer;

  private void setUpConsumerWithMaxPollMessages(int maxPollMessages) throws Exception {
    when(connectionFactory.newConnection()).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    TbRabbitMqSettings settings = new TbRabbitMqSettings();
    settings.setMaxPollMessages(maxPollMessages);
    settings.setConnectionFactory(connectionFactory);

    consumer = new TbRabbitMqConsumerTemplate<>(admin, settings, TOPIC, decoder);
    when(partition.getFullTopicName()).thenReturn(TOPIC);
    consumer.subscribe(Set.of(partition));
  }

  @Test
  void pollWithMax5PollMessagesReturnsEmptyListIfNoMessages() throws Exception {
    setUpConsumerWithMaxPollMessages(5);
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(null);

    assertThat(consumer.poll(0L)).isEmpty();

    verify(channel).basicGet(anyString(), anyBoolean());
  }

  @Test
  void pollWithMax5PollMessagesReturns5MessagesIfQueueContains5() throws Exception {
    setUpConsumerWithMaxPollMessages(5);
    when(getResponse.getBody()).thenReturn(newMessageBody());
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(getResponse);

    assertThat(consumer.poll(0L)).hasSize(5);

    verify(channel, times(5)).basicGet(anyString(), anyBoolean());
  }

  @Test
  void pollWithMax1PollMessageReturns1MessageIfQueueContainsMore() throws Exception {
    setUpConsumerWithMaxPollMessages(1);
    when(getResponse.getBody()).thenReturn(newMessageBody());
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(getResponse);

    assertThat(consumer.poll(0L)).hasSize(1);

    verify(channel).basicGet(anyString(), anyBoolean());
  }

  @Test
  void pollWithMax3PollMessagesReturns2MessagesIfQueueContains2() throws Exception {
    setUpConsumerWithMaxPollMessages(3);
    when(getResponse.getBody()).thenReturn(newMessageBody());
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(getResponse, getResponse, null);

    assertThat(consumer.poll(0L)).hasSize(2);

    verify(channel, times(3)).basicGet(anyString(), anyBoolean());
  }

  private byte[] newMessageBody() {
    return ("{\"key\": \"" + UUID.randomUUID() + "\"}").getBytes(StandardCharsets.UTF_8);
  }

}