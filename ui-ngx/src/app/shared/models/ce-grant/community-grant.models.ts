// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { parseHttpErrorMessage } from '@core/utils';

export enum CommunityGrantState {
  intro = 'intro',
  awaiting = 'awaiting',
  verifying = 'verifying',
  registered = 'registered',
  review = 'review',
  offline = 'offline',
  verify = 'verify',
  result = 'result',
  /** The report was carried to the portal by hand; the outcome is read there, never reported back here. */
  handedOver = 'handedOver',
  expired = 'expired',
  already = 'already',
  wrongInstallation = 'wrongInstallation'
}

export type CommunityGrantMarkTone = 'brand' | 'info';

export type CommunityGrantRailKind = 'online' | 'offline';

export interface CommunityGrantStateConfig {
  icon?: string;
  cardIcon?: string;
  tone?: CommunityGrantMarkTone;
  rail?: CommunityGrantRailKind;
  railStep?: number;
  hero?: boolean;
  live?: boolean;
  spinner?: boolean;
  topBanner?: string;
}

export const communityGrantStateConfigs =
  new Map<CommunityGrantState, CommunityGrantStateConfig>([
    [CommunityGrantState.intro, {hero: true}],
    [CommunityGrantState.awaiting, {icon: 'ce-grant-waiting', tone: 'brand', rail: 'online', railStep: 0, live: true}],
    [CommunityGrantState.verifying, {spinner: true, tone: 'brand', rail: 'online', railStep: 1, live: true}],
    [CommunityGrantState.registered, {icon: 'check', tone: 'brand'}],
    [CommunityGrantState.review, {icon: 'hourglass_empty', tone: 'brand'}],
    [CommunityGrantState.offline, {icon: 'computer', tone: 'brand', rail: 'offline', railStep: 0, topBanner: 'offlineBanner'}],
    [CommunityGrantState.verify, {icon: 'upload_file', tone: 'brand', rail: 'offline', railStep: 1}],
    [CommunityGrantState.result, {icon: 'assignment_return', tone: 'brand', rail: 'offline', railStep: 2}],
    // Opens the report screen, so it shares `result`'s header.
    [CommunityGrantState.handedOver, {cardIcon: 'check', icon: 'assignment_return', tone: 'brand', rail: 'offline', railStep: 2}],
    [CommunityGrantState.expired, {icon: 'timer_off', tone: 'info'}],
    [CommunityGrantState.already, {icon: 'how_to_reg', tone: 'info'}],
    [CommunityGrantState.wrongInstallation, {icon: 'link_off', tone: 'info'}]
  ]);

export const communityGrantStateTitles = new Map<CommunityGrantState, string>([
  [CommunityGrantState.awaiting, 'ce-grant.awaiting-title'],
  [CommunityGrantState.verifying, 'ce-grant.verifying-title'],
  [CommunityGrantState.registered, 'ce-grant.registered-title'],
  [CommunityGrantState.review, 'ce-grant.review-title'],
  [CommunityGrantState.offline, 'ce-grant.offline-title'],
  [CommunityGrantState.verify, 'ce-grant.verify-title'],
  [CommunityGrantState.result, 'ce-grant.result-title'],
  [CommunityGrantState.handedOver, 'ce-grant.result-title'],
  [CommunityGrantState.expired, 'ce-grant.expired-title'],
  [CommunityGrantState.already, 'ce-grant.already-title'],
  [CommunityGrantState.wrongInstallation, 'ce-grant.wrong-installation-title']
]);

export enum CommunityGrantCardState {
  progress = 'progress',
  status = 'status',
  offer = 'offer',
  upToDate = 'upToDate',
  /** A read is still out, so nothing is rendered. */
  pending = 'pending'
}

export type CommunityGrantCardBody = CommunityGrantCardState.progress | CommunityGrantCardState.status;

export interface CommunityGrantCardConfig {
  body: CommunityGrantCardBody;
  title: string;
  text: string;
  action?: string;
  registrationLink?: string;
  /** Label for a link that reopens this dialog, so it carries no external-link arrow. */
  linkAction?: string;
  held?: boolean;
  waiting?: boolean;
  done?: boolean;
}

/** `intro` and `expired` have no entry on purpose: the card falls through to the release offer. */
export const communityGrantCardConfigs = new Map<CommunityGrantState, CommunityGrantCardConfig>([
  [CommunityGrantState.awaiting, {
    body: CommunityGrantCardState.progress,
    title: 'ce-grant.card-progress-title',
    text: 'ce-grant.card-awaiting-text',
    action: 'ce-grant.action-continue'
  }],
  [CommunityGrantState.verifying, {
    body: CommunityGrantCardState.progress,
    title: 'ce-grant.card-progress-title',
    text: 'ce-grant.card-verifying-text',
    action: 'ce-grant.action-continue'
  }],
  [CommunityGrantState.offline, {
    body: CommunityGrantCardState.progress,
    title: 'ce-grant.card-progress-title',
    text: 'ce-grant.card-offline-text',
    action: 'ce-grant.action-continue'
  }],
  [CommunityGrantState.verify, {
    body: CommunityGrantCardState.progress,
    title: 'ce-grant.card-verify-title',
    text: 'ce-grant.card-verify-text',
    action: 'ce-grant.action-verify-deployment',
    waiting: true
  }],
  [CommunityGrantState.result, {
    body: CommunityGrantCardState.progress,
    title: 'ce-grant.card-result-title',
    text: 'ce-grant.card-result-text',
    action: 'ce-grant.action-return-report',
    waiting: true
  }],
  [CommunityGrantState.handedOver, {
    body: CommunityGrantCardState.status,
    title: 'ce-grant.card-handed-over-title',
    text: 'ce-grant.card-handed-over-text',
    registrationLink: 'ce-grant.card-handed-over-link',
    linkAction: 'ce-grant.action-open-report',
    done: true
  }],
  [CommunityGrantState.registered, {
    body: CommunityGrantCardState.status,
    title: 'ce-grant.card-registered-title',
    text: 'ce-grant.card-registered-text',
    registrationLink: 'ce-grant.card-registered-link',
    done: true
  }],
  [CommunityGrantState.review, {
    body: CommunityGrantCardState.status,
    title: 'ce-grant.card-review-title',
    text: 'ce-grant.card-review-text',
    done: true
  }],
  [CommunityGrantState.already, {
    body: CommunityGrantCardState.status,
    title: 'ce-grant.card-already-title',
    text: 'ce-grant.card-already-text-no-mask',
    action: 'ce-grant.action-see-options',
    held: true
  }],
  // Terminal: without an entry the card would fall through to the release offer.
  [CommunityGrantState.wrongInstallation, {
    body: CommunityGrantCardState.status,
    title: 'ce-grant.card-wrong-installation-title',
    text: 'ce-grant.card-wrong-installation-text',
    action: 'ce-grant.action-see-details',
    held: true
  }]
]);

export const communityGrantRailLabels = new Map<CommunityGrantRailKind, string[]>([
  ['online', ['ce-grant.step-account-and-terms', 'ce-grant.step-verification', 'ce-grant.step-ce-grant']],
  ['offline', ['ce-grant.step-account-and-terms', 'ce-grant.step-verification', 'ce-grant.step-report-back']]
]);

export interface CommunityGrantRegistration {
  state: CommunityGrantState;
  mode?: CommunityGrantBackendMode;
  /** Kept because {@link mapCommunityGrantState} is many-to-one and the dialog needs the raw state. */
  backendState?: CommunityGrantBackendState;
  parkReason?: CommunityGrantParkReason;
  portalUrl?: string;
  /** A bearer credential: it identifies this registration wherever it is opened. */
  registrationLink?: string;
  report?: string;
  offlineRunInProgress?: boolean;
  offlineRunError?: string;
}

/** How often the dialog re-reads this instance's `GET /state`. */
export const communityGrantPollInterval = 3000;

export const communityGrantStallTimeout = 30000;

/** Used when the server supplies no portal URL or sign-up link. */
export const communityGrantPortalUrl = 'https://license.thingsboard.io';

/** Mirrors the backend's `CommunityGrantState`. */
export enum CommunityGrantBackendState {
  NOT_STARTED = 'NOT_STARTED',
  AWAITING_SIGNUP = 'AWAITING_SIGNUP',
  COLLECTING = 'COLLECTING',
  VALIDATING = 'VALIDATING',
  REPORT_HANDED_OVER = 'REPORT_HANDED_OVER',
  REGISTERED = 'REGISTERED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  ALREADY_REGISTERED = 'ALREADY_REGISTERED',
  WRONG_INSTALLATION = 'WRONG_INSTALLATION',
  RESUME = 'RESUME'
}

/** Mirrors the backend's `CommunityGrantMode`: the server's own reachability to the portal. */
export enum CommunityGrantBackendMode {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE'
}

/** Mirrors the backend's `CommunityGrantParkReason`; set only with `RESUME`. */
export enum CommunityGrantParkReason {
  LINK_EXPIRED = 'LINK_EXPIRED',
  SIGNUP_ABANDONED = 'SIGNUP_ABANDONED',
  CHECK_FAILED = 'CHECK_FAILED',
  PORTAL_UNREACHABLE = 'PORTAL_UNREACHABLE'
}

/** Mirrors the backend's `CommunityGrantStateInfo`. */
export interface CommunityGrantStateInfo {
  mode: CommunityGrantBackendMode;
  state: CommunityGrantBackendState;
  signUpUrl?: string;
  lastPolledAt?: number;
  parkReason?: CommunityGrantParkReason;
  portalUrl?: string;
  /** Present only while the flow sits on the report step; `GET /offline/report` serves it otherwise. */
  offlineReport?: string;
  offlineRunInProgress?: boolean;
  offlineRunError?: string;
}

/** Mirrors the backend's `CommunityGrantState.isPollable()`. */
const communityGrantPollableBackendStates: readonly CommunityGrantBackendState[] = [
  CommunityGrantBackendState.AWAITING_SIGNUP,
  CommunityGrantBackendState.COLLECTING,
  CommunityGrantBackendState.VALIDATING,
  CommunityGrantBackendState.UNDER_REVIEW,
  CommunityGrantBackendState.REPORT_HANDED_OVER
];

/**
 * `VALIDATING` + `ONLINE` maps to `verifying` even when a by-hand report exists; the dialog tells the two
 * apart by the report. Throws on a value this build does not know.
 */
export function mapCommunityGrantState(state: CommunityGrantBackendState,
                                       mode: CommunityGrantBackendMode): CommunityGrantState {
  switch (state) {
    case CommunityGrantBackendState.NOT_STARTED:
      return CommunityGrantState.intro;
    case CommunityGrantBackendState.AWAITING_SIGNUP:
      return mapByMode(mode, CommunityGrantState.awaiting, CommunityGrantState.offline);
    case CommunityGrantBackendState.COLLECTING:
      return mapByMode(mode, CommunityGrantState.verifying, CommunityGrantState.verify);
    case CommunityGrantBackendState.VALIDATING:
      return mapByMode(mode, CommunityGrantState.verifying, CommunityGrantState.result);
    case CommunityGrantBackendState.REPORT_HANDED_OVER:
      return CommunityGrantState.handedOver;
    case CommunityGrantBackendState.REGISTERED:
      return CommunityGrantState.registered;
    case CommunityGrantBackendState.UNDER_REVIEW:
      return CommunityGrantState.review;
    case CommunityGrantBackendState.ALREADY_REGISTERED:
      return CommunityGrantState.already;
    case CommunityGrantBackendState.WRONG_INSTALLATION:
      return CommunityGrantState.wrongInstallation;
    case CommunityGrantBackendState.RESUME:
      return CommunityGrantState.expired;
    default:
      return assertNeverCommunityGrantValue('backend Community Grant state', state);
  }
}

function mapByMode(mode: CommunityGrantBackendMode,
                   online: CommunityGrantState,
                   offline: CommunityGrantState): CommunityGrantState {
  switch (mode) {
    case CommunityGrantBackendMode.ONLINE:
      return online;
    case CommunityGrantBackendMode.OFFLINE:
      return offline;
    default:
      return assertNeverCommunityGrantValue('backend Community Grant mode', mode);
  }
}

function assertNeverCommunityGrantValue(what: string, value: never): never {
  throw new Error(`Unhandled ${what}: ${JSON.stringify(value)}`);
}

/** Checked in either mode: offline, `handedOver` re-reads `/state` to no effect. */
export const communityGrantOnlinePollableStates: ReadonlySet<CommunityGrantState> = new Set(
  communityGrantPollableBackendStates.map(state => mapCommunityGrantState(state, CommunityGrantBackendMode.ONLINE))
);

export function communityGrantRegistrationFrom(info: CommunityGrantStateInfo): CommunityGrantRegistration {
  return {
    state: mapCommunityGrantState(info.state, info.mode),
    mode: info.mode,
    backendState: info.state,
    parkReason: info.parkReason,
    portalUrl: info.portalUrl,
    registrationLink: info.signUpUrl,
    report: info.offlineReport,
    offlineRunInProgress: info.offlineRunInProgress,
    offlineRunError: info.offlineRunError
  };
}

/**
 * The `TBICBDL1` enrollment bundle the License Portal writes for one installation:
 *
 * ```
 * offset  width  field         value / constraint
 * 0       8      MAGIC         "TBICBDL1"
 * 8       1      VERSION       0x01
 * 9       4      MANIFEST_LEN  uint32 big-endian, 1..8192
 * 13      4      CHECKER_LEN   uint32 big-endian, >= 1
 * 17      74     RELSIG        a complete TBICSIG1 file, verbatim
 * 91      M      MANIFEST      UTF-8 JSON
 * 91+M    C      CHECKER       the executable image
 * ```
 */
const COMMUNITY_GRANT_BUNDLE_MAGIC = 'TBICBDL1';
const COMMUNITY_GRANT_BUNDLE_VERSION = 1;
const COMMUNITY_GRANT_BUNDLE_HEADER_LEN = 91;
const COMMUNITY_GRANT_BUNDLE_MIN_MANIFEST_LEN = 1;
const COMMUNITY_GRANT_BUNDLE_MAX_MANIFEST_LEN = 8192;

export interface CommunityGrantBundleManifest {
  bundleVersion: number;
  os: string;
  arch: string;
  clusterId: string;
  checkerInput: Record<string, unknown>;
  challengeExpiresAt: number;
  issuedAt: number;
  checkerFileName: string;
}

export enum CommunityGrantBundleErrorCode {
  /** Too short, wrong magic, or an unsupported version. */
  NOT_A_BUNDLE = 'NOT_A_BUNDLE',
  MALFORMED = 'MALFORMED'
}

export class CommunityGrantBundleParseError extends Error {
  constructor(public readonly code: CommunityGrantBundleErrorCode, message: string) {
    super(message);
    this.name = 'CommunityGrantBundleParseError';
  }
}

export interface CommunityGrantBundlePreflight {
  manifest: CommunityGrantBundleManifest;
  /** By this browser's clock. A warning only: the portal's clock decides expiry. */
  challengeExpired: boolean;
}

/**
 * Pre-flight before uploading: reads only the header and manifest, and checks structure alone. The
 * installation, platform and release signature are checked by the server.
 */
export async function peekCommunityGrantBundle(file: File): Promise<CommunityGrantBundlePreflight> {
  if (!file || file.size < COMMUNITY_GRANT_BUNDLE_HEADER_LEN) {
    throw notABundle(`The file is ${file ? file.size : 0} bytes, shorter than a bundle's `
      + `${COMMUNITY_GRANT_BUNDLE_HEADER_LEN}-byte header.`);
  }
  const header = new DataView(await file.slice(0, COMMUNITY_GRANT_BUNDLE_HEADER_LEN).arrayBuffer());
  for (let i = 0; i < COMMUNITY_GRANT_BUNDLE_MAGIC.length; i++) {
    if (header.getUint8(i) !== COMMUNITY_GRANT_BUNDLE_MAGIC.charCodeAt(i)) {
      throw notABundle('The file does not carry the marker every enrollment bundle begins with.');
    }
  }
  const version = header.getUint8(COMMUNITY_GRANT_BUNDLE_MAGIC.length);
  if (version !== COMMUNITY_GRANT_BUNDLE_VERSION) {
    throw notABundle(`The bundle declares format version ${version}, and this server supports version `
      + `${COMMUNITY_GRANT_BUNDLE_VERSION} only.`);
  }
  const manifestLength = header.getUint32(9);
  const checkerLength = header.getUint32(13);
  if (manifestLength < COMMUNITY_GRANT_BUNDLE_MIN_MANIFEST_LEN
    || manifestLength > COMMUNITY_GRANT_BUNDLE_MAX_MANIFEST_LEN) {
    throw malformedBundle(`The bundle declares a manifest of ${manifestLength} bytes, outside the permitted `
      + `range of ${COMMUNITY_GRANT_BUNDLE_MIN_MANIFEST_LEN} to ${COMMUNITY_GRANT_BUNDLE_MAX_MANIFEST_LEN}.`);
  }
  if (checkerLength < 1) {
    throw malformedBundle(`The bundle declares a checker of ${checkerLength} bytes.`);
  }
  const expectedSize = COMMUNITY_GRANT_BUNDLE_HEADER_LEN + manifestLength + checkerLength;
  if (file.size !== expectedSize) {
    throw malformedBundle(`The bundle's declared lengths need a file of ${expectedSize} bytes, but this one `
      + `is ${file.size} bytes.`);
  }
  const manifestBytes = await file
    .slice(COMMUNITY_GRANT_BUNDLE_HEADER_LEN, COMMUNITY_GRANT_BUNDLE_HEADER_LEN + manifestLength)
    .arrayBuffer();
  const manifest = parseCommunityGrantBundleManifest(manifestBytes);
  // Strictly later, as the portal compares: the challenge is still live at the deadline instant.
  return {manifest, challengeExpired: Date.now() > manifest.challengeExpiresAt};
}

function parseCommunityGrantBundleManifest(bytes: ArrayBuffer): CommunityGrantBundleManifest {
  let text: string;
  try {
    // `fatal`, so binary fails here as unreadable text rather than later as bad JSON.
    text = new TextDecoder('utf-8', {fatal: true}).decode(bytes);
  } catch {
    throw malformedBundle('The bundle\'s manifest is not readable text.');
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(text);
  } catch {
    throw malformedBundle('The bundle\'s manifest is not valid JSON.');
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw malformedBundle('The bundle\'s manifest is not a JSON object.');
  }
  const candidate = parsed as Record<string, unknown>;
  // Presence and type only; the server judges the values.
  for (const key of ['os', 'arch', 'clusterId', 'checkerFileName']) {
    if (typeof candidate[key] !== 'string' || !(candidate[key] as string).length) {
      throw malformedBundle(`The bundle's manifest is missing "${key}".`);
    }
  }
  const checkerInput = candidate.checkerInput;
  if (!checkerInput || typeof checkerInput !== 'object' || Array.isArray(checkerInput)
    || !Object.keys(checkerInput).length) {
    throw malformedBundle('The bundle\'s manifest is missing "checkerInput".');
  }
  for (const key of ['bundleVersion', 'challengeExpiresAt', 'issuedAt']) {
    if (typeof candidate[key] !== 'number' || !Number.isFinite(candidate[key] as number)) {
      throw malformedBundle(`The bundle's manifest is missing "${key}".`);
    }
  }
  if (candidate.bundleVersion !== COMMUNITY_GRANT_BUNDLE_VERSION) {
    throw notABundle(`The bundle's manifest declares version ${candidate.bundleVersion}.`);
  }
  return candidate as unknown as CommunityGrantBundleManifest;
}

function notABundle(message: string): CommunityGrantBundleParseError {
  return new CommunityGrantBundleParseError(CommunityGrantBundleErrorCode.NOT_A_BUNDLE, message);
}

function malformedBundle(message: string): CommunityGrantBundleParseError {
  return new CommunityGrantBundleParseError(CommunityGrantBundleErrorCode.MALFORMED, message);
}

/** A `text` request's error body arrives as an unparsed JSON string, so it is parsed first. */
function communityGrantErrorHasServerMessage(error: HttpErrorResponse, responseType?: string): boolean {
  let body: unknown = null;
  if (responseType === 'text') {
    try {
      body = error.error ? JSON.parse(error.error) : null;
    } catch {
      body = null;
    }
  } else {
    body = error.error;
  }
  return !!(body && typeof body === 'object' && (body as { message?: unknown }).message);
}

/**
 * The server's own message, or the generic one when it sent none — never `HttpErrorResponse.message`, which
 * names the request path. Pass the request's `responseType`.
 */
export function resolveCommunityGrantErrorMessage(error: HttpErrorResponse,
                                                   translate: TranslateService,
                                                   responseType?: string): string {
  if (!communityGrantErrorHasServerMessage(error, responseType)) {
    return translate.instant('ce-grant.register-error-generic');
  }
  return parseHttpErrorMessage(error, translate, responseType).message;
}
