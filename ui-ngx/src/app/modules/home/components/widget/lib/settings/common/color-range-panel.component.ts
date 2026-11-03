// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { ColorRange, ColorRangeSettings } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import {
  ColorRangeSettingsComponent
} from '@home/components/widget/lib/settings/common/color-range-settings.component';

@Component({
    selector: 'tb-color-range-panel',
    templateUrl: './color-range-panel.component.html',
    providers: [],
    styleUrls: ['./color-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ColorRangePanelComponent extends PageComponent implements OnInit {

  @Input()
  colorRangeSettings: Array<ColorRange>;

  @Input()
  popover: TbPopoverComponent<ColorRangePanelComponent>;

  @Input()
  settingsComponents: ColorRangeSettingsComponent[];

  @Output()
  colorRangeApplied = new EventEmitter<Array<ColorRange>>();

  colorRangeFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.colorRangeFormGroup = this.fb.group({
        rangeList: [this.colorRangeSettings, []]
    });
  }

  copyColorSettings(comp: ColorRangeSettingsComponent) {
    this.colorRangeSettings = deepClone(comp.modelValue);
    this.colorRangeFormGroup.get('rangeList').patchValue(this.colorRangeSettings || [], {emitEvent: false});
    this.colorRangeFormGroup.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorRangeSettings() {
    const colorRangeSettings: ColorRangeSettings = this.colorRangeFormGroup.get('rangeList').value;
    this.colorRangeApplied.emit(colorRangeSettings.range);
  }

}
