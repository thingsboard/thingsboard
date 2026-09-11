// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { InjectionToken, Type } from '@angular/core';
import { ComponentType } from '@angular/cdk/portal';

export const HELP_MARKDOWN_COMPONENT_TOKEN: InjectionToken<ComponentType<any>> =
  new InjectionToken<ComponentType<any>>('HELP_MARKDOWN_COMPONENT_TOKEN');

export const SHARED_MODULE_TOKEN: InjectionToken<Type<any>> =
  new InjectionToken<Type<any>>('SHARED_MODULE_TOKEN');
