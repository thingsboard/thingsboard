// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { isDefinedAndNotNull } from '@core/public-api';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EdgeSource } from '@home/components/rule-node/rule-node-config.models';

@Directive()
export abstract class AbstractEdgeEntityGroupConfigComponent extends RuleNodeConfigurationComponent {

  edgeGroupConfigForm: UntypedFormGroup;

  protected constructor(protected fb: UntypedFormBuilder) {
    super();
  }

  /** Config defaults this node adds to the shared edge source and entity groups pair. */
  protected nodeSpecificInputConfig(_configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {};
  }

  /** Form controls this node adds to the shared ones. */
  protected nodeSpecificControls(_configuration: RuleNodeConfiguration): {[controlName: string]: unknown} {
    return {};
  }

  protected configForm(): UntypedFormGroup {
    return this.edgeGroupConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      edgeSource: isDefinedAndNotNull(configuration?.edgeSource) ? configuration.edgeSource : EdgeSource.MESSAGE_ORIGINATOR,
      edgeNamePattern: isDefinedAndNotNull(configuration?.edgeNamePattern) ? configuration.edgeNamePattern : null,
      entityGroups: isDefinedAndNotNull(configuration?.entityGroups) ? configuration.entityGroups : [],
      ...this.nodeSpecificInputConfig(configuration)
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.edgeGroupConfigForm = this.fb.group({
      edgeSource: [configuration ? configuration.edgeSource : null, [Validators.required]],
      edgeNamePattern: [configuration ? configuration.edgeNamePattern : null, []],
      entityGroups: [configuration ? configuration.entityGroups : [], [Validators.required]],
      ...this.nodeSpecificControls(configuration)
    });
  }

  protected validatorTriggers(): string[] {
    return ['edgeSource'];
  }

  protected updateValidators(emitEvent: boolean) {
    const edgeNamePattern = this.edgeGroupConfigForm.get('edgeNamePattern');
    if (this.edgeGroupConfigForm.get('edgeSource').value === EdgeSource.EDGE_NAME_PATTERN) {
      edgeNamePattern.setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      edgeNamePattern.setValidators([]);
    }
    edgeNamePattern.updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.edgeNamePattern = configuration.edgeNamePattern ? configuration.edgeNamePattern.trim() : null;
    return configuration;
  }
}
