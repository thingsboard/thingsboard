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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { AutoCommitSettings, AutoVersionCreateConfig } from '@shared/models/settings.models';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import {
  EntityTypeVersionCreateConfig,
  exportableEntityTypes,
  typesWithCalculatedFields
} from '@shared/models/vc.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'tb-auto-commit-settings',
  templateUrl: './auto-commit-settings.component.html',
  styleUrls: ['./auto-commit-settings.component.scss', './../../pages/admin/settings-card.scss']
})
export class AutoCommitSettingsComponent extends PageComponent implements OnInit {

  autoCommitSettingsForm: UntypedFormGroup;
  settings: AutoCommitSettings = null;

  entityTypes = EntityType;

  isReadOnly: Observable<boolean>;

  readonly typesWithCalculatedFields = typesWithCalculatedFields;

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private dialogService: DialogService,
              private sanitizer: DomSanitizer,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.autoCommitSettingsForm = this.fb.group({
      entityTypes: this.fb.array([], [])
    });
    this.adminService.autoCommitSettingsExists().pipe(
      catchError(() => of(false)),
      mergeMap((hasAutoCommitSettings) => {
        if (hasAutoCommitSettings) {
          return this.adminService.getAutoCommitSettings({ignoreErrors: true}).pipe(
            catchError(() => of(null))
          );
        } else {
          return of(null);
        }
      })
    ).subscribe(
      (settings) => {
        this.settings = settings;
        this.autoCommitSettingsForm.setControl('entityTypes',
          this.prepareEntityTypesFormArray(settings), {emitEvent: false});
      });
    this.isReadOnly = this.adminService.getRepositorySettingsInfo().pipe(map(settings => settings.readOnly));
  }

  entityTypesFormGroupArray(): UntypedFormGroup[] {
    return (this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray).controls as UntypedFormGroup[];
  }

  entityTypesFormGroupExpanded(entityTypeControl: AbstractControl): boolean {
    return !!(entityTypeControl as any).expanded;
  }

  public trackByEntityType(index: number, entityTypeControl: AbstractControl): any {
    return entityTypeControl;
  }

  public removeEntityType(index: number) {
    (this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray).removeAt(index);
    this.autoCommitSettingsForm.markAsDirty();
  }

  public addEnabled(): boolean {
    const entityTypesArray = this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray;
    return entityTypesArray.length < exportableEntityTypes.length;
  }

  public addEntityType() {
    const entityTypesArray = this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray;
    const config: AutoVersionCreateConfig = {
      branch: null,
      saveAttributes: true,
      saveRelations: false,
      saveCredentials: true,
      saveCalculatedFields: true,
    };
    const allowed = this.allowedEntityTypes();
    let entityType: EntityType = null;
    if (allowed.length) {
      entityType = allowed[0];
    }
    const entityTypeControl = this.createEntityTypeControl(entityType, config);
    (entityTypeControl as any).expanded = true;
    entityTypesArray.push(entityTypeControl);
    this.autoCommitSettingsForm.updateValueAndValidity();
    this.autoCommitSettingsForm.markAsDirty();
  }

  public removeAll() {
    const entityTypesArray = this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray;
    entityTypesArray.clear();
    this.autoCommitSettingsForm.updateValueAndValidity();
    this.autoCommitSettingsForm.markAsDirty();
  }

  entityTypeText(entityTypeControl: AbstractControl): SafeHtml {
    const entityType: EntityType = entityTypeControl.get('entityType').value;
    const config: AutoVersionCreateConfig = entityTypeControl.get('config').value;
    let message = entityType ? this.translate.instant(entityTypeTranslations.get(entityType).typePlural) : 'Undefined';
    let branchName;
    if (config.branch) {
      branchName = config.branch;
    } else {
      branchName = this.translate.instant('version-control.default');
    }
    message += ` (<small>${this.translate.instant('version-control.auto-commit-to-branch', {branch: branchName})}</small>)`;
    return this.sanitizer.bypassSecurityTrustHtml(message);
  }

  allowedEntityTypes(entityTypeControl?: AbstractControl): Array<EntityType> {
    let res = [...exportableEntityTypes];
    const currentEntityType: EntityType = entityTypeControl?.get('entityType')?.value;
    const value: [{entityType: string, config: EntityTypeVersionCreateConfig}] =
      this.autoCommitSettingsForm.get('entityTypes').value || [];
    const usedEntityTypes = value.map(val => val.entityType).filter(val => val);
    res = res.filter(entityType => !usedEntityTypes.includes(entityType) || entityType === currentEntityType);
    return res;
  }

  save(): void {
    const value: [{entityType: string, config: AutoVersionCreateConfig}] =
      this.autoCommitSettingsForm.get('entityTypes').value || [];
    const settings: AutoCommitSettings = {};
    if (value && value.length) {
      value.forEach((val) => {
        settings[val.entityType] = val.config;
      });
    }
    this.adminService.saveAutoCommitSettings(settings).subscribe(
      (savedSettings) => {
        this.settings = savedSettings;
        this.autoCommitSettingsForm.setControl('entityTypes',
          this.prepareEntityTypesFormArray(savedSettings), {emitEvent: false});
        this.autoCommitSettingsForm.markAsPristine();
      }
    );
  }

  delete(formDirective: FormGroupDirective): void {
    this.dialogService.confirm(
      this.translate.instant('admin.delete-auto-commit-settings-title', ),
      this.translate.instant('admin.delete-auto-commit-settings-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.adminService.deleteAutoCommitSettings().subscribe(
          () => {
            this.settings = null;
            this.autoCommitSettingsForm.setControl('entityTypes',
              this.prepareEntityTypesFormArray(this.settings), {emitEvent: false});
            this.autoCommitSettingsForm.markAsPristine();
          }
        );
      }
    });
  }

  private prepareEntityTypesFormArray(settings: AutoCommitSettings | null): UntypedFormArray {
    const entityTypesControls: Array<AbstractControl> = [];
    if (settings) {
      for (const entityType of Object.keys(settings)) {
        const config = settings[entityType];
        entityTypesControls.push(this.createEntityTypeControl(entityType as EntityType, config));
      }
    }
    return this.fb.array(entityTypesControls);
  }

  private createEntityTypeControl(entityType: EntityType, config: AutoVersionCreateConfig): AbstractControl {
    const entityTypeControl = this.fb.group(
      {
        entityType: [entityType, [Validators.required]],
        config: this.fb.group({
          branch: [config.branch, []],
          saveRelations: [config.saveRelations, []],
          saveAttributes: [config.saveAttributes, []],
          saveCredentials: [config.saveCredentials, []],
          saveCalculatedFields: [config.saveCalculatedFields, []]
        })
      }
    );
    return entityTypeControl;
  }


}
