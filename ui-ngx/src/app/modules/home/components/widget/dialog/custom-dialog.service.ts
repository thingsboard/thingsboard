///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Injectable, NgModule } from '@angular/core';
import { Observable } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { mergeMap, tap } from 'rxjs/operators';
import { CustomDialogComponent } from './custom-dialog.component';
import {
  CustomDialogContainerComponent,
  CustomDialogContainerData
} from '@home/components/widget/dialog/custom-dialog-container.component';

@Injectable()
export class CustomDialogService {

  constructor(
    private translate: TranslateService,
    private authService: AuthService,
    private dynamicComponentFactoryService: DynamicComponentFactoryService,
    public dialog: MatDialog
  ) {
  }

  customDialog(template: string, controller: (instance: CustomDialogComponent) => void, data?: any): Observable<any> {
    return this.dynamicComponentFactoryService.createDynamicComponentFactory(
      class CustomDialogComponentInstance extends CustomDialogComponent {},
      template,
      [SharedModule, CustomDialogModule]).pipe(
      mergeMap((factory) => {
          const dialogData: CustomDialogContainerData = {
            controller,
            customComponentFactory: factory,
            data
          };
          return this.dialog.open<CustomDialogContainerComponent, CustomDialogContainerData, any>(
            CustomDialogContainerComponent,
            {
              disableClose: true,
              panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
              data: dialogData
            }).afterClosed().pipe(
            tap(() => {
              this.dynamicComponentFactoryService.destroyDynamicComponentFactory(factory);
            })
          );
        }
      ));
  }

}

@NgModule({
  declarations:
    [
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
  ]
})
class CustomDialogModule { }
