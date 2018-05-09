/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.queue.cassandra;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class UnprocessedMsgFilterTest {

    private UnprocessedMsgFilter msgFilter = new UnprocessedMsgFilter();

    @Test
    public void acknowledgedMsgsAreFilteredOut() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TbMsg msg1 = new TbMsg(id1, "T", null, null, null, null, null, null, 0L);
        TbMsg msg2 = new TbMsg(id2, "T", null, null, null, null, null, null, 0L);
        List<TbMsg> msgs = Lists.newArrayList(msg1, msg2);
        List<MsgAck> acks = Lists.newArrayList(new MsgAck(id2, UUID.randomUUID(), 1L, 1L));
        Collection<TbMsg> actual = msgFilter.filter(msgs, acks);
        assertEquals(1, actual.size());
        assertEquals(msg1, actual.iterator().next());
    }

}