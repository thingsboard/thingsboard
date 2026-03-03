///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { AccessToken, ClientCredentials } from 'simple-oauth2'
import { _logger, KafkaJsWinstonLogCreator } from '../config/logger';

interface OauthBearerProviderOptions {
  clientId: string;
  clientSecret: string;
  host: string;
  refreshThresholdMs: number;
}

export const oauthBearerProvider = (options: OauthBearerProviderOptions) => {
  const logger = _logger('oauthBearerProvider')
  const client = new ClientCredentials({
    client: {
      id: options.clientId,
      secret: options.clientSecret
    },
    auth: {
      tokenHost: options.host
    }
  });

  let tokenPromise: Promise<string>;
  let accessToken: AccessToken;

  async function refreshToken():Promise<string>{
    logger.info('Start token refreshing/validation');
    try {
      if (accessToken == null) {
        accessToken = await client.getToken({})
        logger.info('Got new token');
      }

      if (accessToken.expired(options.refreshThresholdMs / 1000)) {
        logger.info(`Token will expire during next ${options.refreshThresholdMs}ms. Refresh token`);
        accessToken = await accessToken.refresh()
      }
      let expires_in = typeof accessToken.token.expires_in === "number" ? accessToken.token.expires_in : 0; //throw exception
      const nextRefresh = expires_in * 1000 - options.refreshThresholdMs;
      logger.info(`Next token validation in ${nextRefresh}ms.`);
      setTimeout(() => {
        tokenPromise = refreshToken()
      }, nextRefresh);
      let access_token = typeof accessToken.token.access_token === "string" ? accessToken.token.access_token : ""; //throw exception
      return access_token;
    } catch (error) {
      throw error;
    }
  }

  tokenPromise = refreshToken();

  return async function () {
    return {
      value: await tokenPromise
    }
  }
};