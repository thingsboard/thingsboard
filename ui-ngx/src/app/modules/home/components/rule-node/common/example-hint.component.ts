// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input } from '@angular/core';

@Component({
    selector: 'tb-example-hint',
    templateUrl: './example-hint.component.html',
    styleUrls: [],
    standalone: false
})
export class ExampleHintComponent {
  @Input() hintText: string;

  @Input() popupHelpLink: string;

  @Input() textAlign: string = 'left';
}


