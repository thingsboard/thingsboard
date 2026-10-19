// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { TrendzAnalyticsRoutingModule } from '@home/pages/trendz-analytics/trendz-analytics-routing.module';
import { TrendzAnalyticsComponent } from '@home/pages/trendz-analytics/trendz-analytics.component';
import { TrendzAnalyticsUnavailableComponent } from '@home/pages/trendz-analytics/trendz-analytics-unavailable.component';
import { RequestTrendzComponent } from '@home/pages/trendz-analytics/request-trendz.component';

@NgModule({
  declarations: [
    TrendzAnalyticsComponent,
    TrendzAnalyticsUnavailableComponent,
    RequestTrendzComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    TrendzAnalyticsRoutingModule,
  ]
})
export class TrendzAnalyticsModule { }
