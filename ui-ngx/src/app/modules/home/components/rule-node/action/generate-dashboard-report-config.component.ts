// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-generate-dashboard-report-config',
    templateUrl: './generate-dashboard-report-config.component.html',
    standalone: false
})
export class GenerateDashboardReportConfigComponent extends RuleNodeConfigurationComponent {

  generateDashboardReportConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.generateDashboardReportConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.generateDashboardReportConfigForm = this.fb.group({
      useSystemReportsServer: [configuration ? configuration.useSystemReportsServer : false, []],
      reportsServerEndpointUrl: [configuration ? configuration.reportsServerEndpointUrl : null, []],
      useReportConfigFromMessage: [configuration ? configuration.useReportConfigFromMessage : false, []],
      reportConfig: [configuration ? configuration.reportConfig : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemReportsServer', 'useReportConfigFromMessage'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemReportsServer: boolean = this.generateDashboardReportConfigForm.get('useSystemReportsServer').value;
    const useReportConfigFromMessage: boolean = this.generateDashboardReportConfigForm.get('useReportConfigFromMessage').value;
    if (emitEvent) {
      const reportsServerEndpointUrl: string = this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').value;
      if (useSystemReportsServer) {
        this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').reset(null, {emitEvent: false});
      } else {
        if (!reportsServerEndpointUrl || !reportsServerEndpointUrl.length) {
          this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').reset('http://localhost:8383',
            {emitEvent: false});
        }
      }
    }
    if (useSystemReportsServer) {
      this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').setValidators([]);
    } else {
      this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').setValidators([Validators.required]);
    }
    if (useReportConfigFromMessage) {
      this.generateDashboardReportConfigForm.get('reportConfig').setValidators([]);
    } else {
      this.generateDashboardReportConfigForm.get('reportConfig').setValidators([Validators.required]);
    }
    this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').updateValueAndValidity({emitEvent});
    this.generateDashboardReportConfigForm.get('reportConfig').updateValueAndValidity({emitEvent});
  }

}
