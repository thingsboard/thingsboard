///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, forwardRef, Inject, InjectionToken, Input, OnInit, Optional, TemplateRef, ViewChild, ViewContainerRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { isDefinedAndNotNull } from '@app/core/utils';
import { StringItemsOption } from '@app/shared/components/string-items-list.component';
import { ActionType, actionTypeTranslations, AuditLogFilter, coerceBoolean } from '@app/shared/public-api';
import { TranslateService } from '@ngx-translate/core';


export const AUDIT_LOG_FILTER_CONFIG_DATA = new InjectionToken<any>('AuditLogFilterConfigData');

export interface AuditLogConfigData {
  panelMode: boolean;
  auditLogFilter: AuditLogFilter;
}

@Component({
  selector: 'tb-audit-log-filter',
  templateUrl: './audit-log-filter.component.html',
  styleUrl: './audit-log-filter.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AuditLogFilterComponent),
      multi: true
    }
  ]
})
export class AuditLogFilterComponent implements OnInit, ControlValueAccessor {
  
  @ViewChild('auditLogFilterPanel')
  auditLogFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  ActionType = ActionType;

  actionTypes = Object.values(ActionType);
  
  actionTypeTranslations = actionTypeTranslations;

  panelMode = false;

  buttonDisplayValue = this.translate.instant('audit-log.filter');

  auditLogFilterForm: UntypedFormGroup;

  auditLogOverlayRef: OverlayRef;

  panelResult: AuditLogFilter = null;

  private auditLogFilter: AuditLogFilter;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(AUDIT_LOG_FILTER_CONFIG_DATA)
              private data: AuditLogConfigData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.auditLogFilter = this.data.auditLogFilter;
    }
    this.auditLogFilterForm = this.fb.group({
      types: ['', []]
    });

    this.auditLogFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.updateValidators();
        if (!this.buttonMode) {
          this.auditLogFilterUpdated(this.auditLogFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateAuditLogFilterForm(this.auditLogFilter);
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
      this.auditLogFilterForm.disable({emitEvent: false});
    } else {
      this.auditLogFilterForm.enable({emitEvent: false});
      this.updateValidators();
    }
  }
  
  writeValue(auditLogFilter?: AuditLogFilter): void {
    this.auditLogFilter = auditLogFilter;
    this.updateButtonDisplayValue();
    this.updateAuditLogFilterForm(auditLogFilter);
  }
  
  get predefinedTypeValues (): StringItemsOption[] {
    return [...this.actionTypes].map(type => ({
      name: this.translate.instant(this.actionTypeTranslations.get(type)),
      value: type
    }))
  }

  private updateValidators() {
  }

  toggleAuditLogFilterPanel($event: Event) {
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
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(this.nativeElement)
      .withPositions([connectedPosition]);

    this.auditLogOverlayRef = this.overlay.create(config);
    this.auditLogOverlayRef.backdropClick().subscribe(() => {
      this.auditLogOverlayRef.dispose();
    });
    this.auditLogOverlayRef.attach(new TemplatePortal(this.auditLogFilterPanel,
      this.viewContainerRef));
  }

  cancel() {
    this.updateAuditLogFilterForm(this.auditLogFilter);
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.auditLogOverlayRef.dispose();
    }
  }

  update() {
    this.auditLogFilterUpdated(this.auditLogFilterForm.value);
    if (this.panelMode) {
      this.panelResult = this.auditLogFilter;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.auditLogOverlayRef.dispose();
    }
  }

  private updateAuditLogFilterForm(auditLogFilter?: AuditLogFilter) {
    this.auditLogFilterForm.patchValue({
      types: auditLogFilter?.types,
    }, {emitEvent: false});
    this.updateValidators();
  }

  private auditLogFilterUpdated(auditLogFilter: AuditLogFilter) {
    this.auditLogFilter = auditLogFilter;
    this.updateButtonDisplayValue();
    this.propagateChange(this.auditLogFilter);
  }

  private updateButtonDisplayValue() {
    if (this.buttonMode) {
      const filterTextParts: string[] = [];
       if (isDefinedAndNotNull(this.auditLogFilter?.types)) {
        const types = this.auditLogFilter.types;
        types.forEach(type => filterTextParts.push(this.translate.instant(this.actionTypeTranslations.get(type as ActionType)))) 
      }
      if (!filterTextParts.length) {
        this.buttonDisplayValue = this.translate.instant('audit-log.audit-log-filter-title');
      } else {
        this.buttonDisplayValue = this.translate.instant('audit-log.filter-title') + `: ${filterTextParts.join(', ')}`;
      }
      this.cd.detectChanges();
    }
  }
}
