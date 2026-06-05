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
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ShapeFillImageSettings, ShapeFillImageType } from '@shared/models/widget/maps/map.models';

@Component({
    selector: 'tb-shape-fill-image-settings-panel',
    templateUrl: './shape-fill-image-settings-panel.component.html',
    providers: [],
    styleUrls: ['./shape-fill-image-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ShapeFillImageSettingsPanelComponent implements OnInit {

  @Input()
  shapeFillImageSettings: ShapeFillImageSettings;

  @Output()
  shapeFillImageSettingsApplied = new EventEmitter<ShapeFillImageSettings>();

  ShapeFillImageType = ShapeFillImageType;

  shapeFillImageSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private popover: TbPopoverComponent,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.shapeFillImageSettingsFormGroup = this.fb.group(
      {
        type: [this.shapeFillImageSettings?.type || ShapeFillImageType.image, []],
        image: [this.shapeFillImageSettings?.image, [Validators.required]],
        preserveAspectRatio: [this.shapeFillImageSettings?.preserveAspectRatio, []],
        opacity: [this.shapeFillImageSettings?.opacity, [Validators.min(0), Validators.max(1)]],
        angle: [this.shapeFillImageSettings?.angle, [Validators.min(0), Validators.max(360)]],
        scale: [this.shapeFillImageSettings?.scale, [Validators.min(0)]],
        imageFunction: [this.shapeFillImageSettings?.imageFunction, [Validators.required]],
        images: [this.shapeFillImageSettings?.images, []]
      }
    );
    this.shapeFillImageSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyShapeFillImageSettings() {
    const shapeFillImageSettings: ShapeFillImageSettings = this.shapeFillImageSettingsFormGroup.value;
    this.shapeFillImageSettingsApplied.emit(shapeFillImageSettings);
    this.popover?.hide();
  }

  private updateValidators() {
    const type: ShapeFillImageType = this.shapeFillImageSettingsFormGroup.get('type').value;
    if (type === ShapeFillImageType.image) {
      this.shapeFillImageSettingsFormGroup.get('image').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('preserveAspectRatio').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('opacity').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('angle').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('scale').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('imageFunction').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('images').disable({emitEvent: false});
    } else {
      this.shapeFillImageSettingsFormGroup.get('image').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('preserveAspectRatio').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('angle').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('scale').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('imageFunction').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('images').enable({emitEvent: false});
    }
  }

}
