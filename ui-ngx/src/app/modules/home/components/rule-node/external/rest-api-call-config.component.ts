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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { HttpRequestType, IntLimit } from '../rule-node-config.models';

@Component({
  selector: 'tb-external-node-rest-api-call-config',
  templateUrl: './rest-api-call-config.component.html',
  styleUrls: []
})
export class RestApiCallConfigComponent extends RuleNodeConfigurationComponent {

  restApiCallConfigForm: UntypedFormGroup;

  readonly proxySchemes: string[] = ['http', 'https'];
  readonly httpRequestTypes = Object.keys(HttpRequestType);
  readonly MemoryBufferSizeInKbLimit = 25000;
  readonly IntLimit = IntLimit;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.restApiCallConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.restApiCallConfigForm = this.fb.group({
      restEndpointUrlPattern: [configuration ? configuration.restEndpointUrlPattern : null, [Validators.required]],
      requestMethod: [configuration ? configuration.requestMethod : null, [Validators.required]],
      useSimpleClientHttpFactory: [configuration ? configuration.useSimpleClientHttpFactory : false, []],
      parseToPlainText: [configuration ? configuration.parseToPlainText : false, []],
      ignoreRequestBody: [configuration ? configuration.ignoreRequestBody : false, []],
      enableProxy: [configuration ? configuration.enableProxy : false, []],
      useSystemProxyProperties: [configuration ? configuration.enableProxy : false, []],
      proxyScheme: [configuration ? configuration.proxyHost : null, []],
      proxyHost: [configuration ? configuration.proxyHost : null, []],
      proxyPort: [configuration ? configuration.proxyPort : null, []],
      proxyUser: [configuration ? configuration.proxyUser :null, []],
      proxyPassword: [configuration ? configuration.proxyPassword :null, []],
      readTimeoutMs: [configuration ? configuration.readTimeoutMs : null, [Validators.min(0), Validators.max(IntLimit)]],
      maxParallelRequestsCount: [configuration ? configuration.maxParallelRequestsCount : null, [Validators.min(0), Validators.max(IntLimit)]],
      headers: [configuration ? configuration.headers : null, []],
      credentials: [configuration ? configuration.credentials : null, []],
      maxInMemoryBufferSizeInKb: [configuration ? configuration.maxInMemoryBufferSizeInKb : null, [Validators.min(1), Validators.max(this.MemoryBufferSizeInKbLimit)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSimpleClientHttpFactory', 'enableProxy', 'useSystemProxyProperties'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSimpleClientHttpFactory: boolean = this.restApiCallConfigForm.get('useSimpleClientHttpFactory').value;
    const enableProxy: boolean = this.restApiCallConfigForm.get('enableProxy').value;
    const useSystemProxyProperties: boolean = this.restApiCallConfigForm.get('useSystemProxyProperties').value;

    if (enableProxy && !useSystemProxyProperties) {
      this.restApiCallConfigForm.get('proxyHost').setValidators(enableProxy ? [Validators.required] : []);
      this.restApiCallConfigForm.get('proxyPort').setValidators(enableProxy ?
        [Validators.required, Validators.min(1), Validators.max(65535)] : []);
    } else {
      this.restApiCallConfigForm.get('proxyHost').setValidators([]);
      this.restApiCallConfigForm.get('proxyPort').setValidators([]);

      if (useSimpleClientHttpFactory) {
        this.restApiCallConfigForm.get('readTimeoutMs').setValidators([]);
      } else {
        this.restApiCallConfigForm.get('readTimeoutMs').setValidators([Validators.min(0), Validators.max(IntLimit)]);
      }
    }

    this.restApiCallConfigForm.get('readTimeoutMs').updateValueAndValidity({emitEvent});
    this.restApiCallConfigForm.get('proxyHost').updateValueAndValidity({emitEvent});
    this.restApiCallConfigForm.get('proxyPort').updateValueAndValidity({emitEvent});
    this.restApiCallConfigForm.get('credentials').updateValueAndValidity({emitEvent});
  }
}
