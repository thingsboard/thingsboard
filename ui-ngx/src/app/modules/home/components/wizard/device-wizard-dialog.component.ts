// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Inject, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { Device, DeviceProfileInfo, DeviceTransportType } from '@shared/models/device.models';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { CustomerId } from '@shared/models/id/customer-id';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { deepTrim } from '@core/utils';
import { HttpErrorResponse } from '@angular/common/http';
import { EntityGroup } from '@shared/models/entity-group.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { OwnerAndGroupsData } from '@home/components/group/owner-and-groups.component';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

export interface DeviceWizardDialogData {
  customerId?: string;
  entityGroup?: EntityGroup;
}

@Component({
    selector: 'tb-device-wizard',
    templateUrl: './device-wizard-dialog.component.html',
    styleUrls: ['./device-wizard-dialog.component.scss'],
    standalone: false
})
export class DeviceWizardDialogComponent extends DialogComponent<DeviceWizardDialogComponent, Device> {

  @ViewChild('addDeviceWizardStepper', {static: true}) addDeviceWizardStepper: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;

  stepperLabelPosition: Observable<'bottom' | 'end'>;

  selectedIndex = 0;

  credentialsOptionalStep = true;

  showNext = true;

  entityType = EntityType;

  readonly isTenantAdmin: boolean;

  deviceWizardFormGroup: FormGroup;

  credentialsFormGroup: FormGroup;

  initialOwnerId: EntityId;

  private entityGroup = this.data.entityGroup;

  private customerId = this.data.customerId;

  private currentDeviceProfileTransportType = DeviceTransportType.DEFAULT;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceWizardDialogData,
              public dialogRef: MatDialogRef<DeviceWizardDialogComponent, Device>,
              private deviceService: DeviceService,
              private userPermissionsService: UserPermissionsService,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.isTenantAdmin = getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN;

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'end' : 'bottom'));

    let initialGroups: EntityInfoData[] = [];
    if (this.entityGroup) {
      this.initialOwnerId = this.entityGroup.ownerId;
      if (!this.entityGroup.groupAll) {
        initialGroups = [{id: this.entityGroup.id, name: this.entityGroup.name}];
      }
    } else {
      if (this.customerId) {
        this.initialOwnerId = new CustomerId(this.customerId);
      } else {
        this.initialOwnerId = this.userPermissionsService.getUserOwnerId();
      }
    }

    const ownerAndGroups: OwnerAndGroupsData = {
      owner: this.initialOwnerId,
      groups: initialGroups
    };

    this.deviceWizardFormGroup = this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(255)]],
        label: ['', Validators.maxLength(255)],
        gateway: [false],
        overwriteActivityTime: [false],
        deviceProfileId: [null, Validators.required],
        ownerAndGroups: [ownerAndGroups, [Validators.required]],
        description: ['']
      }
    );

    this.credentialsFormGroup  = this.fb.group({
        credential: []
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addDeviceWizardStepper.previous();
  }

  nextStep(): void {
    this.addDeviceWizardStepper.next();
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'device.wizard.device-details';
      case 1:
        return 'device.credentials';
    }
  }

  get maxStepperIndex(): number {
    return this.addDeviceWizardStepper?._steps?.length - 1;
  }

  add(): void {
    if (this.allValid()) {
      this.createDevice().subscribe(
        (device) => this.dialogRef.close(device)
      );
    }
  }

  get deviceTransportType(): DeviceTransportType {
    return this.currentDeviceProfileTransportType;
  }

  deviceProfileChanged(deviceProfile: DeviceProfileInfo) {
    if (deviceProfile) {
      this.currentDeviceProfileTransportType = deviceProfile.transportType;
      this.credentialsOptionalStep = this.currentDeviceProfileTransportType !== DeviceTransportType.LWM2M;
    }
  }

  private createDevice(): Observable<Device> {
    const device: Device = {
      name: this.deviceWizardFormGroup.get('name').value,
      label: this.deviceWizardFormGroup.get('label').value,
      deviceProfileId: this.deviceWizardFormGroup.get('deviceProfileId').value,
      additionalInfo: {
        gateway: this.deviceWizardFormGroup.get('gateway').value,
        overwriteActivityTime: this.deviceWizardFormGroup.get('overwriteActivityTime').value,
        description: this.deviceWizardFormGroup.get('description').value
      },
      customerId: null
    } as Device;
    const targetOwnerAndGroups: OwnerAndGroupsData = this.deviceWizardFormGroup.get('ownerAndGroups').value;
    const targetOwner = targetOwnerAndGroups.owner;
    let targetOwnerId: EntityId;
    if ((targetOwner as EntityInfoData).name) {
      targetOwnerId = (targetOwner as EntityInfoData).id;
    } else {
      targetOwnerId = targetOwner as EntityId;
    }
    if (targetOwnerId.entityType === EntityType.CUSTOMER) {
      device.customerId = targetOwnerId as CustomerId;
    }
    const entityGroupIds = targetOwnerAndGroups.groups.map(group => group.id.id);
    if (this.addDeviceWizardStepper.steps.last.completed || this.addDeviceWizardStepper.selectedIndex > 0) {
      return this.deviceService.saveDeviceWithCredentials(deepTrim(device), deepTrim(this.credentialsFormGroup.value.credential),
                                                          entityGroupIds).pipe(
        catchError((e: HttpErrorResponse) => {
          if (e.error.message.includes('Device credentials')) {
            this.addDeviceWizardStepper.selectedIndex = 1;
          } else {
            this.addDeviceWizardStepper.selectedIndex = 0;
          }
          return throwError(() => e);
        })
      );
    }
    return this.deviceService.saveDevice(deepTrim(device), entityGroupIds).pipe(
      catchError(e => {
        this.addDeviceWizardStepper.selectedIndex = 0;
        return throwError(e);
      })
    );
  }
  allValid(): boolean {
    return !this.addDeviceWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addDeviceWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    this.showNext = this.selectedIndex !== this.maxStepperIndex;
  }
}
