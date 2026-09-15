// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { Lwm2mDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/lwm2m/lwm2m-device-profile-transport-configuration.component';
import { Lwm2mObjectListComponent } from '@home/components/profile/device/lwm2m/lwm2m-object-list.component';
import { Lwm2mObserveAttrTelemetryComponent } from '@home/components/profile/device/lwm2m/lwm2m-observe-attr-telemetry.component';
import { Lwm2mObserveAttrTelemetryResourcesComponent } from '@home/components/profile/device/lwm2m/lwm2m-observe-attr-telemetry-resources.component';
import { Lwm2mAttributesDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-attributes-dialog.component';
import { Lwm2mAttributesComponent } from '@home/components/profile/device/lwm2m/lwm2m-attributes.component';
import { Lwm2mAttributesKeyListComponent } from '@home/components/profile/device/lwm2m/lwm2m-attributes-key-list.component';
import { Lwm2mDeviceConfigServerComponent } from '@home/components/profile/device/lwm2m/lwm2m-device-config-server.component';
import { Lwm2mObjectAddInstancesDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances-dialog.component';
import { Lwm2mObjectAddInstancesListComponent } from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances-list.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { Lwm2mObserveAttrTelemetryInstancesComponent } from '@home/components/profile/device/lwm2m/lwm2m-observe-attr-telemetry-instances.component';
import { DeviceProfileCommonModule } from '@home/components/profile/device/common/device-profile-common.module';
import { Lwm2mBootstrapConfigServersComponent } from '@home/components/profile/device/lwm2m/lwm2m-bootstrap-config-servers.component';
import { Lwm2mBootstrapAddConfigServerDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-bootstrap-add-config-server-dialog.component';

@NgModule({
  declarations:
    [
      Lwm2mDeviceProfileTransportConfigurationComponent,
      Lwm2mObjectListComponent,
      Lwm2mObserveAttrTelemetryComponent,
      Lwm2mObserveAttrTelemetryResourcesComponent,
      Lwm2mAttributesDialogComponent,
      Lwm2mAttributesComponent,
      Lwm2mAttributesKeyListComponent,
      Lwm2mBootstrapConfigServersComponent,
      Lwm2mDeviceConfigServerComponent,
      Lwm2mBootstrapAddConfigServerDialogComponent,
      Lwm2mObjectAddInstancesDialogComponent,
      Lwm2mObjectAddInstancesListComponent,
      Lwm2mObserveAttrTelemetryInstancesComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    DeviceProfileCommonModule
   ],
  exports: [
    Lwm2mDeviceProfileTransportConfigurationComponent,
    Lwm2mObjectListComponent,
    Lwm2mObserveAttrTelemetryComponent,
    Lwm2mObserveAttrTelemetryResourcesComponent,
    Lwm2mAttributesDialogComponent,
    Lwm2mAttributesComponent,
    Lwm2mAttributesKeyListComponent,
    Lwm2mBootstrapConfigServersComponent,
    Lwm2mDeviceConfigServerComponent,
    Lwm2mBootstrapAddConfigServerDialogComponent,
    Lwm2mObjectAddInstancesDialogComponent,
    Lwm2mObjectAddInstancesListComponent,
    Lwm2mObserveAttrTelemetryInstancesComponent
  ],
  providers: [
  ]
})
export class Lwm2mProfileComponentsModule { }
