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

import { AccessToken, ClientCredentials } from 'simple-oauth2';
import { _logger } from '../config/logger';

interface OauthBearerProviderOptions {
  clientId: string;
  clientSecret: string;
  host: string;
  refreshThresholdMs: number;
}

const RETRY_DELAY_MS = 5000;
const MIN_REFRESH_DELAY_MS = 1000;

export const oauthBearerProvider = (options: OauthBearerProviderOptions) => {
  const logger = _logger('oauthBearerProvider');
  if (!options.clientId || !options.clientSecret || !options.host) {
    throw new Error('Kafka OAUTHBEARER requires kafka.confluent.oauth.client_id, client_secret and endpoint_url to be set');
  }
  if (!/^https:\/\//i.test(options.host)) {
    logger.warn('Kafka OAuth token endpoint URL is not HTTPS (%s); client credentials will be sent unencrypted', options.host);
  }
  const refreshThresholdMs = Number(options.refreshThresholdMs);
  if (!Number.isFinite(refreshThresholdMs) || refreshThresholdMs < 0) {
    throw new Error(`Kafka OAuth refresh_threshold must be a non-negative number, got: ${options.refreshThresholdMs}`);
  }
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
  let refreshTimer: NodeJS.Timeout | undefined;

  function scheduleRefresh(delayMs: number): void {
    if (refreshTimer) {
      clearTimeout(refreshTimer);
    }
    const delay = Math.max(delayMs, MIN_REFRESH_DELAY_MS);
    logger.info('Next Kafka OAuth token refresh in %dms', delay);
    refreshTimer = setTimeout(() => {
      tokenPromise = refreshToken();
      // refreshToken() reschedules its own retry on failure; swallow here to avoid an unhandled rejection.
      tokenPromise.catch((err) => {
        logger.error('Scheduled Kafka OAuth token refresh failed: %s', err?.message ?? err);
      });
    }, delay);
    refreshTimer.unref();
  }

  async function refreshToken(): Promise<string> {
    logger.info('Requesting Kafka OAuth bearer token');
    try {
      // Client-credentials grant issues no refresh_token, so always request a fresh token.
      const accessToken: AccessToken = await client.getToken({});

      const expiresIn = accessToken.token.expires_in;
      if (typeof expiresIn !== 'number' || expiresIn <= 0) {
        throw new Error(`OAuth token response has invalid "expires_in": ${expiresIn}`);
      }
      const accessTokenValue = accessToken.token.access_token;
      if (typeof accessTokenValue !== 'string' || accessTokenValue.length === 0) {
        throw new Error('OAuth token response has no "access_token"');
      }

      const nextRefresh = expiresIn * 1000 - refreshThresholdMs;
      scheduleRefresh(nextRefresh);
      return accessTokenValue;
    } catch (err) {
      logger.error('Failed to obtain Kafka OAuth bearer token, retrying in %dms', RETRY_DELAY_MS);
      scheduleRefresh(RETRY_DELAY_MS);
      throw err;
    }
  }

  tokenPromise = refreshToken();
  // The first fetch is retried internally; swallow here, so a startup failure is not an unhandled rejection.
  tokenPromise.catch(() => { /* retry already scheduled in refreshToken() */ });

  return async function () {
    return {
      value: await tokenPromise
    };
  };
};
