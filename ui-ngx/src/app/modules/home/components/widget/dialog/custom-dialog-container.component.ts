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

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  Component,
  ComponentRef,
  HostBinding,
  Inject,
  Injector,
  OnDestroy,
  Type,
  ViewContainerRef
} from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import {
  CUSTOM_DIALOG_DATA,
  CustomDialogComponent,
  CustomDialogData
} from '@home/components/widget/dialog/custom-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';

export interface CustomDialogContainerData {
  controller: (instance: CustomDialogComponent) => void;
  data?: any;
  customComponentType: Type<CustomDialogComponent>;
}

@Component({
    selector: 'tb-custom-dialog-container-component',
    template: '',
    standalone: false
})
export class CustomDialogContainerComponent extends DialogComponent<CustomDialogContainerComponent> implements OnDestroy {

  @HostBinding('style.height') height = '0px';

  private readonly customComponentRef: ComponentRef<CustomDialogComponent>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public viewContainerRef: ViewContainerRef,
              public dialogRef: MatDialogRef<CustomDialogContainerComponent>,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: CustomDialogContainerData) {
    super(store, router, dialogRef);
    let customDialogData: CustomDialogData = {
      controller: this.data.controller
    };
    if (this.data.data) {
      customDialogData = {...customDialogData, ...this.data.data};
    }
    const injector: Injector = Injector.create({
      providers: [{
        provide: CUSTOM_DIALOG_DATA,
        useValue: customDialogData
      },
        {
          provide: MatDialogRef,
          useValue: dialogRef
        }]
    });
    try {
      this.customComponentRef = this.viewContainerRef.createComponent(this.data.customComponentType,
        {index: 0, injector});
    } catch (e: any) {
      let message;
      if (e.message?.startsWith('NG0')) {
        message = this.translate.instant('widget-action.custom-pretty-template-error');
      } else {
        message = this.translate.instant('widget-action.custom-pretty-controller-error');
      }
      dialogRef.close();
      console.error(e);
      this.dialogService.errorAlert(this.translate.instant('widget-action.custom-pretty-error-title'), message, e);
    }
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.customComponentRef) {
      this.customComponentRef.destroy();
    }
  }

}
