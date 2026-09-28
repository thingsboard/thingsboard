// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  ComponentRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  Type,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {
  AbstractControl,
  AsyncValidator,
  ControlValueAccessor,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { cloneMetadata, deepClone } from '@core/utils';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import {
  CustomSchedulerEventConfigComponent
} from '@home/components/scheduler/config/custom-scheduler-event-config.component';
import { SharedModule } from '@shared/shared.module';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';
import { map, tap } from 'rxjs/operators';
import { OtaUpdateEventConfigComponent } from '@home/components/scheduler/config/ota-update-event-config.component';

@Component({
    selector: 'tb-scheduler-event-template-config',
    template: '<ng-container #configContent></ng-container>',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SchedulerEventTemplateConfigComponent),
            multi: true
        },
        {
            provide: NG_ASYNC_VALIDATORS,
            useExisting: forwardRef(() => SchedulerEventTemplateConfigComponent),
            multi: true
        }],
    standalone: false
})
export class SchedulerEventTemplateConfigComponent implements ControlValueAccessor,
  OnInit, AfterViewInit, OnChanges, OnDestroy, AsyncValidator {

  @ViewChild('configContent', {read: ViewContainerRef, static: true}) configContentContainer: ViewContainerRef;

  private configuration: SchedulerEventConfiguration | null;

  @Input()
  disabled: boolean;

  @Input()
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};

  @Input()
  schedulerEventType: string;

  private customSchedulerEventComponentType: Type<ControlValueAccessor>;
  private configComponentRef: ComponentRef<ControlValueAccessor & Validator>;
  private configComponent: ControlValueAccessor & Validator;

  private configTemplate: string;

  private configComponent$: Subject<ControlValueAccessor & Validator> = null;

  private propagateChange = (_v: any) => { };

  constructor(private dynamicComponentFactoryService: DynamicComponentFactoryService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.loadTemplate();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'schedulerEventType') {
          this.loadTemplate();
        }
      }
    }
  }

  private loadTemplate() {
    if (this.configComponentRef) {
      this.configComponentRef.destroy();
      this.configComponentRef = null;
      this.configComponent = null;
    }
    if (this.customSchedulerEventComponentType) {
      this.dynamicComponentFactoryService.destroyDynamicComponent(this.customSchedulerEventComponentType);
      this.customSchedulerEventComponentType = null;
    }
    if (this.schedulerEventType) {
      this.configTemplate = '<div>Not defined!</div>';
      let componentType: Type<ControlValueAccessor & Validator>;
      const configType = this.schedulerEventConfigTypes[this.schedulerEventType];
      if (configType) {
        if (configType.componentType) {
          componentType = configType.componentType;
          // template = `<${selector} [(ngModel)]="configuration"></${selector}>`;
        } else if (configType.template) {
          this.configTemplate = configType.template;
        }
      }
      this.resolveComponent(componentType, this.configTemplate).subscribe((resolvedComponentType) => {
        this.configContentContainer.clear();
        this.configComponentRef = this.configContentContainer
          .createComponent(resolvedComponentType, {});
        this.configComponent = this.configComponentRef.instance;
        if (this.configComponent instanceof OtaUpdateEventConfigComponent) {
          this.configComponent.schedulerEventType = this.schedulerEventType;
        }
        this.configComponent.registerOnChange((configuration: SchedulerEventConfiguration) => {
          this.updateModel(configuration);
        });
        this.configComponent.setDisabledState(this.disabled);
        this.configComponent.writeValue(this.configuration);
        this.configComponent$?.next(this.configComponent);
      });
    }
  }

  private resolveComponent(componentType: Type<ControlValueAccessor & Validator>,
                           template: string): Observable<Type<ControlValueAccessor & Validator>> {
    if (componentType) {
      return of(componentType);
    } else if (template) {
      class CustomSchedulerEventConfigComponentInstance extends CustomSchedulerEventConfigComponent {
      }
      cloneMetadata(CustomSchedulerEventConfigComponent, CustomSchedulerEventConfigComponentInstance);
      return this.dynamicComponentFactoryService.createDynamicComponent(
        CustomSchedulerEventConfigComponentInstance,
        template,
        [SharedModule]).pipe(
        tap((comp) => {
          this.customSchedulerEventComponentType = comp;
        })
      );
    } else {
      return of(null);
    }
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    if (this.configComponentRef) {
      this.configComponentRef.destroy();
    }
    if (this.customSchedulerEventComponentType) {
      this.dynamicComponentFactoryService.destroyDynamicComponent(this.customSchedulerEventComponentType);
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.configComponent) {
      this.configComponent.setDisabledState(isDisabled);
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.configuration = deepClone(value);
    if (this.configComponent) {
      this.configComponent.writeValue(this.configuration);
    }
  }

  private updateModel(configuration: SchedulerEventConfiguration) {
    this.propagateChange(configuration);
  }

  validate(control: AbstractControl): Observable<ValidationErrors | null> {
    let comp$: Observable<ControlValueAccessor & Validator>;
    if (this.configComponent) {
      comp$ = of(this.configComponent);
    } else if (this.configTemplate) {
      this.configComponent$ = new Subject();
      comp$ = this.configComponent$;
    } else {
      comp$ = of(null);
    }

    return comp$.pipe(
      map((comp) => this.doValidate(comp, control))
    );
  }

  private doValidate(configComponent?: ControlValueAccessor & Validator, control?: AbstractControl): ValidationErrors | null {
    if (configComponent && configComponent.validate) {
      return configComponent.validate(control);
    }

    return null;
  }

}
