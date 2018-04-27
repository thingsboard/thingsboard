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

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.kinesis.action.KinesisActionMsg;
import org.thingsboard.server.extensions.kinesis.action.KinesisActionPayload;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.extensions.kinesis.plugin.TestUtils.*;

public class KinesisMsgHandlerTest {

	private final PluginContext pluginContextMock = Mockito.mock(PluginContext.class);
	private final AmazonKinesisAsync kinesisMock = Mockito.mock(AmazonKinesisAsync.class);
	private final KinesisMsgHandler msgHandler = new KinesisMsgHandler(kinesisMock);

	@Test
	public void shouldFailWhenMessageFormatUnsupported() {
		//given
		TenantId tenantId = aTenantId();
		RuleId ruleId = aRuleId();

		//when
		try {
			msgHandler.process(pluginContextMock, tenantId, ruleId, aRuleToPluginMsg());
			fail();
		} catch (RuleException e) {
			// then exception expected
		}
	}

	@Test
	public void shouldSendSuccesfullyWhenFormatCorrect() {
		//given
		TenantId tenantId = aTenantId();
		RuleId ruleId = aRuleId();
		DeviceId deviceId = aDeviceId();
		CustomerId customerId = aCustomerId();
		boolean sync = false;
		KinesisActionMsg msg = anActionMsg(tenantId, customerId, deviceId, sync);

		//when
		try {
			msgHandler.process(pluginContextMock, tenantId, ruleId, msg);
		} catch (RuleException e) {
			fail(e.getMessage());
		}

		//then
		verify(kinesisMock).putRecordAsync(eq(KinesisPutRecordRequestFactory.INSTANCE.create(msg)), any(AsyncHandler.class));
	}

	@Test
	public void shouldSendSuccesfullyAndReplyItToPluginContext() {
		//given
		TenantId tenantId = aTenantId();
		RuleId ruleId = aRuleId();
		DeviceId deviceId = aDeviceId();
		CustomerId customerId = aCustomerId();
		boolean sync = true;
		KinesisActionMsg msg = anActionMsg(tenantId, customerId, deviceId, sync);
		PutRecordRequest putRecordRequest = KinesisPutRecordRequestFactory.INSTANCE.create(msg);

		setUpMockToAnswerKinesisAsSuccess(putRecordRequest);

		//when
		try {
			msgHandler.process(pluginContextMock, tenantId, ruleId, msg);
		} catch (RuleException e) {
			fail(e.getMessage());
		}

		//then
		verify(pluginContextMock).reply(any(ResponsePluginToRuleMsg.class));
	}

	@Test
	public void shouldFailAndReplyItToPluginContext() {
		//given
		TenantId tenantId = aTenantId();
		RuleId ruleId = aRuleId();
		DeviceId deviceId = aDeviceId();
		CustomerId customerId = aCustomerId();
		boolean sync = true;
		KinesisActionMsg msg = anActionMsg(tenantId, customerId, deviceId, sync);
		PutRecordRequest putRecordRequest = KinesisPutRecordRequestFactory.INSTANCE.create(msg);

		setUpMockToAnswerKinesisAsFailed(putRecordRequest);

		//when
		try {
			msgHandler.process(pluginContextMock, tenantId, ruleId, msg);
		} catch (RuleException e) {
			fail(e.getMessage());
		}

		//then
		verify(pluginContextMock).reply(any(ResponsePluginToRuleMsg.class));
	}

	private void setUpMockToAnswerKinesisAsSuccess(PutRecordRequest putRecordRequest) {
		when(kinesisMock.putRecordAsync(eq(putRecordRequest), any(AsyncHandler.class)))
				.thenAnswer(invocationOnMock -> {
					final AsyncHandler asyncHandler = (AsyncHandler) (invocationOnMock.getArguments())[1];
					asyncHandler.onSuccess(putRecordRequest, new PutRecordResult());
					return Mockito.mock(Future.class);
				});
	}

	private void setUpMockToAnswerKinesisAsFailed(PutRecordRequest putRecordRequest) {
		when(kinesisMock.putRecordAsync(eq(putRecordRequest), any(AsyncHandler.class)))
				.thenAnswer(invocationOnMock -> {
					final AsyncHandler asyncHandler = (AsyncHandler) (invocationOnMock.getArguments())[1];
					asyncHandler.onError(new Exception());
					return Mockito.mock(Future.class);
				});
	}

	private KinesisActionMsg anActionMsg(TenantId tenantId, CustomerId customerId, DeviceId deviceId, boolean sync) {
		KinesisActionPayload aPayload = KinesisActionPayload.builder()
				.msgBody("testMsg")
				.sync(sync)
				.stream("testStream")
				.requestId(1)
				.build();

		return new KinesisActionMsg(tenantId, customerId, deviceId, aPayload);
	}


	private RuleToPluginMsg aRuleToPluginMsg() {
		return new RuleToPluginMsg() {
			@Override
			public UUID getUid() {
				return null;
			}

			@Override
			public DeviceId getDeviceId() {
				return null;
			}

			@Override
			public CustomerId getCustomerId() {
				return null;
			}

			@Override
			public Serializable getPayload() {
				return null;
			}
		};
	}

}
