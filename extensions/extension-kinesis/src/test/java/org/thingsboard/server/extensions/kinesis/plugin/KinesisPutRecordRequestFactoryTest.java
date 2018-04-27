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
package org.thingsboard.server.extensions.kinesis.plugin;

import com.amazonaws.services.kinesis.model.PutRecordRequest;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.kinesis.action.KinesisActionMsg;
import org.thingsboard.server.extensions.kinesis.action.KinesisActionPayload;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.thingsboard.server.extensions.kinesis.plugin.TestUtils.aCustomerId;
import static org.thingsboard.server.extensions.kinesis.plugin.TestUtils.aDeviceId;
import static org.thingsboard.server.extensions.kinesis.plugin.TestUtils.aTenantId;

public class KinesisPutRecordRequestFactoryTest {

	@Test
	public void shouldCreatePutRequestFactoryProperly() {
		//given
		TenantId tenantId = aTenantId();
		CustomerId customerId = aCustomerId();
		DeviceId deviceId = aDeviceId();
		KinesisActionMsg msg = anActionMsg(tenantId, customerId, deviceId);

		//when
		PutRecordRequest actual = KinesisPutRecordRequestFactory.INSTANCE.create(msg);

		//then
		PutRecordRequest expected = new PutRecordRequest()
				.withData(ByteBuffer.wrap(msg.getPayload().getMsgBody().getBytes()))
				.withStreamName(msg.getPayload().getStream())
				.withPartitionKey(msg.getUid().toString());
		assertEquals(expected, actual);

	}

	private KinesisActionMsg anActionMsg(TenantId tenantId, CustomerId customerId, DeviceId deviceId) {
		KinesisActionPayload aPayload = KinesisActionPayload.builder()
				.msgBody("testMsg")
				.stream("testStream")
				.build();

		return new KinesisActionMsg(tenantId, customerId, deviceId, aPayload);
	}
}
