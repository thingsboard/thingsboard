// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { InjectionToken } from '@angular/core';
import { ComponentType } from '@angular/cdk/portal';

export const EMBED_DASHBOARD_DIALOG_TOKEN: InjectionToken<ComponentType<any>> =
  new InjectionToken<ComponentType<any>>('EMBED_DASHBOARD_DIALOG_TOKEN');

