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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CreatorView } from '@shared/models/iot-hub/iot-hub-creator.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';

@Component({
  selector: 'tb-iot-hub-creator-profile',
  standalone: false,
  templateUrl: './iot-hub-creator-profile.component.html',
  styleUrls: ['./iot-hub-creator-profile.component.scss']
})
export class TbIotHubCreatorProfileComponent implements OnInit, OnDestroy {

  creator: CreatorView;
  creatorId: string;
  isLoading = false;
  hasError = false;

  private retryTimer: any = null;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private iotHubApiService: IotHubApiService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.creatorId = params['creatorId'];
      this.loadCreator();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  getAvatarUrl(): string | null {
    return this.creator?.avatarUrl ? this.iotHubApiService.resolveResourceUrl(this.creator.avatarUrl) : null;
  }

  getWebsiteLabel(): string {
    return this.creator?.website?.replace(/^https?:\/\//, '') || '';
  }

  goBack(): void {
    void this.router.navigate(['/iot-hub']);
  }

  retryLoadCreator(): void {
    // Debounce frequent retry clicks: show the loading spinner
    // immediately and defer the actual loadCreator() call by 350ms
    // so rapid-fire clicks coalesce into a single network round-trip.
    // hasError is left as-is until the next request actually succeeds.
    if (this.retryTimer != null) {
      clearTimeout(this.retryTimer);
    }
    this.isLoading = true;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.loadCreator();
    }, 350);
  }

  private loadCreator(): void {
    if (this.retryTimer != null) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    this.isLoading = true;
    // hasError stays as-is until the request actually succeeds —
    // cleared in the `next` callback below.
    this.iotHubApiService.getCreatorProfile(this.creatorId, { ignoreLoading: true, ignoreErrors: true }).subscribe({
      next: creator => {
        this.creator = creator;
        this.isLoading = false;
        this.hasError = false;
      },
      error: () => {
        this.creator = null;
        this.isLoading = false;
        this.hasError = true;
      }
    });
  }

}
