// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive, Input, OnDestroy, TemplateRef } from '@angular/core';
import { Subject } from 'rxjs';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class IntegrationForm implements OnDestroy {

  @Input() executeRemotelyTemplate: TemplateRef<any>;
  @Input() genericAdditionalInfoTemplate: TemplateRef<any>;

  @Input()
  disabled: boolean;

  private allowLocalNetworkValue = true;

  get allowLocalNetwork(): boolean {
    return this.allowLocalNetworkValue;
  }

  @Input()
  set allowLocalNetwork(value: boolean) {
    if (this.allowLocalNetworkValue !== value) {
      this.allowLocalNetworkValue = value;
      this.updatedValidationPrivateNetwork();
    }
  }

  protected destroy$ = new Subject<void>();

  protected updatedValidationPrivateNetwork() {}

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
