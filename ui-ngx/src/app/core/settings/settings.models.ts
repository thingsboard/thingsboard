// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
export interface SettingsState {
  userLang: string;
  reload?: boolean;
  ignoredLoad?: boolean;
}
