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

  private loadCreator(): void {
    this.iotHubApiService.getCreatorProfile(this.creatorId, { ignoreLoading: true }).subscribe({
      next: creator => this.creator = creator,
      error: () => void this.router.navigate(['/iot-hub'])
    });
  }

}
