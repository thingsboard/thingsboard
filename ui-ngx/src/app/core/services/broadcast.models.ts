// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface BroadcastMessage {
  name: string;
  args?: Array<any>;
}

export interface BroadcastEvent {
  name: string;
}

export type BroadcastListener = (event: BroadcastEvent, ...args: Array<any>) => void;
