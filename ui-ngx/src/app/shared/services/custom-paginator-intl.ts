// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
