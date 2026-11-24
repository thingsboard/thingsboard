// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, shareReplay } from 'rxjs/operators';
import { AdminService } from '@core/http/admin.service';
import { FeaturesInfo, UpdateMessage } from '@shared/models/settings.models';

let hiddenRelease: string | null = null;

@Injectable()
export class SysAdminHomeService {

  private adminService = inject(AdminService);

  readonly featuresInfo$: Observable<FeaturesInfo | null> = this.adminService.getFeaturesInfo({ignoreErrors: true, ignoreLoading: true}).pipe(
    catchError(() => of(null)),
    shareReplay(1)
  );

  readonly update$: Observable<UpdateMessage | null> = this.adminService.checkUpdates({ignoreErrors: true, ignoreLoading: true}).pipe(
    catchError(() => of(null)),
    shareReplay(1)
  );

  readonly hiddenRelease = signal<string | null>(hiddenRelease);

  setHiddenRelease(version: string | null): void {
    hiddenRelease = version;
    this.hiddenRelease.set(version);
  }
}
