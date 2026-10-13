// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Type } from '@angular/core';
import { ControlValueAccessor, Validator } from '@angular/forms';
import { SendRpcRequestComponent } from '@home/components/scheduler/config/send-rpc-request.component';
import { UpdateAttributesComponent } from '@home/components/scheduler/config/update-attributes.component';
import { GenerateDashboardReportComponent } from '@home/components/scheduler/config/generate-dashboard-report.component';
import { OtaUpdateEventConfigComponent } from '@home/components/scheduler/config/ota-update-event-config.component';
import { GenerateReportComponent } from '@home/components/scheduler/config/generate-report.component';

export interface SchedulerEventConfigType {
  name: string;
  componentType?: Type<ControlValueAccessor & Validator>;
  template?: string;
  originator?: boolean;
  msgType?: boolean;
  metadata?: boolean;
  clearMsgBody?: boolean;
  clearMetadata?: boolean;
  clearOriginator?: boolean;
  clearMsgType?: boolean;
}

// Example of custom scheduler event config type

/*
test = {
  originator: true,
  msgType: true,
  template: '<form #myCustomConfigForm="ngForm">' +
    '<mat-form-field class="mat-block">' +
    '<mat-label>My custom field</mat-label>' +
    '<input name="myField" #myField="ngModel" matInput [(ngModel)]="configuration.msgBody.myField" required>' +
    '<mat-error *ngIf="myField.hasError(\'required\')">' +
    'My field is required.' +
    '</mat-error>' +
    '</mat-form-field>' +
    '<div>Form valid: {{myCustomConfigForm.valid}}</div>' +
    '</form>',
  name: 'Test!'
}*/

export const defaultSchedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType} = {
  generateReport: {
    name: 'Generate Report',
    componentType: GenerateReportComponent,
    originator: false,
    msgType: false,
    metadata: false,
    clearMsgBody: true,
    clearMetadata: true,
    clearMsgType: true,
    clearOriginator: true
  },
  generateDashboardReport: {
    name: 'Generate Dashboard Report (deprecated)',
    componentType: GenerateDashboardReportComponent,
    originator: false,
    msgType: false,
    metadata: false,
    clearMetadata: true,
    clearMsgType: true,
    clearOriginator: true
  },
  updateAttributes: {
    name: 'Update Attributes',
    componentType: UpdateAttributesComponent,
    originator: false,
    msgType: false,
    metadata: false
  },
  sendRpcRequest: {
    name: 'Send RPC Request to Device',
    componentType: SendRpcRequestComponent,
    originator: false,
    msgType: false,
    metadata: false
  },
  updateFirmware: {
    name: 'Update Firmware',
    componentType: OtaUpdateEventConfigComponent,
    originator: false,
    msgType: false,
    metadata: false,
    clearMetadata: true,
  },
  updateSoftware: {
    name: 'Update Software',
    componentType: OtaUpdateEventConfigComponent,
    originator: false,
    msgType: false,
    metadata: false,
    clearMetadata: true,
  }
};

/**
 * The scheduler event types that produce a report. A scheduler event of one of these types keeps generating
 * reports on its own schedule once saved, so it is a reporting write and is offered only while the reporting
 * feature is granted. Mirrors DataConstants.GENERATE_REPORT / GENERATE_DASHBOARD_REPORT on the server, which
 * refuses saving them for the same reason.
 */
export const reportSchedulerEventTypes: string[] = ['generateReport', 'generateDashboardReport'];

