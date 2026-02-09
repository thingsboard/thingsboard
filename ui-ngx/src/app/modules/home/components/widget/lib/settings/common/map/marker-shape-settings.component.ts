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
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { MarkerIconSettings, MarkerShapeSettings, MarkerType } from '@shared/models/widget/maps/map.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeHtml, SafeUrl } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import {
  createColorMarkerIconElement,
  createColorMarkerShapeURI
} from '@shared/models/widget/maps/marker-shape.models';
import tinycolor from 'tinycolor2';
import { map, share } from 'rxjs/operators';
import { MarkerShapesComponent } from '@home/components/widget/lib/settings/common/map/marker-shapes.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  MarkerIconShapesComponent
} from '@home/components/widget/lib/settings/common/map/marker-icon-shapes.component';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DatasourceType } from '@shared/models/widget.models';

@Component({
    selector: 'tb-marker-shape-settings',
    templateUrl: './marker-shape-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MarkerShapeSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MarkerShapeSettingsComponent implements ControlValueAccessor, OnInit {

  MarkerType = MarkerType;

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
  markerType: MarkerType;

  @Input()
  @coerceBoolean()
  trip = false;

  modelValue: MarkerShapeSettings | MarkerIconSettings;

  public shapeSettingsFormGroup: UntypedFormGroup;

  public shapePreview$: Observable<SafeUrl>;
  public iconPreview$: Observable<SafeHtml>;

  private propagateChange: (v: any) => void = () => { };

  constructor(private popoverService: TbPopoverService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private iconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
    this.shapeSettingsFormGroup = this.fb.group({
      size: [null, [Validators.required, Validators.min(1)]],
      color: [null, [Validators.required]]
    });
    if (this.markerType === MarkerType.shape) {
      this.shapeSettingsFormGroup.addControl('shape', this.fb.control(null, [Validators.required]));
    }
    if (this.markerType === MarkerType.icon) {
      this.shapeSettingsFormGroup.addControl('iconContainer', this.fb.control(null, []));
      this.shapeSettingsFormGroup.addControl('icon', this.fb.control(null, [Validators.required]));
    }
    this.shapeSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.shapeSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.shapeSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MarkerShapeSettings | MarkerIconSettings): void {
    this.modelValue = value;
    this.shapeSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updatePreview();
  }

  openShapePopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      if (this.markerType === MarkerType.shape) {
        const ctx: any = {
          shape: (this.modelValue as MarkerShapeSettings).shape,
          color: this.modelValue.color.color,
          trip: this.trip
        };
        const markerShapesPopover = this.popoverService.displayPopover(trigger, this.renderer,
          this.viewContainerRef, MarkerShapesComponent, 'left', true, null,
          ctx,
          {},
          {}, {}, true);
        markerShapesPopover.tbComponentRef.instance.popover = markerShapesPopover;
        markerShapesPopover.tbComponentRef.instance.markerShapeSelected.subscribe((shape) => {
          markerShapesPopover.hide();
          this.shapeSettingsFormGroup.get('shape').patchValue(
            shape
          );
        });
      } else if (this.markerType === MarkerType.icon) {
        const markerIconShapesPopover = this.popoverService.displayPopover({
          trigger,
          renderer: this.renderer,
          componentType: MarkerIconShapesComponent,
          hostView: this.viewContainerRef,
          preferredPlacement: 'left',
          context: {
            iconContainer: (this.modelValue as MarkerIconSettings).iconContainer,
            icon: (this.modelValue as MarkerIconSettings).icon,
            color: this.modelValue.color.color,
            trip: this.trip
          },
          isModal: true
        });
        markerIconShapesPopover.tbComponentRef.instance.popover = markerIconShapesPopover;
        markerIconShapesPopover.tbComponentRef.instance.markerIconSelected.subscribe((iconInfo) => {
          markerIconShapesPopover.hide();
          this.shapeSettingsFormGroup.get('iconContainer').patchValue(
            iconInfo.iconContainer, {emitEvent: false}
          );
          this.shapeSettingsFormGroup.get('icon').patchValue(
            iconInfo.icon, {emitEvent: false}
          );
          this.updateModel();
        });
      }
    }
  }

  private updateModel() {
    this.modelValue = this.shapeSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
    this.updatePreview();
  }

  private updatePreview() {
    const color = this.modelValue.color.color;
    if (this.markerType === MarkerType.shape) {
      const shape = (this.modelValue as MarkerShapeSettings).shape;
      this.shapePreview$ = createColorMarkerShapeURI(this.iconRegistry, this.domSanitizer, shape, tinycolor(color)).pipe(
        map((url) => {
          return this.domSanitizer.bypassSecurityTrustUrl(url);
        }),
        share()
      );
    } else if (this.markerType === MarkerType.icon) {
      const iconContainer = (this.modelValue as MarkerIconSettings).iconContainer;
      const icon = (this.modelValue as MarkerIconSettings).icon;
      this.iconPreview$ = createColorMarkerIconElement(this.iconRegistry, this.domSanitizer, iconContainer, icon, tinycolor(color)).pipe(
        map((element) => {
          return this.domSanitizer.bypassSecurityTrustHtml(element.outerHTML);
        }),
        share()
      );
    }
  }
}
