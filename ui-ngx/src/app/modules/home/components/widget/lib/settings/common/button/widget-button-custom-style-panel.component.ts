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
  defaultMainColorDisabled,
  WidgetButtonAppearance,
  WidgetButtonCustomStyle,
  WidgetButtonState,
  widgetButtonStates,
  widgetButtonStatesTranslations,
  WidgetButtonType
} from '@shared/components/button/widget-button.models';
import { merge } from 'rxjs';
import { deepClone } from '@core/utils';
import { WidgetButtonComponent } from '@shared/components/button/widget-button.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-widget-button-custom-style-panel',
    templateUrl: './widget-button-custom-style-panel.component.html',
    providers: [],
    styleUrls: ['./widget-button-custom-style-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class WidgetButtonCustomStylePanelComponent extends PageComponent implements OnInit {

  @ViewChild('widgetButtonPreview')
  widgetButtonPreview: WidgetButtonComponent;

  @Input()
  appearance: WidgetButtonAppearance;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  state: WidgetButtonState;

  @Input()
  customStyle: WidgetButtonCustomStyle;

  private popoverValue: TbPopoverComponent<WidgetButtonCustomStylePanelComponent>;

  @Input()
  set popover(popover: TbPopoverComponent<WidgetButtonCustomStylePanelComponent>) {
    this.popoverValue = popover;
    popover.tbAnimationDone.subscribe(() => {
      this.widgetButtonPreview?.validateSize();
    });
  }

  get popover(): TbPopoverComponent<WidgetButtonCustomStylePanelComponent> {
    return this.popoverValue;
  }

  @Output()
  customStyleApplied = new EventEmitter<WidgetButtonCustomStyle>();

  widgetButtonStateTranslationMap = widgetButtonStatesTranslations;

  widgetButtonState = WidgetButtonState;

  previewAppearance: WidgetButtonAppearance;

  copyFromStates: WidgetButtonState[];

  customStyleFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.copyFromStates = widgetButtonStates.filter(state =>
      state !== this.state && !!this.appearance.customStyle[state]);
    this.customStyleFormGroup = this.fb.group(
      {
        overrideMainColor: [false, []],
        mainColor: [null, []],
        overrideBackgroundColor: [false, []],
        backgroundColor: [null, []],
        overrideDropShadow: [false, []],
        dropShadow: [false, []]
      }
    );
    merge(this.customStyleFormGroup.get('overrideMainColor').valueChanges,
          this.customStyleFormGroup.get('overrideBackgroundColor').valueChanges,
          this.customStyleFormGroup.get('overrideDropShadow').valueChanges
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

  copyStyle(state: WidgetButtonState) {
    this.customStyle = deepClone(this.appearance.customStyle[state]);
    this.setStyle(this.customStyle);
    this.customStyleFormGroup.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyCustomStyle() {
    const customStyle: WidgetButtonCustomStyle = this.customStyleFormGroup.value;
    this.customStyleApplied.emit(customStyle);
  }

  private setStyle(customStyle?: WidgetButtonCustomStyle): void {
    let mainColor = this.state === WidgetButtonState.disabled ? defaultMainColorDisabled : this.appearance.mainColor;
    if (customStyle?.overrideMainColor) {
      mainColor = customStyle?.mainColor;
    }
    let backgroundColor = this.state === WidgetButtonState.disabled ? defaultBackgroundColorDisabled : this.appearance.backgroundColor;
    if (customStyle?.overrideBackgroundColor) {
      backgroundColor = customStyle?.backgroundColor;
    }
    let dropShadow = this.appearance.type !== WidgetButtonType.basic;
    if (customStyle?.overrideDropShadow) {
      dropShadow = customStyle?.dropShadow;
    }
    this.customStyleFormGroup.patchValue({
      overrideMainColor: customStyle?.overrideMainColor,
      mainColor,
      overrideBackgroundColor: customStyle?.overrideBackgroundColor,
      backgroundColor,
      overrideDropShadow: customStyle?.overrideDropShadow,
      dropShadow
    }, {emitEvent: false});
    this.updateValidators();
    this.updatePreviewAppearance();
  }

  private updateValidators() {
    const overrideMainColor: boolean = this.customStyleFormGroup.get('overrideMainColor').value;
    const overrideBackgroundColor: boolean = this.customStyleFormGroup.get('overrideBackgroundColor').value;
    const overrideDropShadow: boolean = this.customStyleFormGroup.get('overrideDropShadow').value;

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
    if (overrideDropShadow) {
      this.customStyleFormGroup.get('dropShadow').enable({emitEvent: false});
    } else {
      this.customStyleFormGroup.get('dropShadow').disable({emitEvent: false});
    }
  }

  private updatePreviewAppearance() {
    this.previewAppearance = deepClone(this.appearance);
    this.previewAppearance.customStyle[this.state] = this.customStyleFormGroup.value;
    this.cd.markForCheck();
  }
}
