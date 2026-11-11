// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { SubscriptionInfo } from '@shared/models/subscription.models';

export enum SystemSetupState {
  LICENSE_REQUIRED = 'LICENSE_REQUIRED',
  ACCOUNT_REQUIRED = 'ACCOUNT_REQUIRED',
  NON_PRODUCTION_CONFIRMATION_REQUIRED = 'NON_PRODUCTION_CONFIRMATION_REQUIRED',
  READY = 'READY'
}

export interface SetupInfo {
  status: SystemSetupState;
}

export enum LicenseClaimStatus {
  NOT_REQUESTED = 'NOT_REQUESTED',
  PENDING = 'PENDING',
  EXPIRED = 'EXPIRED',
  ACTIVATED = 'ACTIVATED'
}

export interface LicenseClaimInfo {
  status: LicenseClaimStatus;
}

/**
 * How this instance expects to receive its license, as the server's own reachability probe reads it. A hint,
 * not a verdict: OFFLINE says the server could not reach the portal, which says nothing about the workstation
 * the operator is sitting at - so the sign-up URL is rendered in full either way.
 */
export enum LicenseClaimMode {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE'
}

export interface LicenseClaimResult {
  signUpUrl: string;
  mode: LicenseClaimMode;
  /** The token this claim was minted with. Named on every poll, so a claim a later request superseded is answered EXPIRED. */
  claimToken: string;
}

export interface LicenseKeyRequest {
  secret: string;
}

export interface SystemSetupRequest {
  email: string;
  password: string;
  loadDemo: boolean;
}

export interface LicenseChangeResult {
  status: SystemSetupState;
  subscription: SubscriptionInfo;
}

/**
 * Body of the HTTP 423 (Locked) error returned by any API call while the system setup is not complete.
 */
export interface SetupIncompleteError {
  setupState: SystemSetupState;
}

/** READY is rejected: a stray 423 carrying it would announce that setup just completed, mid-session. */
export const isSetupIncompleteError = (error: any): error is SetupIncompleteError => {
  return !!error?.setupState && error.setupState !== SystemSetupState.READY &&
    Object.values(SystemSetupState).includes(error.setupState);
}
