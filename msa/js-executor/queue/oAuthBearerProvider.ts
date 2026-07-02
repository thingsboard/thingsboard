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
  endpointUrl: string;
  refreshThresholdMs: number;
  // Optional OAuth2 scope. Required by some IdPs for client-credentials (e.g. Azure AD's "api://<id>/.default").
  scope?: string;
}

const RETRY_DELAY_MS = 5000;
const MIN_REFRESH_DELAY_MS = 1000;
// Safety margin applied at serve time: if the cached token is within this window of (or past) its
// hard expiry, fetch synchronously instead of serving it. Covers a scheduled refresh that slipped
// because the unref()'d timer was starved or the process was suspended. Also absorbs minor clock skew.
const EXPIRY_SAFETY_MS = 5000;

export const oauthBearerProvider = (options: OauthBearerProviderOptions) => {
  const logger = _logger('oauthBearerProvider');
  if (!options.clientId || !options.clientSecret || !options.endpointUrl) {
    throw new Error('Kafka OAUTHBEARER requires kafka.confluent.oauth.client_id, client_secret and endpoint_url to be set');
  }
  if (!/^https:\/\//i.test(options.endpointUrl)) {
    logger.warn('Kafka OAuth token endpoint URL is not HTTPS (%s); client credentials will be sent unencrypted', options.endpointUrl);
  }
  const refreshThresholdMs = Number(options.refreshThresholdMs);
  if (!Number.isFinite(refreshThresholdMs) || refreshThresholdMs < 0) {
    throw new Error(`Kafka OAuth refresh_threshold must be a non-negative number, got: ${options.refreshThresholdMs}`);
  }
  const scope = options.scope && options.scope.trim().length > 0 ? options.scope.trim() : undefined;
  let tokenUrl: URL;
  try {
    tokenUrl = new URL(options.endpointUrl);
  } catch {
    throw new Error(`Kafka OAuth endpoint_url is not a valid URL: ${options.endpointUrl}`);
  }
  const client = new ClientCredentials({
    client: {
      id: options.clientId,
      secret: options.clientSecret
    },
    auth: {
      // endpoint_url is the full token endpoint URL. Split it into host + path so
      // simple-oauth2 does not append its default tokenPath (/oauth/token) and
      // discard the real path (breaks Keycloak, Azure AD, Okta, etc.).
      tokenHost: tokenUrl.origin,
      tokenPath: tokenUrl.pathname + tokenUrl.search
    }
  });

  // Last successfully fetched token. Kept across refreshes so a failed background refresh
  // does not drop a still-valid token out from under KafkaJS (which calls this provider on
  // each reauthentication). It is only swapped in after a successful refresh.
  let cachedToken: string | undefined;
  // Epoch ms at which cachedToken hard-expires (access_token lifetime from the IdP). 0 until the
  // first successful fetch. Used to decide at serve time whether the cached token is still safe.
  let cachedTokenExpiresAt = 0;
  // In-flight token fetch, shared so a serve-triggered refresh and the scheduled timer (and
  // concurrent KafkaJS callbacks) coalesce onto one request instead of stampeding the IdP.
  let refreshInFlight: Promise<string> | undefined;
  let refreshTimer: NodeJS.Timeout | undefined;
  let warnedShortLivedToken = false;

  function scheduleRefresh(delayMs: number): void {
    if (refreshTimer) {
      clearTimeout(refreshTimer);
    }
    const delay = Math.max(delayMs, MIN_REFRESH_DELAY_MS);
    logger.info('Next Kafka OAuth token refresh in %dms', delay);
    refreshTimer = setTimeout(() => {
      // refreshTokenOnce() updates cachedToken on success and reschedules its own retry on failure;
      // swallow here so a failed background refresh is not an unhandled rejection.
      refreshTokenOnce().catch((err) => {
        logger.error('Scheduled Kafka OAuth token refresh failed: %s', err?.message ?? err);
      });
    }, delay);
    refreshTimer.unref();
  }

  // Coalesce concurrent refreshes (scheduled timer, serve-time fallback, parallel KafkaJS callbacks)
  // onto a single in-flight request so a near-expiry burst cannot stampede the token endpoint.
  function refreshTokenOnce(): Promise<string> {
    if (!refreshInFlight) {
      refreshInFlight = refreshToken().finally(() => {
        refreshInFlight = undefined;
      });
    }
    return refreshInFlight;
  }

  async function refreshToken(): Promise<string> {
    logger.info('Requesting Kafka OAuth bearer token');
    try {
      // Client-credentials grant issues no refresh_token, so always request a fresh token.
      const accessToken: AccessToken = await client.getToken(scope ? { scope } : {});

      // Some IdPs return expires_in as a numeric string ("3600"); coerce before validating.
      const rawExpiresIn = accessToken.token.expires_in;
      const expiresIn = Number(rawExpiresIn);
      if (!Number.isFinite(expiresIn) || expiresIn <= 0) {
        throw new Error(`OAuth token response has invalid "expires_in": ${rawExpiresIn}`);
      }
      const accessTokenValue = accessToken.token.access_token;
      if (typeof accessTokenValue !== 'string' || accessTokenValue.length === 0) {
        throw new Error('OAuth token response has no "access_token"');
      }

      const lifetimeMs = expiresIn * 1000;
      cachedToken = accessTokenValue;
      cachedTokenExpiresAt = Date.now() + lifetimeMs;

      // If the token lifetime is shorter than the refresh threshold, refreshing "threshold
      // before expiry" would fire immediately and clamp to MIN_REFRESH_DELAY_MS, hammering the
      // token endpoint every second. Cap the effective threshold to half the lifetime instead.
      let effectiveThresholdMs = refreshThresholdMs;
      if (refreshThresholdMs >= lifetimeMs) {
        effectiveThresholdMs = lifetimeMs / 2;
        if (!warnedShortLivedToken) {
          logger.warn('Kafka OAuth token lifetime (%dms) is <= refresh threshold (%dms); capping threshold to %dms to avoid hammering the token endpoint',
            lifetimeMs, refreshThresholdMs, effectiveThresholdMs);
          warnedShortLivedToken = true;
        }
      }

      const nextRefresh = lifetimeMs - effectiveThresholdMs;
      scheduleRefresh(nextRefresh);
      return accessTokenValue;
    } catch (err) {
      logger.error('Failed to obtain Kafka OAuth bearer token, retrying in %dms', RETRY_DELAY_MS);
      scheduleRefresh(RETRY_DELAY_MS);
      throw err;
    }
  }

  // Kick off the initial fetch eagerly so the first authentication is fast.
  // The first fetch is retried internally; swallow here, so a startup failure is not an unhandled rejection.
  refreshTokenOnce().catch(() => { /* retry already scheduled in refreshToken() */ });

  return async function () {
    // Serve the cached token while it is comfortably valid; the background timer rotates it
    // proactively. If there is no token yet, or the cached one is within EXPIRY_SAFETY_MS of its
    // hard expiry (a scheduled refresh slipped), fetch synchronously so KafkaJS never reauthenticates
    // with an expired token. Concurrent callers coalesce onto the same request via refreshTokenOnce().
    if (!cachedToken || Date.now() >= cachedTokenExpiresAt - EXPIRY_SAFETY_MS) {
      return {
        value: await refreshTokenOnce()
      };
    }
    return {
      value: cachedToken
    };
  };
};
