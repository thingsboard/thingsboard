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
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  BackgroundSettings,
  backgroundStyle,
  BackgroundType,
  ComponentStyle,
  overlayStyle, validateAndUpdateBackgroundSettings
} from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  BackgroundSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/background-settings-panel.component';
import { Observable, of, pipe } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { tap } from 'rxjs/operators';

@Component({
    selector: 'tb-background-settings',
    templateUrl: './background-settings.component.html',
    styleUrls: ['./background-settings.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => BackgroundSettingsComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class BackgroundSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled = false;

  backgroundType = BackgroundType;

  modelValue: BackgroundSettings;

  backgroundStyle$: Observable<ComponentStyle>;

  overlayStyle: ComponentStyle = {};

  private propagateChange = null;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabled !== isDisabled) {
      this.disabled = isDisabled;
      this.updateBackgroundStyle();
    }
  }

  writeValue(value: BackgroundSettings): void {
    this.modelValue = validateAndUpdateBackgroundSettings(value);
    this.updateBackgroundStyle();
  }

  openBackgroundSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
     const backgroundSettingsPanelPopover = this.popoverService.displayPopover({
       trigger,
       renderer: this.renderer,
       componentType: BackgroundSettingsPanelComponent,
       hostView: this.viewContainerRef,
       preferredPlacement: 'left',
       context: {
         backgroundSettings: this.modelValue
       },
       isModal: true
     });
      backgroundSettingsPanelPopover.tbComponentRef.instance.popover = backgroundSettingsPanelPopover;
      backgroundSettingsPanelPopover.tbComponentRef.instance.backgroundSettingsApplied.subscribe((backgroundSettings) => {
        backgroundSettingsPanelPopover.hide();
        this.modelValue = backgroundSettings;
        this.updateBackgroundStyle();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateBackgroundStyle() {
    if (!this.disabled) {
      this.backgroundStyle$ = backgroundStyle(this.modelValue, this.imagePipe, this.sanitizer,  true);
      this.overlayStyle = overlayStyle(this.modelValue.overlay);
    } else {
      this.backgroundStyle$ = of({});
      this.overlayStyle = {};
    }
    this.cd.markForCheck();
  }

}
