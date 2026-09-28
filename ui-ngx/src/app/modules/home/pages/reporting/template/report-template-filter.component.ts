// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Inject,
  InjectionToken,
  Input,
  OnInit,
  Optional,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { deepClone } from '@core/utils';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  reportFormats,
  ReportTemplateFilter,
  reportTemplateFiltersEquals,
  reportTemplateTypes,
  reportTemplateTypeTranslationMap
} from '@shared/models/report.models';

export const REPORT_TEMPLATE_FILTER_DATA = new InjectionToken<any>('ReportTemplateFilterData');

export interface ReportTemplateFilterData {
  panelMode: boolean;
  reportTemplateFilter: ReportTemplateFilter;
  initialReportTemplateFilter?: ReportTemplateFilter;
}

// @dynamic
@Component({
    selector: 'tb-report-template-filter',
    templateUrl: './report-template-filter.component.html',
    styleUrls: ['./report-template-filter.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportTemplateFilterComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportTemplateFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('reportTemplateFilterPanel')
  reportTemplateFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @Input()
  initialReportTemplateFilter: ReportTemplateFilter;

  panelMode = false;

  reportTemplateTypes = reportTemplateTypes;

  reportTemplateTypeTranslationMap = reportTemplateTypeTranslationMap;

  reportFormats = reportFormats;

  reportTemplateFilterForm: UntypedFormGroup;

  reportTemplateFilterOverlayRef: OverlayRef;

  panelResult: ReportTemplateFilter = null;

  private reportTemplateFilter: ReportTemplateFilter;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(REPORT_TEMPLATE_FILTER_DATA)
              private data: ReportTemplateFilterData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.reportTemplateFilter = this.data.reportTemplateFilter;
      this.initialReportTemplateFilter = this.data.initialReportTemplateFilter;
      if (this.panelMode && !this.initialReportTemplateFilter) {
        this.initialReportTemplateFilter = deepClone(this.reportTemplateFilter);
      }
    }
    this.reportTemplateFilterForm = this.fb.group({
      type: [null, []],
      format: [null, []]
    });
    this.reportTemplateFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.reportTemplateFilterUpdated(this.reportTemplateFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateReportTemplateFilterForm(this.reportTemplateFilter);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.reportTemplateFilterForm.disable({emitEvent: false});
    } else {
      this.reportTemplateFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(reportTemplateFilter?: ReportTemplateFilter): void {
    this.reportTemplateFilter = reportTemplateFilter;
    if (!this.initialReportTemplateFilter && reportTemplateFilter) {
      this.initialReportTemplateFilter = deepClone(reportTemplateFilter);
    }
    this.updateReportTemplateFilterForm(reportTemplateFilter);
  }

  toggleReportTemplateFilterPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    config.positionStrategy = this.overlay.position()
    .flexibleConnectedTo(this.nativeElement)
    .withPositions([POSITION_MAP.bottomLeft]);

    this.reportTemplateFilterOverlayRef = this.overlay.create(config);
    this.reportTemplateFilterOverlayRef.backdropClick().subscribe(() => {
      this.reportTemplateFilterOverlayRef.dispose();
    });
    this.reportTemplateFilterOverlayRef.attach(new TemplatePortal(this.reportTemplateFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.reportTemplateFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateReportTemplateFilterForm(this.reportTemplateFilter);
    this.reportTemplateFilterForm.markAsPristine();
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.reportTemplateFilterOverlayRef.dispose();
    }
  }

  update() {
    this.reportTemplateFilterUpdated(this.reportTemplateFilterForm.value);
    this.reportTemplateFilterForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.reportTemplateFilter;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.reportTemplateFilterOverlayRef.dispose();
    }
  }

  reset() {
    if (this.initialReportTemplateFilter) {
      if (this.buttonMode || this.panelMode) {
        const reportTemplateFilter = this.reportTemplateFilterFromFormValue(this.reportTemplateFilterForm.value);
        if (!reportTemplateFiltersEquals(reportTemplateFilter, this.initialReportTemplateFilter)) {
          this.updateReportTemplateFilterForm(this.initialReportTemplateFilter);
          this.reportTemplateFilterForm.markAsDirty();
        }
      } else {
        if (!reportTemplateFiltersEquals(this.reportTemplateFilter, this.initialReportTemplateFilter)) {
          this.reportTemplateFilter = this.initialReportTemplateFilter;
          this.updateReportTemplateFilterForm(this.reportTemplateFilter);
          this.propagateChange(this.reportTemplateFilter);
        }
      }
    }
  }

  private updateReportTemplateFilterForm(reportTemplateFilter?: ReportTemplateFilter) {
    this.reportTemplateFilterForm.patchValue({
      type: reportTemplateFilter?.typeList?.length ? reportTemplateFilter.typeList[0] : null,
      format: reportTemplateFilter?.formatList?.length ? reportTemplateFilter.formatList[0] : null
    }, {emitEvent: false});
  }

  private reportTemplateFilterUpdated(formValue: any) {
    this.reportTemplateFilter = this.reportTemplateFilterFromFormValue(formValue);
    this.propagateChange(this.reportTemplateFilter);
  }

  private reportTemplateFilterFromFormValue(formValue: any): ReportTemplateFilter {
    return {
      typeList: formValue.type ? [formValue.type] : null,
      formatList: formValue.format ? [formValue.format] : null
    };
  }

}
