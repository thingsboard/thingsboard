import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import {
  MobileQrCodeWidgetSettingsComponent
} from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings.component';
import { WidgetComponentsModule } from '@home/components/widget/widget-components.module';
import {
  MobileQrCodeWidgetSettingsRoutingModule
} from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings-routing.module';


@NgModule({
  declarations: [
    MobileQrCodeWidgetSettingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    WidgetComponentsModule,
    MobileQrCodeWidgetSettingsRoutingModule
  ]
})
export class MobileQrCodeWidgetSettingsModule { }
