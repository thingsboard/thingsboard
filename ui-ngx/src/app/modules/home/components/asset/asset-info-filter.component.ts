// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { AssetProfileId } from '@shared/models/id/asset-profile-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-asset-info-filter',
  templateUrl: './asset-info-filter.component.html',
  styleUrls: ['../../../../../scss/filter-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AssetInfoFilterComponent),
      multi: true
    }
  ],
  standalone: false
})
export class AssetInfoFilterComponent implements ControlValueAccessor {

  @ViewChild('assetFilterPanel')
  assetFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  buttonDisplayValue = this.translate.instant('asset.asset-filter-title');

  assetInfoFilterForm: FormGroup = this.fb.group({
    assetProfileId: [null, []]
  });

  private assetProfileId: AssetProfileId;
  private assetProfileInfo: EntityInfoData;
  private assetFilterOverlayRef: OverlayRef;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private assetProfileService: AssetProfileService,
              private cd: ChangeDetectorRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.assetInfoFilterForm.disable({emitEvent: false});
    } else {
      this.assetInfoFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(assetProfileId?: AssetProfileId): void {
    this.assetProfileId = assetProfileId ?? null;
    this.updateButtonDisplayValue();
    this.updateAssetInfoFilterForm(this.assetProfileId);
  }

  toggleAssetFilterPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.assetFilterOverlayRef = this.overlay.create(config);
    this.assetFilterOverlayRef.backdropClick().subscribe(() => {
      this.cancel();
    });
    this.assetFilterOverlayRef.attach(new TemplatePortal(this.assetFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.assetFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateAssetInfoFilterForm(this.assetProfileId);
    this.assetInfoFilterForm.markAsPristine();
    this.closePanel();
  }

  update() {
    this.assetProfileId = this.assetInfoFilterForm.value.assetProfileId ?? null;
    this.updateButtonDisplayValue();
    this.propagateChange(this.assetProfileId);
    this.assetInfoFilterForm.markAsPristine();
    this.closePanel();
  }

  reset() {
    if (isDefinedAndNotNull(this.assetInfoFilterForm.value.assetProfileId)) {
      this.updateAssetInfoFilterForm(null);
      this.assetInfoFilterForm.markAsDirty();
    }
  }

  assetProfileChanged(assetProfileInfo: EntityInfoData) {
    this.assetProfileInfo = assetProfileInfo;
  }

  private closePanel() {
    this.resizeWindows?.unsubscribe();
    this.assetFilterOverlayRef?.dispose();
  }

  private updateAssetInfoFilterForm(assetProfileId: AssetProfileId) {
    this.assetInfoFilterForm.patchValue({
      assetProfileId
    }, {emitEvent: false});
  }

  private updateButtonDisplayValue() {
    if (isDefinedAndNotNull(this.assetProfileId)) {
      if (this.assetProfileInfo?.id?.id !== this.assetProfileId.id) {
        const assetProfileId = this.assetProfileId.id;
        this.assetProfileService.getAssetProfileInfo(assetProfileId,
          {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: (assetProfileInfo) => {
            if (this.assetProfileId?.id === assetProfileId) {
              this.assetProfileInfo = assetProfileInfo;
              this.updateButtonDisplayValue();
            }
          },
          error: () => {
            if (this.assetProfileId?.id === assetProfileId) {
              this.buttonDisplayValue = this.translate.instant('asset.filter-title');
              this.cd.markForCheck();
            }
          }
        });
        return;
      }
      this.buttonDisplayValue = this.translate.instant('asset.filter-title') + `: ${this.assetProfileInfo.name}`;
    } else {
      this.buttonDisplayValue = this.translate.instant('asset.asset-filter-title');
    }
    this.cd.markForCheck();
  }
}
