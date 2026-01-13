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

import { ChangeDetectorRef, Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { MapDataLayerType, ShapeFillStripeSettings } from '@shared/models/widget/maps/map.models';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull, stringToBase64 } from '@core/utils';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DatasourceType } from '@shared/models/widget.models';
import {
  ShapeFillStripeSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-stripe-settings-panel.component';

@Component({
  selector: 'tb-shape-fill-stripe-settings',
  templateUrl: './shape-fill-stripe-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ShapeFillStripeSettingsComponent),
      multi: true
    }
  ]
})
export class ShapeFillStripeSettingsComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  @Input()
  dsType: DatasourceType;

  @Input()
  dsEntityAliasId: string;

  @Input()
  dsDeviceId: string;

  @Input()
  dataLayerType: MapDataLayerType;

  modelValue: ShapeFillStripeSettings;

  stripePreviewUrl: SafeUrl;

  private propagateChange: (v: any) => void = () => { };

  constructor(private popoverService: TbPopoverService,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: ShapeFillStripeSettings): void {
    if (value) {
      this.modelValue = value;
    }
    this.updatePreview();
  }

  openStripeSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ShapeFillStripeSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: {
          shapeFillStripeSettings: this.modelValue,
          context: this.context,
          dsType: this.dsType,
          dsEntityAliasId: this.dsEntityAliasId,
          dsDeviceId: this.dsDeviceId,
          dataLayerType: this.dataLayerType
        },
        isModal: true
      }).tbComponentRef.instance.shapeFillStripeSettingsApplied.subscribe((shapeFillStripeSettings) => {
        this.modelValue = shapeFillStripeSettings;
        this.updatePreview();
        this.propagateChange(this.modelValue);
        this.cd.detectChanges();
      });
    }
  }

  private updatePreview() {
    this.stripePreviewUrl = this.sanitizer.bypassSecurityTrustUrl(generateStripePreviewUrl(this.modelValue));
  }
}

export const generateStripePreviewUrl = (settings: ShapeFillStripeSettings, previewWidth = 48, previewHeight = 48): string => {
  const weight = isDefinedAndNotNull(settings?.weight) ? settings.weight : 3;
  const spaceWeight = isDefinedAndNotNull(settings?.spaceWeight) ? settings.spaceWeight : 9;
  const angle = isDefinedAndNotNull(settings?.angle) ? settings.angle : 45;
  const height = weight + spaceWeight;
  const color = settings?.color?.color || '#8f8f8f';
  const spaceColor = settings?.spaceColor?.color || 'rgba(143,143,143,0)';
  const svgStr = `<svg x="0" y="0" width="${previewWidth}" height="${previewHeight}" viewBox="0 0 ${previewWidth} ${previewHeight}" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="0" y="0" width="${previewWidth}" height="${previewHeight}" fill="url(#stripePattern)" fill-opacity="1"></rect>
        <defs>
          <pattern id="stripePattern" x="0" y="0" width="8" height="${height}" patternUnits="userSpaceOnUse"
                    patternContentUnits="userSpaceOnUse" patternTransform="rotate(${angle})">
              <path d="M0 ${weight/2} H 8" stroke="${color}" stroke-width="${weight}" stroke-opacity="1"></path>
              <path d="M0 ${weight + spaceWeight/2} H 8" stroke="${spaceColor}" stroke-width="${spaceWeight}" stroke-opacity="1"></path>
          </pattern>
        </defs>
      </svg>`;
  const encodedSvg = stringToBase64(svgStr);
  return `data:image/svg+xml;base64,${encodedSvg}`;
}
