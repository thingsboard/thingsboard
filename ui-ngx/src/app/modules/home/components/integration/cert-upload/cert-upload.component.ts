// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    selector: 'tb-cert-upload',
    templateUrl: './cert-upload.component.html',
    styleUrls: [],
    standalone: false
})
export class CertUploadComponent {

  @Input() form: UntypedFormGroup;

  @Input() disabled: boolean;

  @Input() ignoreCaCert = false;

  constructor() {
  }

}
