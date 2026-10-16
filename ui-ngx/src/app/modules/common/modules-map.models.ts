// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Observable } from 'rxjs';

export interface IModulesMap {
  init(): Observable<any>;
}
