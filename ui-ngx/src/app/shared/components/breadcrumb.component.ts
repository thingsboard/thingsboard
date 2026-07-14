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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input, OnDestroy,
  OnInit,
  TemplateRef
} from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { BreadCrumb } from './breadcrumb';
import { BroadcastService } from '@core/services/broadcast.service';
import { UtilsService } from '@core/services/utils.service';
import { BreadcrumbService } from '@core/services/breadcrumb.service';

@Component({
    selector: 'tb-breadcrumb',
    templateUrl: './breadcrumb.component.html',
    styleUrls: ['./breadcrumb.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class BreadcrumbComponent implements OnInit, OnDestroy {

  @Input()
  componentBreadcrumbsTpl?: TemplateRef<any>;

  breadcrumbs$: Observable<BreadCrumb[]> = this.breadcrumbService.breadcrumbs$;
  lastBreadcrumb$: Observable<BreadCrumb> = this.breadcrumbService.lastBreadcrumb$;

  private updateBreadcrumbsSubscription: Subscription;

  constructor(private broadcast: BroadcastService,
              private breadcrumbService: BreadcrumbService,
              private cd: ChangeDetectorRef,
              public utils: UtilsService) {
  }

  ngOnInit(): void {
    this.updateBreadcrumbsSubscription = this.broadcast.on('updateBreadcrumb', () => {
      this.cd.markForCheck();
    });
  }

  ngOnDestroy(): void {
    if (this.updateBreadcrumbsSubscription) {
      this.updateBreadcrumbsSubscription.unsubscribe();
    }
  }
}
