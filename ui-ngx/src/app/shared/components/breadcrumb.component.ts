// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
