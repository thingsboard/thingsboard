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

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class KinesisAsyncFactoryTest {

	@Test
	public void shouldCreateWithBasicAWSCredentialsWhenProvided() {
		//given
		KinesisPluginConfiguration config = configWithBasicCredentials();
		System.setProperty("aws.region", config.getRegion());

		//when
		AmazonKinesisAsync actual = KinesisAsyncFactory.INSTANCE.create(config);

		//then
		assertNotNull(actual);
	}

	@Test(expected = AmazonClientException.class)
	public void shouldCreateWithProfileAWSCredentialsWhenProvided() {
		//given
		KinesisPluginConfiguration config = configWithProfileCredentials();

		//when
		KinesisAsyncFactory.INSTANCE.create(config);

		//then
		//expected exception, because test profile does not exist
	}

	@Test(expected = IllegalStateException.class)
	public void shouldNotCreateWhenNeitherBasicNorProfileCredentialsProvided() {
		//given
		KinesisPluginConfiguration config = new KinesisPluginConfiguration();

		//when
		KinesisAsyncFactory.INSTANCE.create(config);

		//then
		//expected exception, because empty config
	}

	private KinesisPluginConfiguration configWithBasicCredentials() {
		KinesisPluginConfiguration config = new KinesisPluginConfiguration();
		config.setAccessKeyId("testAccessKeyId");
		config.setRegion("eu-west-1");
		config.setSecretAccessKey("testSecretAccessKey");
		return config;
}

	private KinesisPluginConfiguration configWithProfileCredentials() {
		KinesisPluginConfiguration config = new KinesisPluginConfiguration();
		config.setProfile("testProfile");
		return config;
	}

	@After
	public void cleanUp() {
		System.clearProperty("aws.region");
	}
}
