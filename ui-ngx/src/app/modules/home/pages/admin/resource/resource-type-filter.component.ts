// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { isNotEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-resource-type-filter',
  templateUrl: './resource-type-filter.component.html',
  styleUrls: ['../../../../../../scss/filter-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ResourceTypeFilterComponent),
      multi: true
    }
  ],
  standalone: false
})
export class ResourceTypeFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('resourceTypeFilterPanel')
  resourceTypeFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @Input() types: string[] = [];

  @Input() typeTranslations: Map<string, string>;

  @Input() typeTitle: string;

  @Input() allTypesTitle: string;

  @Input() filterTitle = 'resource.filter-title';

  @Input() resourceFilterTitle = 'resource.resource-filter-title';

  buttonDisplayValue: string;

  resourceTypeFilterForm: FormGroup = this.fb.group({
    type: ['', []]
  });

  private type = '';
  private resourceTypeFilterOverlayRef: OverlayRef;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    this.updateButtonDisplayValue();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.resourceTypeFilterForm.disable({emitEvent: false});
    } else {
      this.resourceTypeFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(type?: string): void {
    this.type = type ?? '';
    this.updateButtonDisplayValue();
    this.updateResourceTypeFilterForm(this.type);
  }

  toggleResourceTypeFilterPanel($event: Event) {
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
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.resourceTypeFilterOverlayRef = this.overlay.create(config);
    this.resourceTypeFilterOverlayRef.backdropClick().subscribe(() => {
      this.cancel();
    });
    this.resourceTypeFilterOverlayRef.attach(new TemplatePortal(this.resourceTypeFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.resourceTypeFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateResourceTypeFilterForm(this.type);
    this.resourceTypeFilterForm.markAsPristine();
    this.closePanel();
  }

  update() {
    this.type = this.resourceTypeFilterForm.value.type ?? '';
    this.updateButtonDisplayValue();
    this.propagateChange(this.type);
    this.resourceTypeFilterForm.markAsPristine();
    this.closePanel();
  }

  reset() {
    if (isNotEmptyStr(this.resourceTypeFilterForm.value.type)) {
      this.updateResourceTypeFilterForm('');
      this.resourceTypeFilterForm.markAsDirty();
    }
  }

  private closePanel() {
    this.resizeWindows?.unsubscribe();
    this.resourceTypeFilterOverlayRef?.dispose();
  }

  private updateResourceTypeFilterForm(type: string) {
    this.resourceTypeFilterForm.patchValue({
      type
    }, {emitEvent: false});
  }

  private updateButtonDisplayValue() {
    if (isNotEmptyStr(this.type)) {
      const typeName = this.translate.instant(this.typeTranslations?.get(this.type) ?? this.type);
      this.buttonDisplayValue = this.translate.instant(this.filterTitle) + `: ${typeName}`;
    } else {
      this.buttonDisplayValue = this.translate.instant(this.resourceFilterTitle);
    }
  }
}
