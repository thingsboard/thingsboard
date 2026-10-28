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
import { EntityType } from '@shared/models/entity-type.models';
import { isNotEmptyStr } from '@core/utils';

interface EntitySubtypeFilterTranslations {
  filterTitle: string;
  entityFilterTitle: string;
  subtypeTitle: string;
}

const entitySubtypeFilterTranslations = new Map<EntityType, EntitySubtypeFilterTranslations>([
  [EntityType.EDGE, {
    filterTitle: 'edge.filter-title',
    entityFilterTitle: 'edge.edge-filter-title',
    subtypeTitle: 'edge.edge-type'
  }],
  [EntityType.ENTITY_VIEW, {
    filterTitle: 'entity-view.filter-title',
    entityFilterTitle: 'entity-view.entity-view-filter-title',
    subtypeTitle: 'entity-view.entity-view-type'
  }]
]);

@Component({
  selector: 'tb-entity-subtype-filter',
  templateUrl: './entity-subtype-filter.component.html',
  styleUrls: ['../../../../../scss/filter-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntitySubtypeFilterComponent),
      multi: true
    }
  ],
  standalone: false
})
export class EntitySubtypeFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('entitySubtypeFilterPanel')
  entitySubtypeFilterPanel: TemplateRef<any>;

  @Input() entityType: EntityType;

  @Input() disabled: boolean;

  translations: EntitySubtypeFilterTranslations;

  buttonDisplayValue: string;

  entitySubtypeFilterForm: FormGroup = this.fb.group({
    subType: ['', []]
  });

  private subType = '';
  private entitySubtypeFilterOverlayRef: OverlayRef;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    this.translations = entitySubtypeFilterTranslations.get(this.entityType);
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
      this.entitySubtypeFilterForm.disable({emitEvent: false});
    } else {
      this.entitySubtypeFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(subType?: string): void {
    this.subType = subType ?? '';
    this.updateButtonDisplayValue();
    this.updateEntitySubtypeFilterForm(this.subType);
  }

  toggleEntitySubtypeFilterPanel($event: Event) {
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

    this.entitySubtypeFilterOverlayRef = this.overlay.create(config);
    this.entitySubtypeFilterOverlayRef.backdropClick().subscribe(() => {
      this.cancel();
    });
    this.entitySubtypeFilterOverlayRef.attach(new TemplatePortal(this.entitySubtypeFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.entitySubtypeFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateEntitySubtypeFilterForm(this.subType);
    this.entitySubtypeFilterForm.markAsPristine();
    this.closePanel();
  }

  update() {
    this.subType = this.entitySubtypeFilterForm.value.subType ?? '';
    this.updateButtonDisplayValue();
    this.propagateChange(this.subType);
    this.entitySubtypeFilterForm.markAsPristine();
    this.closePanel();
  }

  reset() {
    if (isNotEmptyStr(this.entitySubtypeFilterForm.value.subType)) {
      this.updateEntitySubtypeFilterForm('');
      this.entitySubtypeFilterForm.markAsDirty();
    }
  }

  private closePanel() {
    this.resizeWindows?.unsubscribe();
    this.entitySubtypeFilterOverlayRef?.dispose();
  }

  private updateEntitySubtypeFilterForm(subType: string) {
    this.entitySubtypeFilterForm.patchValue({
      subType
    }, {emitEvent: false});
  }

  private updateButtonDisplayValue() {
    if (!this.translations) {
      return;
    }
    if (isNotEmptyStr(this.subType)) {
      this.buttonDisplayValue = this.translate.instant(this.translations.filterTitle) + `: ${this.subType}`;
    } else {
      this.buttonDisplayValue = this.translate.instant(this.translations.entityFilterTitle);
    }
  }
}
