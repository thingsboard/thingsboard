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

import { Component, Input, OnInit } from '@angular/core';
import { HistoryWindowType, RealtimeWindowType, TimewindowType } from '@shared/models/time/time.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
  selector: 'tb-interval-options-config-panel',
  templateUrl: './interval-options-config-panel.component.html',
  styleUrls: ['./interval-options-config-panel.component.scss']
})
export class IntervalOptionsConfigPanelComponent implements OnInit {

  @Input()
  allowedIntervals: Array<any>;

  @Input()
  intervalType: RealtimeWindowType | HistoryWindowType;

  @Input()
  timewindowType: TimewindowType;

  @Input()
  onClose: (result: Array<any> | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  intervalOptionsConfigForm: FormGroup;

  intervals = [];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.intervalOptionsConfigForm = this.fb.group({
      allowedIntervals: [this.allowedIntervals]
    });
  }

  update() {
    if (this.onClose) {
      this.onClose([]);
    }
  }

  cancel() {
    if (this.onClose) {
      this.onClose(null);
    }
  }

}
