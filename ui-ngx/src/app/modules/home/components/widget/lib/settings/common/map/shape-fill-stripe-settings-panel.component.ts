// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MapDataLayerType, ShapeFillStripeSettings } from '@shared/models/widget/maps/map.models';
import { DomSanitizer } from '@angular/platform-browser';
import {
  generateStripePreviewUrl
} from '@home/components/widget/lib/settings/common/map/shape-fill-stripe-settings.component';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DatasourceType } from '@shared/models/widget.models';

@Component({
    selector: 'tb-shape-fill-stripe-settings-panel',
    templateUrl: './shape-fill-stripe-settings-panel.component.html',
    providers: [],
    styleUrls: ['./shape-fill-stripe-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ShapeFillStripeSettingsPanelComponent implements OnInit {

  @Input()
  shapeFillStripeSettings: ShapeFillStripeSettings;

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

  @Output()
  shapeFillStripeSettingsApplied = new EventEmitter<ShapeFillStripeSettings>();

  stripePreviewStyle: ComponentStyle;

  shapeFillStripeSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private sanitizer: DomSanitizer,
              private popover: TbPopoverComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.shapeFillStripeSettingsFormGroup = this.fb.group(
      {
        weight: [this.shapeFillStripeSettings?.weight, [Validators.min(0)]],
        color: [this.shapeFillStripeSettings?.color, []],
        spaceWeight: [this.shapeFillStripeSettings?.spaceWeight, [Validators.min(0)]],
        spaceColor: [this.shapeFillStripeSettings?.spaceColor, []],
        angle: [this.shapeFillStripeSettings?.angle, [Validators.min(0), Validators.max(180)]]
      }
    );
    this.shapeFillStripeSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updatePreview();
    });
    this.updatePreview();
  }

  cancel() {
    this.popover?.hide();
  }

  applyShapeFillStripeSettings() {
    const shapeFillStripeSettings: ShapeFillStripeSettings = this.shapeFillStripeSettingsFormGroup.value;
    this.shapeFillStripeSettingsApplied.emit(shapeFillStripeSettings);
    this.popover?.hide();
  }

  private updatePreview() {
    const shapeFillStripeSettings: ShapeFillStripeSettings = this.shapeFillStripeSettingsFormGroup.value;
    const previewUrl = generateStripePreviewUrl(shapeFillStripeSettings, 136, 118);
    this.stripePreviewStyle = {
      background: this.sanitizer.bypassSecurityTrustStyle(`url(${previewUrl}) no-repeat 50% 50% / cover`)
    };
  }

}
