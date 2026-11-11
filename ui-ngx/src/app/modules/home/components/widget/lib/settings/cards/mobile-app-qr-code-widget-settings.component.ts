// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup } from "@angular/forms";
import { WidgetSettings, WidgetSettingsComponent } from "@shared/models/widget.models";
import { AppState } from '@core/core.state';
import { Store } from "@ngrx/store";
import { badgePositionTranslationsMap } from '@shared/models/mobile-app.models';
import { mobileAppQrCodeWidgetDefaultSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService} from '@core/http/user-permissions.service';
import { Operation, Resource} from "@shared/models/security.models";

@Component({
    selector: 'tb-mobile-app-qr-code-widget-settings',
    templateUrl: './mobile-app-qr-code-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class MobileAppQrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  mobileAppQRCodeWidgetSettingsForm: UntypedFormGroup;

  badgePositionTranslationsMap = badgePositionTranslationsMap;

  displayConfigurationHint = false;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store);
    this.displayConfigurationHint = getCurrentAuthUser(this.store).authority !== Authority.CUSTOMER_USER &&
      (this.userPermissionsService.hasGenericPermission(Resource.MOBILE_APP_SETTINGS, Operation.WRITE) ||
      this.userPermissionsService.hasGenericPermission(Resource.MOBILE_APP_SETTINGS, Operation.READ));
  }

  protected settingsForm(): UntypedFormGroup {
    return this.mobileAppQRCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mobileAppQrCodeWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.mobileAppQRCodeWidgetSettingsForm = this.fb.group({
      useSystemSettings: [settings.useSystemSettings],
      qrCodeConfig: this.fb.group({
        badgeEnabled: [settings.qrCodeConfig.badgeEnabled],
        badgePosition: [settings.qrCodeConfig.badgePosition],
        qrCodeLabelEnabled: [settings.qrCodeConfig.qrCodeLabelEnabled],
        qrCodeLabel: [settings.qrCodeConfig.qrCodeLabel]
      }),
      background: [settings.background],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSettings', 'qrCodeConfig.badgeEnabled', 'qrCodeConfig.qrCodeLabelEnabled'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSettings = this.mobileAppQRCodeWidgetSettingsForm.get('useSystemSettings').value;
    if (!useSystemSettings) {
      const badgeEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').value;
      const qrCodeLabelEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').value;
      if (badgeEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
      }
      if (qrCodeLabelEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
      }
    }
  }

  navigateToMobileAppSettings($event) {
    if ($event) {
      $event.stopPropagation();
    }
    window.open(window.location.origin + '/settings/mobile-app', '_blank');
  }

}
