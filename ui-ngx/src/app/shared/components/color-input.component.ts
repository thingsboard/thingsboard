///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DialogService } from '@core/services/dialog.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbPopoverService } from '@shared/components/popover.service';
import { ColorPickerPanelComponent } from '@shared/components/color-picker/color-picker-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-color-input',
  templateUrl: './color-input.component.html',
  styleUrls: ['./color-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorInputComponent),
      multi: true
    }
  ]
})
export class ColorInputComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  @coerceBoolean()
  asBoxInput = false;

  @Input()
  icon: string;

  @Input()
  label: string;

  @Input()
  requiredText: string;

  @Input()
  @coerceBoolean()
  colorClearButton = false;

  @Input()
  @coerceBoolean()
  openOnInput = false;

  @Input()
  @coerceBoolean()
  noBorder = false;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  readonly = false;

  private modelValue: string;

  private propagateChange = null;

  public colorFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private dialogs: DialogService,
              private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.colorFormGroup = this.fb.group({
      color: [null, this.required ? [Validators.required] : []]
    });

    this.colorFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  updateValidators() {
    if (this.colorFormGroup) {
      this.colorFormGroup.get('color').setValidators(this.required ? [Validators.required] : []);
      this.colorFormGroup.get('color').updateValueAndValidity();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.colorFormGroup.disable({emitEvent: false});
    } else {
      this.colorFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.colorFormGroup.patchValue(
      { color: this.modelValue }, {emitEvent: false}
    );
  }

  private updateModel() {
    const color: string = this.colorFormGroup.get('color').value;
    if (this.modelValue !== color) {
      this.modelValue = color;
      this.propagateChange(this.modelValue);
    }
  }

  showColorPicker($event: MouseEvent) {
    $event.stopPropagation();
    if (!this.disabled && !this.readonly) {
      this.dialogs.colorPicker(this.colorFormGroup.get('color').value,
          this.colorClearButton).subscribe(
          (result) => {
            if (!result?.canceled) {
              this.colorFormGroup.patchValue(
                  {color: result?.color}, {emitEvent: true}
              );
              this.cd.markForCheck();
            }
          }
      );
    }
  }

  openColorPickerPopup($event: Event, element?: ElementRef) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.disabled && !this.readonly) {
      const trigger = element ? element.nativeElement : $event.target;
      if (this.popoverService.hasPopover(trigger)) {
        this.popoverService.hidePopover(trigger);
      } else {
        const colorPickerPopover = this.popoverService.displayPopover({
          trigger,
          renderer: this.renderer,
          hostView: this.viewContainerRef,
          componentType: ColorPickerPanelComponent,
          preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
          context: {
            color: this.colorFormGroup.get('color').value,
            colorClearButton: this.colorClearButton,
            colorCancelButton: true
          },
          showCloseButton: false,
          popoverContentStyle: {padding: '12px 4px 12px 12px'},
          isModal: true
        })
        colorPickerPopover.tbComponentRef.instance.popover = colorPickerPopover;
        colorPickerPopover.tbComponentRef.instance.colorSelected.subscribe((color) => {
          colorPickerPopover.hide();
          this.colorFormGroup.patchValue(
              {color}, {emitEvent: true}
          );
          this.cd.markForCheck();
        });
      }
    }
  }

  clear() {
    this.colorFormGroup.get('color').patchValue(null, {emitEvent: true});
    this.cd.markForCheck();
  }
}
