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
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarkerImageSettings, MarkerImageType } from '@shared/models/widget/maps/map.models';

@Component({
    selector: 'tb-marker-image-settings-panel',
    templateUrl: './marker-image-settings-panel.component.html',
    providers: [],
    styleUrls: ['./marker-image-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MarkerImageSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  markerImageSettings: MarkerImageSettings;

  @Input()
  popover: TbPopoverComponent<MarkerImageSettingsPanelComponent>;

  @Output()
  markerImageSettingsApplied = new EventEmitter<MarkerImageSettings>();

  MarkerImageType = MarkerImageType;

  markerImageSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.markerImageSettingsFormGroup = this.fb.group(
      {
        type: [this.markerImageSettings?.type || MarkerImageType.image, []],
        image: [this.markerImageSettings?.image, [Validators.required]],
        imageSize: [this.markerImageSettings?.imageSize, [Validators.min(1)]],
        imageFunction: [this.markerImageSettings?.imageFunction, [Validators.required]],
        images: [this.markerImageSettings?.images, []]
      }
    );
    this.markerImageSettingsFormGroup.get('type').valueChanges.pipe(
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

  applyMarkerImageSettings() {
    const markerImageSettings: MarkerImageSettings = this.markerImageSettingsFormGroup.value;
    this.markerImageSettingsApplied.emit(markerImageSettings);
  }

  private updateValidators() {
    const type: MarkerImageType = this.markerImageSettingsFormGroup.get('type').value;
    if (type === MarkerImageType.image) {
      this.markerImageSettingsFormGroup.get('image').enable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageSize').enable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageFunction').disable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('images').disable({emitEvent: false});
    } else {
      this.markerImageSettingsFormGroup.get('image').disable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageSize').disable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageFunction').enable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('images').enable({emitEvent: false});
    }
  }

}
