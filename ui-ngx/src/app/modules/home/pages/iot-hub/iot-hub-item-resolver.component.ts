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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { ListingItemVersionNotFound, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import {
  DeepLinkOpenItem,
  isPublished,
  isUUID,
  typeSegment
} from './iot-hub-deep-link.utils';
import {
  IotHubUnpublishedWarningDialogData,
  TbIotHubUnpublishedWarningDialogComponent
} from '@home/components/iot-hub/iot-hub-unpublished-warning-dialog.component';
import {
  TbIotHubPeRequiredDialogComponent
} from '@home/components/iot-hub/iot-hub-pe-required-dialog.component';
import {
  IotHubUpgradeRequiredDialogData,
  TbIotHubUpgradeRequiredDialogComponent
} from '@home/components/iot-hub/iot-hub-upgrade-required-dialog.component';

@Component({
  selector: 'tb-iot-hub-item-resolver',
  standalone: false,
  template: ''
})
export class TbIotHubItemResolverComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private store: Store<AppState>,
    private translate: TranslateService,
    private iotHubApi: IotHubApiService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.paramMap;
    const slug = params.get('slug');
    const itemVersionId = params.get('itemVersionId');
    const itemId = params.get('itemId');
    const bySlug = slug != null;
    const byVersion = !bySlug && itemVersionId != null;

    let fetch$: Observable<MpItemVersionView>;
    if (bySlug) {
      fetch$ = this.iotHubApi.getListingItemVersion(slug, { ignoreErrors: true });
    } else {
      const id = byVersion ? itemVersionId : itemId;
      if (!isUUID(id)) {
        this.failTo('iot-hub.deep-link-invalid-id');
        return;
      }
      fetch$ = byVersion
        ? this.iotHubApi.getVersionInfo(id, { ignoreErrors: true })
        : this.iotHubApi.getPublishedVersion(id, { ignoreErrors: true });
    }

    fetch$.subscribe({
      next: v => this.handleResolved(v, byVersion),
      error: err => {
        if (bySlug && err?.status === 404) {
          const body = (err?.error ?? {}) as ListingItemVersionNotFound;
          if (body.peRequired) {
            this.showPeRequired();
            return;
          }
          if (typeof body.minTbVersionRequired === 'number') {
            this.showMinTbVersionRequired(body.minTbVersionRequired);
            return;
          }
          if (body.noMatchingVersions) {
            this.failTo('iot-hub.deep-link-not-found');
            return;
          }
        }
        const key = err?.status === 404
          ? 'iot-hub.deep-link-not-found'
          : 'iot-hub.deep-link-fetch-failed';
        this.failTo(key);
      }
    });
  }

  private showPeRequired(): void {
    this.router.navigate(['/iot-hub'], { replaceUrl: true }).then(() => {
      this.dialog.open(TbIotHubPeRequiredDialogComponent, {
        panelClass: ['tb-dialog'],
        disableClose: true,
        autoFocus: false
      });
    });
  }

  private showMinTbVersionRequired(minTbVersion: number): void {
    this.router.navigate(['/iot-hub'], { replaceUrl: true }).then(() => {
      this.dialog.open<TbIotHubUpgradeRequiredDialogComponent, IotHubUpgradeRequiredDialogData>(
        TbIotHubUpgradeRequiredDialogComponent,
        {
          panelClass: ['tb-dialog'],
          disableClose: true,
          autoFocus: false,
          data: { minTbVersion }
        }
      );
    });
  }

  private handleResolved(version: MpItemVersionView, byVersion: boolean): void {
    const segment = typeSegment(version.type);
    if (!segment) {
      this.failTo('iot-hub.deep-link-fetch-failed');
      return;
    }

    const unpublished = byVersion && !isPublished(version);

    if (unpublished) {
      this.dialog.open<
        TbIotHubUnpublishedWarningDialogComponent,
        IotHubUnpublishedWarningDialogData,
        boolean
      >(TbIotHubUnpublishedWarningDialogComponent, {
        panelClass: ['tb-dialog'],
        disableClose: true,
        autoFocus: false,
        data: { item: version }
      }).afterClosed().subscribe(confirmed => {
        if (confirmed) {
          this.openOnTypePage(version, segment, true);
        } else {
          this.router.navigate(['/iot-hub'], { replaceUrl: true });
        }
      });
    } else {
      this.openOnTypePage(version, segment, false);
    }
  }

  private openOnTypePage(version: MpItemVersionView, segment: string, preview: boolean): void {
    const openItem: DeepLinkOpenItem = { version, preview };
    this.router.navigate(['/iot-hub', segment], {
      state: { openItem },
      replaceUrl: true
    });
  }

  private failTo(messageKey: string): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageKey),
      type: 'error',
      duration: 5000
    }));
    this.router.navigate(['/iot-hub'], { replaceUrl: true });
  }
}
