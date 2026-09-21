// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export class InterceptorConfig {
  constructor(public ignoreLoading: boolean = false,
              public ignoreErrors: boolean = false,
              public ignoreVersionConflict: boolean = false,
              public resendRequest: boolean = false) {}
}
