// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { Observable } from 'rxjs';
import { Filter } from '@shared/models/query/query.models';

export interface FilterSelectCallbacks {
  createFilter: (filter: string) => Observable<Filter>;
}
