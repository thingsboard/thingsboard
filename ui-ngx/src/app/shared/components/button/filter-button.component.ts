// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChild,
  DestroyRef,
  ElementRef,
  forwardRef,
  inject,
  input,
  signal,
  TemplateRef,
  viewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { FocusMonitor, InputModalityDetector } from '@angular/cdk/a11y';
import { ESCAPE, hasModifierKey } from '@angular/cdk/keycodes';
import { MatButton } from '@angular/material/button';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { fromEvent, isObservable, Observable, of, Subscription } from 'rxjs';
import { catchError, filter, switchMap } from 'rxjs/operators';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { isDefinedAndNotNull, isEqual, isObject } from '@core/utils';

export type FilterButtonValueLabelFn<T> = (value: T) => string | Observable<string>;

export interface FilterButtonPanelContext<T> {
  $implicit: FormControl<T>;
}

/**
 * Filter button for entity table headers. Shows the current filter value on the button and
 * opens an overlay panel with Reset/Cancel/Update actions. The panel control is supplied
 * by the host as an <ng-template let-control> bound to the provided FormControl.
 */
@Component({
  selector: 'tb-filter-button',
  templateUrl: './filter-button.component.html',
  styleUrls: ['../../../../scss/filter-button.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterButtonComponent),
      multi: true
    }
  ],
  standalone: false
})
export class FilterButtonComponent<T = any> implements ControlValueAccessor {

  private translate = inject(TranslateService);
  private overlay = inject(Overlay);
  private elementRef = inject(ElementRef);
  private viewContainerRef = inject(ViewContainerRef);
  private destroyRef = inject(DestroyRef);
  private focusMonitor = inject(FocusMonitor);
  private inputModality = inject(InputModalityDetector);

  emptyLabel = input.required<string>();
  valueLabel = input<FilterButtonValueLabelFn<T>>(value => String(value));
  emptyValue = input<T>(null);

  panelContent = contentChild.required<TemplateRef<FilterButtonPanelContext<T>>>(TemplateRef);
  private panelTemplate = viewChild.required<TemplateRef<any>>('filterPanel');
  private filterButton = viewChild.required(MatButton, {read: ElementRef});

  disabled = signal(false);

  filterControl = new FormControl<T>(null);

  private value = signal<T>(null);

  private valueText = toSignal(
    toObservable(computed(() => ({value: this.value(), labelFn: this.valueLabel()}))).pipe(
      switchMap(({value, labelFn}) => {
        if (this.isEmpty(value)) {
          return of(null);
        }
        const label = labelFn(value);
        return (isObservable(label) ? label : of(label)).pipe(catchError(() => of(null)));
      })
    ),
    {initialValue: null}
  );

  buttonDisplayValue = computed(() => {
    if (this.isEmpty(this.value())) {
      return this.translate.instant(this.emptyLabel());
    }
    const filterTitle = this.translate.instant('filter.filter');
    const valueText = this.valueText();
    return valueText ? `${filterTitle}: ${valueText}` : filterTitle;
  });

  private overlayRef: OverlayRef;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor() {
    this.destroyRef.onDestroy(() => this.closePanel(false));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
    if (isDisabled) {
      this.filterControl.disable({emitEvent: false});
    } else {
      this.filterControl.enable({emitEvent: false});
    }
  }

  writeValue(value?: T): void {
    this.value.set(this.normalize(value));
    this.filterControl.setValue(this.value(), {emitEvent: false});
  }

  openPanel($event: Event) {
    $event?.stopPropagation();
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.elementRef)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.overlayRef = this.overlay.create(config);
    this.overlayRef.backdropClick().subscribe(() => this.cancel());
    this.overlayRef.keydownEvents()
      .pipe(filter(event => event.keyCode === ESCAPE && !hasModifierKey(event)))
      .subscribe(event => {
        event.preventDefault();
        this.cancel();
      });
    this.overlayRef.attach(new TemplatePortal(this.panelTemplate(), this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => this.overlayRef.updatePosition());
  }

  cancel() {
    this.filterControl.setValue(this.value(), {emitEvent: false});
    this.filterControl.markAsPristine();
    this.closePanel();
  }

  update() {
    this.value.set(this.normalize(this.filterControl.value));
    this.propagateChange(this.value());
    this.filterControl.markAsPristine();
    this.closePanel();
  }

  reset() {
    if (!this.isEmpty(this.filterControl.value)) {
      this.filterControl.setValue(this.emptyValue(), {emitEvent: false});
      this.filterControl.markAsDirty();
    }
  }

  isChanged(): boolean {
    return this.filterControl.dirty && !isEqual(this.normalize(this.filterControl.value), this.value());
  }

  private closePanel(restoreFocus = true) {
    this.resizeWindows?.unsubscribe();
    this.resizeWindows = null;
    if (this.overlayRef) {
      this.overlayRef.dispose();
      this.overlayRef = null;
      if (restoreFocus) {
        this.focusMonitor.focusVia(this.filterButton(), this.inputModality.mostRecentModality ?? 'program');
      }
    }
  }

  private normalize(value: T): T {
    return this.isEmpty(value) ? this.emptyValue() : value;
  }

  private isEmpty(value: T): boolean {
    if (!isDefinedAndNotNull(value) || value === '') {
      return true;
    }
    return isObject(value) && Object.values(value).every(v => !isDefinedAndNotNull(v) || v === '');
  }
}
