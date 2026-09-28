// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, DestroyRef, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import {
  CsvReportTemplateConfig,
  defaultCsvReportTemplateConfig,
  defaultPdfReportTemplateConfig,
  PdfReportTemplateConfig,
  reportFormats,
  ReportTemplate,
  ReportTemplateType,
  reportTemplateTypes,
  reportTemplateTypeTranslationMap,
  TbReportFormat
} from '@shared/models/report.models';
import { mergeDeep } from '@core/utils';

@Component({
    selector: 'tb-report-template-form',
    templateUrl: './report-template-form.component.html',
    standalone: false
})
export class ReportTemplateFormComponent extends EntityComponent<ReportTemplate> {

  ReportTemplateType = ReportTemplateType;

  reportTemplateTypes = reportTemplateTypes;

  reportTemplateTypeTranslationMap = reportTemplateTypeTranslationMap;

  TbReportFormat = TbReportFormat;

  reportFormats = reportFormats;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dialog: MatDialog,
              @Inject('entity') protected entityValue: ReportTemplate,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<ReportTemplate>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: ReportTemplate): UntypedFormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        format: [entity?.format ? entity.format : TbReportFormat.PDF, [Validators.required]],
        type: [entity?.type ? entity.type : ReportTemplateType.REPORT, [Validators.required]],
        description: [entity?.description]
      }
    );
  }

  updateForm(entity: ReportTemplate) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({format: entity.format});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({description: entity.description});
  }

  override updateFormState(): void {
    super.updateFormState();
    if (this.isEdit && this.entityForm && !this.isAdd) {
      this.entityForm.get('type').disable({ emitEvent: false });
      this.entityForm.get('format').disable({ emitEvent: false });
    }
  }

  override prepareFormValue(value: ReportTemplate): ReportTemplate {
    if (this.isAdd) {
      if (value.format === TbReportFormat.PDF) {
        value.configuration = mergeDeep({} as PdfReportTemplateConfig, defaultPdfReportTemplateConfig);
      } else {
        value.configuration = mergeDeep({} as CsvReportTemplateConfig, defaultCsvReportTemplateConfig);
      }
    }
    return super.prepareFormValue(value);
  }

  onReportTemplateIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('report-template.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
