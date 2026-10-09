// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { TranslateService } from '@ngx-translate/core';
import { ReportTemplate } from '@shared/models/report.models';

@Component({
    selector: 'tb-report-template-tabs',
    templateUrl: './report-template-tabs.component.html',
    standalone: false
})
export class ReportTemplateTabsComponent extends EntityTabsComponent<ReportTemplate> {

  constructor(protected store: Store<AppState>,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }
}
