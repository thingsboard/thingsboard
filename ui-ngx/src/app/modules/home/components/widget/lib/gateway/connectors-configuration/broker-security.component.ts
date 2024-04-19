///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  NgZone,
  OnDestroy,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Subject } from 'rxjs';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  BrokerSecurityType,
  BrokerSecurityTypeTranslationsMap,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-broker-security',
  templateUrl: './broker-security.component.html',
  styleUrls: ['./broker-security.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BrokerSecurityComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => BrokerSecurityComponent),
      multi: true
    }
  ]
})
export class BrokerSecurityComponent extends PageComponent implements ControlValueAccessor, Validator, OnDestroy {

  BrokerSecurityType = BrokerSecurityType;

  securityTypes = Object.values(BrokerSecurityType);

  SecurityTypeTranslationsMap = BrokerSecurityTypeTranslationsMap;

  securityFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public dialog: MatDialog,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private dialogService: DialogService,
              private entityService: EntityService,
              private utils: UtilsService,
              private zone: NgZone,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private fb: FormBuilder) {
    super(store);
    this.securityFormGroup = this.fb.group({
      type: [BrokerSecurityType.ANONYMOUS, []],
      username: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToCACert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToPrivateKey: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToClientCert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    this.securityFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
    this.securityFormGroup.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.updateValidators(type);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfo: any) {
    if (!deviceInfo.type) {
      deviceInfo.type = BrokerSecurityType.ANONYMOUS;
    }
    this.securityFormGroup.reset(deviceInfo);
    this.updateView(deviceInfo);
  }

  validate(): ValidationErrors | null {
    return this.securityFormGroup.valid ? null : {
      securityForm: { valid: false }
    };
  }

  updateView(value: any) {
    this.propagateChange(value);
  }

  private updateValidators(type) {
    if (type) {
      this.securityFormGroup.get('username').disable({emitEvent: false});
      this.securityFormGroup.get('password').disable({emitEvent: false});
      this.securityFormGroup.get('pathToCACert').disable({emitEvent: false});
      this.securityFormGroup.get('pathToPrivateKey').disable({emitEvent: false});
      this.securityFormGroup.get('pathToClientCert').disable({emitEvent: false});
      if (type === BrokerSecurityType.BASIC) {
        this.securityFormGroup.get('username').enable({emitEvent: false});
        this.securityFormGroup.get('password').enable({emitEvent: false});
      } else if (type === BrokerSecurityType.CERTIFICATES) {
        this.securityFormGroup.get('pathToCACert').enable({emitEvent: false});
        this.securityFormGroup.get('pathToPrivateKey').enable({emitEvent: false});
        this.securityFormGroup.get('pathToClientCert').enable({emitEvent: false});
      }
    }
  }
}
