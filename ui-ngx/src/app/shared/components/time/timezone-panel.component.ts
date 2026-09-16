// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
