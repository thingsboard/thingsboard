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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  defaultBackgroundColorDisabled,
  defaultMainColorDisabled
} from '@shared/components/button/widget-button.models';
import { merge } from 'rxjs';
import { deepClone } from '@core/utils';
import { WidgetButtonToggleComponent } from '@shared/components/button/widget-button-toggle.component';
import {
  ButtonToggleAppearance,
  SegmentedButtonStyles,
  WidgetButtonToggleCustomStyle,
  WidgetButtonToggleState,
  widgetButtonToggleStates,
  widgetButtonToggleStatesTranslations
} from '@home/components/widget/lib/button/segmented-button-widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-widget-button-toggle-custom-style-panel',
  templateUrl: './widget-button-toggle-custom-style-panel.component.html',
  providers: [],
  styleUrls: ['./widget-button-toggle-custom-style-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WidgetButtonToggleCustomStylePanelComponent extends PageComponent implements OnInit {

  @ViewChild('widgetButtonTogglePreview')
  widgetButtonTogglePreview: WidgetButtonToggleComponent;

  @Input()
  appearance: ButtonToggleAppearance;

  @Input()
  value: boolean = false;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  state: WidgetButtonToggleState;

  @Input()
  customStyle: WidgetButtonToggleCustomStyle;

  private popoverValue: TbPopoverComponent<WidgetButtonToggleCustomStylePanelComponent>;

  @Input()
  set popover(popover: TbPopoverComponent<WidgetButtonToggleCustomStylePanelComponent>) {
    this.popoverValue = popover;
    popover.tbAnimationDone.subscribe(() => {
      this.widgetButtonTogglePreview?.validateSize();
    });
  }

  get popover(): TbPopoverComponent<WidgetButtonToggleCustomStylePanelComponent> {
    return this.popoverValue;
  }

  @Output()
  customStyleApplied = new EventEmitter<WidgetButtonToggleCustomStyle>();

  widgetButtonToggleStatesTranslationsMap = widgetButtonToggleStatesTranslations;

  WidgetButtonToggleState = WidgetButtonToggleState;

  previewAppearance: ButtonToggleAppearance;

  copyFromStates: WidgetButtonToggleState[];

  customStyleFormGroup: UntypedFormGroup;

  style: SegmentedButtonStyles;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.style = this.value ? this.appearance.selectedStyle : this.appearance.unselectedStyle;
    this.copyFromStates = widgetButtonToggleStates.filter(state =>
      state !== this.state && !!this.style.customStyle[state]);
    this.customStyleFormGroup = this.fb.group(
      {
        overrideMainColor: [false, []],
        mainColor: [null, []],
        overrideBackgroundColor: [false, []],
        backgroundColor: [null, []],
        overrideBorderColor: [false, []],
        borderColor: [null, []]
      }
    );
    merge(this.customStyleFormGroup.get('overrideMainColor').valueChanges,
      this.customStyleFormGroup.get('overrideBackgroundColor').valueChanges,
      this.customStyleFormGroup.get('overrideBorderColor').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.customStyleFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updatePreviewAppearance();
    });
    this.setStyle(this.customStyle);
  }

  copyStyle(state: WidgetButtonToggleState) {
    this.customStyle = deepClone(this.style[state]);
    this.setStyle(this.customStyle);
    this.customStyleFormGroup.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyCustomStyle() {
    const customStyle: SegmentedButtonStyles = this.customStyleFormGroup.value;
    this.customStyleApplied.emit(customStyle);
  }

  private setStyle(customStyle?: WidgetButtonToggleCustomStyle): void {
    let mainColor = this.state === WidgetButtonToggleState.disabled ? defaultMainColorDisabled : this.style.mainColor;
    if (customStyle?.overrideMainColor) {
      mainColor = customStyle?.mainColor;
    }
    let backgroundColor = this.state === WidgetButtonToggleState.disabled ? defaultBackgroundColorDisabled : this.style.backgroundColor;
    if (customStyle?.overrideBackgroundColor) {
      backgroundColor = customStyle?.backgroundColor;
    }
    let borderColor = this.state === WidgetButtonToggleState.disabled ? defaultBackgroundColorDisabled : this.style.backgroundColor;
    if (customStyle?.overrideBorderColor) {
      borderColor = customStyle?.borderColor;
    }
    this.customStyleFormGroup.patchValue({
      overrideMainColor: customStyle?.overrideMainColor,
      mainColor,
      overrideBackgroundColor: customStyle?.overrideBackgroundColor,
      backgroundColor,
      overrideBorderColor: customStyle?.overrideBorderColor,
      borderColor
    }, {emitEvent: false});
    this.updateValidators();
    this.updatePreviewAppearance();
  }

  private updateValidators() {
    const overrideMainColor: boolean = this.customStyleFormGroup.get('overrideMainColor').value;
    const overrideBackgroundColor: boolean = this.customStyleFormGroup.get('overrideBackgroundColor').value;
    const overrideBorderColor: boolean = this.customStyleFormGroup.get('overrideBorderColor').value;

    if (overrideMainColor) {
      this.customStyleFormGroup.get('mainColor').enable({emitEvent: false});
    } else {
      this.customStyleFormGroup.get('mainColor').disable({emitEvent: false});
    }
    if (overrideBackgroundColor) {
      this.customStyleFormGroup.get('backgroundColor').enable({emitEvent: false});
    } else {
      this.customStyleFormGroup.get('backgroundColor').disable({emitEvent: false});
    }
    if (overrideBorderColor) {
      this.customStyleFormGroup.get('borderColor').enable({emitEvent: false});
    } else {
      this.customStyleFormGroup.get('borderColor').disable({emitEvent: false});
    }
  }

  private updatePreviewAppearance() {
    this.previewAppearance = deepClone(this.appearance);
    if (this.value) {
      this.previewAppearance.selectedStyle.customStyle[this.state] = this.customStyleFormGroup.value;
    } else {
      this.previewAppearance.unselectedStyle.customStyle[this.state] = this.customStyleFormGroup.value;
    }
    this.cd.markForCheck();
  }
}
