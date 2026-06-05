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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { defaultTimewindowStyle, TimewindowStyle } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Timewindow } from '@shared/models/time/time.models';
import { deepClone } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-timewindow-style-panel',
    templateUrl: './timewindow-style-panel.component.html',
    providers: [],
    styleUrls: ['./timewindow-style-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TimewindowStylePanelComponent extends PageComponent implements OnInit {

  @Input()
  timewindowStyle: TimewindowStyle;

  @Input()
  previewValue: Timewindow;

  @Input()
  popover: TbPopoverComponent<TimewindowStylePanelComponent>;

  @Output()
  timewindowStyleApplied = new EventEmitter<TimewindowStyle>();

  timewindowStyleFormGroup: UntypedFormGroup;

  previewTimewindowStyle: TimewindowStyle;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    const computedTimewindowStyle = {...defaultTimewindowStyle, ...(this.timewindowStyle || {})};
    this.timewindowStyleFormGroup = this.fb.group(
      {
        showIcon: [computedTimewindowStyle.showIcon, []],
        iconSize: [computedTimewindowStyle.iconSize, []],
        icon: [computedTimewindowStyle.icon, []],
        iconPosition: [computedTimewindowStyle.iconPosition, []],
        font: [computedTimewindowStyle.font, []],
        color: [computedTimewindowStyle.color, []],
        displayTypePrefix: [computedTimewindowStyle.displayTypePrefix, []]
      }
    );
    this.updatePreviewStyle(this.timewindowStyle);
    this.updateTimewindowStyleEnabledState();
    this.timewindowStyleFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((timewindowStyle: TimewindowStyle) => {
      if (this.timewindowStyleFormGroup.valid) {
        this.updatePreviewStyle(timewindowStyle);
        setTimeout(() => {this.popover?.updatePosition();}, 0);
      }
    });
    this.timewindowStyleFormGroup.get('showIcon').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateTimewindowStyleEnabledState();
    });
  }

  cancel() {
    this.popover?.hide();
  }

  applyTimewindowStyle() {
    const timewindowStyle = this.timewindowStyleFormGroup.getRawValue();
    this.timewindowStyleApplied.emit(timewindowStyle);
  }

  private updateTimewindowStyleEnabledState() {
    const showIcon: boolean = this.timewindowStyleFormGroup.get('showIcon').value;
    if (showIcon) {
      this.timewindowStyleFormGroup.get('iconSize').enable({emitEvent: false});
      this.timewindowStyleFormGroup.get('icon').enable({emitEvent: false});
      this.timewindowStyleFormGroup.get('iconPosition').enable({emitEvent: false});
    } else {
      this.timewindowStyleFormGroup.get('iconSize').disable({emitEvent: false});
      this.timewindowStyleFormGroup.get('icon').disable({emitEvent: false});
      this.timewindowStyleFormGroup.get('iconPosition').disable({emitEvent: false});
    }
  }

  private updatePreviewStyle(timewindowStyle: TimewindowStyle) {
    this.previewTimewindowStyle = deepClone(timewindowStyle);
  }

}
