///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Inject, InjectionToken, OnInit, ViewContainerRef } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { OverlayRef } from '@angular/cdk/overlay';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import { TranslateService } from '@ngx-translate/core';

export interface TimezonePanelData {
  timezone: string;
  isEdit: boolean;
}

export const TIMEZONE_PANEL_DATA = new InjectionToken<any>('TimezonePanelData');

@Component({
  selector: 'tb-timezone-panel',
  templateUrl: './timezone-panel.component.html',
  styleUrls: ['./timezone-panel.component.scss']
})
export class TimezonePanelComponent extends PageComponent implements OnInit {

  timezone: string;
  result: string;

  timezoneForm: FormGroup;

  constructor(@Inject(TIMEZONE_PANEL_DATA) public data: TimezonePanelData,
              public overlayRef: OverlayRef,
              protected store: Store<AppState>,
              public fb: FormBuilder,
              private timeService: TimeService,
              private translate: TranslateService,
              public viewContainerRef: ViewContainerRef) {
    super(store);
    this.timezone = this.data.timezone;
    this.timezoneForm = this.fb.group({
      timezone: [this.timezone]
    });
  }

  ngOnInit(): void {
  }

  update() {
    this.result = this.timezoneForm.get('timezone').value;
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }

}
