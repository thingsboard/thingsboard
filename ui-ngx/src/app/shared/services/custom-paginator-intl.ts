///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class CustomPaginatorIntl implements MatPaginatorIntl {
  constructor(private translate: TranslateService) {}
  changes = new Subject<void>();

  firstPageLabel = this.translate.instant('paginator.first-page-label');
  itemsPerPageLabel = this.translate.instant('paginator.items-per-page');
  lastPageLabel = this.translate.instant('paginator.last-page-label');

  nextPageLabel = this.translate.instant('paginator.next-page-label');
  previousPageLabel = this.translate.instant('paginator.previous-page-label');
  separator = this.translate.instant('paginator.items-per-page-separator');

  getRangeLabel(page: number, pageSize: number, length: number): string {
    const startNumber = page * pageSize + 1;
    const endNumber = pageSize * (page + 1);
    return `${startNumber} – ${endNumber > length ? length : endNumber}  ${this.separator} ${length}`;
  }
}
