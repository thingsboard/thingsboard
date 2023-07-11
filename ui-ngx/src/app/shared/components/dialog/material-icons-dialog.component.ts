///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { AfterViewInit, Component, Inject, OnInit, QueryList, ViewChildren } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { UntypedFormControl } from '@angular/forms';
import { merge, Observable } from 'rxjs';
import { delay, map, mapTo, mergeMap, share, startWith, tap } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';
import { getMaterialIcons } from '@shared/models/icon.models';

export interface MaterialIconsDialogData {
  icon: string;
}

@Component({
  selector: 'tb-material-icons-dialog',
  templateUrl: './material-icons-dialog.component.html',
  providers: [],
  styleUrls: ['./material-icons-dialog.component.scss']
})
export class MaterialIconsDialogComponent extends DialogComponent<MaterialIconsDialogComponent, string>
  implements OnInit, AfterViewInit {

  @ViewChildren('iconButtons') iconButtons: QueryList<HTMLElement>;

  selectedIcon: string;
  icons$: Observable<Array<string>>;
  loadingIcons$: Observable<boolean>;

  showAllControl: UntypedFormControl;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MaterialIconsDialogData,
              private utils: UtilsService,
              private resourcesService: ResourcesService,
              public dialogRef: MatDialogRef<MaterialIconsDialogComponent, string>) {
    super(store, router, dialogRef);
    this.selectedIcon = data.icon;
    this.showAllControl = new UntypedFormControl(false);
  }

  ngOnInit(): void {
    this.icons$ = this.showAllControl.valueChanges.pipe(
      map((showAll) => ({firstTime: false, showAll})),
      startWith<{firstTime: boolean; showAll: boolean}>({firstTime: true, showAll: false}),
      mergeMap((data) => {
        const res = getMaterialIcons(this.resourcesService, data.showAll, '');
        if (data.showAll) {
          return res.pipe(delay(100));
        } else {
          return data.firstTime ? res : res.pipe(delay(50));
        }
      }),
      share()
    );
  }

  ngAfterViewInit(): void {
    this.loadingIcons$ = merge(
      this.showAllControl.valueChanges.pipe(
        mapTo(true),
      ),
      this.iconButtons.changes.pipe(
        delay(100),
        mapTo( false),
      )
    ).pipe(
      tap((loadingIcons) => {
        if (loadingIcons) {
          this.showAllControl.disable({emitEvent: false});
        } else {
          this.showAllControl.enable({emitEvent: false});
        }
      }),
      share()
    );
  }

  selectIcon(icon: string) {
    this.dialogRef.close(icon);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

}
