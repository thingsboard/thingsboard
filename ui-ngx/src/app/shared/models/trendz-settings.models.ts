// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface TrendzSettings {
  enabled: boolean,
  baseUrl: string,
  apiKey: string
}

export const initialTrendzSettings: TrendzSettings = {
  enabled: false,
  baseUrl: null,
  apiKey: null
}
