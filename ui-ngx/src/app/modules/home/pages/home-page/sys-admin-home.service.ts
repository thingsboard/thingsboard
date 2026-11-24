// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, shareReplay } from 'rxjs/operators';
import { AdminService } from '@core/http/admin.service';
import { FeaturesInfo } from '@shared/models/settings.models';

@Injectable()
export class SysAdminHomeService {

  readonly featuresInfo$: Observable<FeaturesInfo | null> = inject(AdminService).getFeaturesInfo({ignoreErrors: true, ignoreLoading: true}).pipe(
    catchError(() => of(null)),
    shareReplay(1)
  );
}
