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

import { Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';

export interface TimezoneSelectionResult {
  timezone: string | null;
}

@Component({
    selector: 'tb-timezone-panel',
    templateUrl: './timezone-panel.component.html',
    styleUrls: ['./timezone-panel.component.scss'],
    standalone: false
})
export class TimezonePanelComponent extends PageComponent implements OnInit {

  @Input()
  timezone: string | null;

  @Input()
  userTimezoneByDefault: boolean;

  @Input()
  localBrowserTimezonePlaceholderOnEmpty: boolean;

  @Input()
  defaultTimezone: string;

  @Input()
  onClose: (result: TimezoneSelectionResult | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  timezoneForm: FormGroup;

  constructor(protected store: Store<AppState>,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.timezoneForm = this.fb.group({
      timezone: [this.timezone]
    });
  }

  update() {
    if (this.onClose) {
      this.onClose({
        timezone: this.timezoneForm.get('timezone').value
      });
    }
  }

  cancel() {
    if (this.onClose) {
      this.onClose(null);
    }
  }

}
