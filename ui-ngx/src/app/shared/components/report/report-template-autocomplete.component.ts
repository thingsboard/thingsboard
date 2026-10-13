// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { getEntityDetailsPageURL } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import {
  ReportTemplateFilter,
  ReportTemplateInfo,
  ReportTemplateQuery,
  ReportTemplateType,
  TbReportFormat
} from '@shared/models/report.models';
import { ReportTemplateService } from '@core/http/report-template.service';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-report-template-autocomplete',
    templateUrl: './report-template-autocomplete.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportTemplateAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class ReportTemplateAutocompleteComponent implements ControlValueAccessor, OnInit {

  private dirty = false;

  selectReportTemplateFormGroup: UntypedFormGroup;

  modelValue: ReportTemplateId | null;

  @Input()
  type: ReportTemplateType;

  @Input()
  format: TbReportFormat;

  @Input()
  @coerceBoolean()
  includeCustomers = false;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  placeholder: string;

  @Input()
  @coerceBoolean()
  newTabDetailsButton: boolean;

  @Input()
  @coerceBoolean()
  showHint: boolean;

  @Input()
  @coerceBoolean()
  showError: boolean;

  @ViewChild('reportTemplateInput', {static: true}) reportTemplateInput: ElementRef;
  @ViewChild('reportTemplateInput', {read: MatAutocompleteTrigger, static: true}) reportTemplateAutocomplete: MatAutocompleteTrigger;

  filteredReportTemplates: Observable<Array<ReportTemplateInfo>>;

  searchText = '';

  reportTemplateURL = '';

  labelTranslationKey: string;
  placeholderTranslationKey: string;
  openNewTabTranslationKey: string;
  noReportTemplatesTranslationKey: string;
  noReportTemplatesMatchingTranslationKey: string;
  reportTemplateRequiredTranslationKey: string;

  private propagateChange = (_v: any) => { };

  constructor(public translate: TranslateService,
              private reportTemplateService: ReportTemplateService,
              private router: Router,
              private fb: UntypedFormBuilder) {

    this.selectReportTemplateFormGroup = this.fb.group({
      reportTemplate: [null, this.required ? [Validators.required] : []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    if (ReportTemplateType.SUB_REPORT === this.type) {
      this.labelTranslationKey = 'report-template.sub-report';
      this.placeholderTranslationKey = 'report-template.select-sub-report';
      this.openNewTabTranslationKey = 'report-template.open-subreport-new-tab';
      this.noReportTemplatesTranslationKey = 'report-template.no-sub-reports-text';
      this.noReportTemplatesMatchingTranslationKey = 'report-template.no-sub-reports-matching';
      this.reportTemplateRequiredTranslationKey = 'report-template.sub-report-required';
    } else {
      this.labelTranslationKey = 'report-template.report-template';
      this.placeholderTranslationKey = 'report-template.select-report-template';
      this.openNewTabTranslationKey = 'report-template.open-report-template-new-tab';
      this.noReportTemplatesTranslationKey = 'report-template.no-report-templates-text';
      this.noReportTemplatesMatchingTranslationKey = 'report-template.no-report-templates-matching';
      this.reportTemplateRequiredTranslationKey = 'report-template.report-template-required';
    }
    this.filteredReportTemplates = this.selectReportTemplateFormGroup.get('reportTemplate').valueChanges
    .pipe(
      debounceTime(150),
      tap(value => {
        let modelValue: ReportTemplateId;
        if (typeof value === 'string' || !value) {
          modelValue = null;
        } else {
          modelValue = value.id;
        }
        this.updateView(modelValue);
      }),
      map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
      switchMap(name => this.fetchReportTemplates(name) ),
      share()
    );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectReportTemplateFormGroup.disable({emitEvent: false});
      this.reportTemplateAutocomplete.closePanel();
    } else {
      this.selectReportTemplateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ReportTemplateId | null): void {
    this.searchText = '';
    if (value != null) {
      this.reportTemplateService.getReportTemplateInfo(value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe({
        next: (reportTemplate) => {
          this.modelValue = reportTemplate.id;
          this.reportTemplateURL = getEntityDetailsPageURL(this.modelValue.id, EntityType.REPORT_TEMPLATE);
          this.selectReportTemplateFormGroup.get('reportTemplate').patchValue(reportTemplate, {emitEvent: false});
        },
        error: () => {
          this.modelValue = null;
          this.reportTemplateURL = '';
          this.selectReportTemplateFormGroup.get('reportTemplate').patchValue('', {emitEvent: false});
          if (this.required) {
            this.propagateChange(this.modelValue);
          }
        }
      });
    } else {
      this.modelValue = null;
      this.reportTemplateURL = '';
      this.selectReportTemplateFormGroup.get('reportTemplate').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  updateView(value: ReportTemplateId | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.reportTemplateURL = this.modelValue ? getEntityDetailsPageURL(this.modelValue.id, EntityType.REPORT_TEMPLATE) : '';
      this.propagateChange(this.modelValue);
    }
  }

  displayReportTemplateFn(reportTemplate?: ReportTemplateInfo): string | undefined {
    return reportTemplate ? reportTemplate.name : undefined;
  }

  private fetchReportTemplates(searchText?: string): Observable<Array<ReportTemplateInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(25, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getReportTemplates(pageLink).pipe(
      catchError(() => of(emptyPageData<ReportTemplateInfo>())),
      map(pageData => pageData.data)
    );
  }

  private getReportTemplates(pageLink: PageLink): Observable<PageData<ReportTemplateInfo>> {
    const filter: ReportTemplateFilter = {
      typeList: this.type ? [this.type] : null,
      formatList: this.format ? [this.format] : null,
      includeCustomers: this.includeCustomers
    };
    const query = new ReportTemplateQuery(pageLink, filter);
    return this.reportTemplateService.getAllReportTemplateInfos(query, {ignoreLoading: true});
  }

  onFocus() {
    if (this.dirty) {
      this.selectReportTemplateFormGroup.get('reportTemplate').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.selectReportTemplateFormGroup.get('reportTemplate').patchValue('');
    setTimeout(() => {
      this.reportTemplateInput.nativeElement.blur();
      this.reportTemplateInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  openReportTemplateNewTab($event: Event) {
    $event.stopPropagation();
    const url = this.router.serializeUrl(this.router.createUrlTree([this.reportTemplateURL]));
    window.open(url, '_blank');
  }
}
