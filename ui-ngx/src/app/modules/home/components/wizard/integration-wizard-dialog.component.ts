// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, DestroyRef, Inject, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  getIntegrationHelpLink,
  Integration,
  IntegrationConvertersInfo,
  IntegrationsConvertersInfo,
  IntegrationType,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { filter, map, mergeMap, tap } from 'rxjs/operators';
import { combineLatest, forkJoin, Observable, of, shareReplay } from 'rxjs';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { TranslateService } from '@ngx-translate/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Converter, ConverterSourceType, ConverterType } from '@shared/models/converter.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { deepTrim, guid, isDefinedAndNotNull } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ConverterService } from '@core/http/converter.service';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterId } from '@shared/models/id/converter-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityType } from '@shared/models/entity-type.models';

export interface IntegrationWizardData<T> extends AddEntityDialogData<T>{
  edgeTemplate: boolean;
}

@Component({
    selector: 'tb-integration-wizard',
    templateUrl: './integration-wizard-dialog.component.html',
    styleUrls: ['./integration-wizard-dialog.component.scss'],
    providers: [],
    standalone: false
})
export class IntegrationWizardDialogComponent extends
  DialogComponent<IntegrationWizardDialogComponent, Integration> implements AfterViewInit {

  @ViewChild('addIntegrationWizardStepper', {static: true}) addIntegrationWizardStepper: MatStepper;
  @ViewChild('uplinkDataConverter', {static: true}) uplinkDataConverterComponent: ConverterComponent;
  @ViewChild('downlinkDataConverter') downlinkDataConverterComponent: ConverterComponent;

  selectedIndex = 0;
  converterType = ConverterType;
  ConverterSourceType = ConverterSourceType;
  isEdgeTemplate = false;
  showCheckConnection = false;
  integrationType = '';
  showCheckSuccess = false;
  checkErrMsg = '';
  showDownlinkStep = true;

  stepperOrientation: Observable<StepperOrientation>;

  integrationWizardForm: UntypedFormGroup;
  uplinkConverterForm: UntypedFormGroup;
  downlinkConverterForm: UntypedFormGroup;
  integrationConfigurationForm: UntypedFormGroup;
  integrationInfo$: Observable<IntegrationConvertersInfo>;

  uplinkConverter = {
    type: ConverterType.UPLINK,
    debugSettings: { allEnabled: true, failuresEnabled: true },
    integrationType: null,
  } as Converter;

  downlinkConverter = {
    type: ConverterType.DOWNLINK,
    debugSettings: { allEnabled: true, failuresEnabled: true },
    integrationType: null,
  } as Converter;

  EntityType = EntityType;

  private checkConnectionAllow = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: IntegrationWizardData<Integration>,
              public dialogRef: MatDialogRef<IntegrationWizardDialogComponent, Integration>,
              private breakpointObserver: BreakpointObserver,
              private converterService: ConverterService,
              private integrationService: IntegrationService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);

    this.isEdgeTemplate = this.data.edgeTemplate;

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.integrationWizardForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      type: [null, [Validators.required]],
      enabled: [true],
      debugSettings: [{ allEnabled: true, failuresEnabled: true }],
      allowCreateDevicesOrAssets: [true],
    });

    this.integrationWizardForm.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: IntegrationType) => {
      if (integrationTypeInfoMap.has(value)) {
        this.integrationType = this.translate.instant(integrationTypeInfoMap.get(value).name);
        this.integrationWizardForm.get('name').patchValue( this.translate.instant('integration.integration-name', {
          integrationType: this.translate.instant(integrationTypeInfoMap.get(value).name)
        }), {emitEvent: false});
        this.checkConnectionAllow = integrationTypeInfoMap.get(value).checkConnection || false;
        this.showDownlinkStep = !integrationTypeInfoMap.get(value).hideDownlink;
        if (integrationTypeInfoMap.get(value).remote) {
          this.integrationConfigurationForm.get('remote').disable({emitEvent: true});
          this.integrationConfigurationForm.get('remote').setValue(true, {emitEvent: true});
        } else {
          this.integrationConfigurationForm.get('remote').enable({emitEvent: true});
          this.integrationConfigurationForm.get('remote').setValue(false, {emitEvent: true});
        }
        this.uplinkConverter = {...this.uplinkConverter, integrationType: value};
        this.downlinkConverter = {...this.downlinkConverter, integrationType: value};
      } else {
        this.integrationWizardForm.get('name').patchValue('', {emitEvent: false});
        this.integrationType = '';
        this.uplinkConverter = {...this.uplinkConverter, integrationType: null};
        this.downlinkConverter = {...this.downlinkConverter, integrationType: null};
      }
      this.integrationConfigurationForm.get('configuration').setValue(null);
      this.uplinkConverterForm.get('uplinkConverterId').patchValue(null, {emitEvent: false});
      this.downlinkConverterForm.get('downlinkConverterId').patchValue(null, {emitEvent: false});
      this.uplinkConverterForm.get('converterType').patchValue(ConverterSourceType.NEW, {emitEvent: true});
      this.downlinkConverterForm.get('converterType').patchValue(ConverterSourceType.SKIP, {emitEvent: true});
    });

    this.uplinkConverterForm = this.fb.group({
      uplinkConverterId: [{value: null, disabled: true}, Validators.required],
      converterType: [ConverterSourceType.NEW],
      newUplinkConverter: [{
          type: ConverterType.UPLINK
        }]
    });

    this.downlinkConverterForm = this.fb.group({
      downlinkConverterId: [null],
      converterType: [ConverterSourceType.SKIP],
      newDownlinkConverter: [{
        value: {
          type: ConverterType.DOWNLINK
        },
        disable: true
      }]
    });

    this.integrationConfigurationForm = this.fb.group({
      configuration: [{}, Validators.required],
      metadata: [{}],
      remote: [false],
      routingKey: [{value: guid(), disabled: true}],
      secret: [{value: this.generateSecret(20), disabled: true}],
      additionalInfo: this.fb.group(
        {
          description: ['']
        }
      )
    });

    this.uplinkConverterForm.get('converterType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      switch (value) {
        case ConverterSourceType.EXISTING:
          this.uplinkConverterForm.get('uplinkConverterId').enable({emitEvent: false});
          this.uplinkConverterForm.get('newUplinkConverter').disable({emitEvent: false});
          break;
        default:
          this.uplinkConverterForm.get('uplinkConverterId').disable({emitEvent: false});
          this.uplinkConverterForm.get('newUplinkConverter').enable({emitEvent: false});
          this.uplinkDataConverterComponent.updatedValidators();
          break;
      }
    });

    this.downlinkConverterForm.get('converterType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      switch (value) {
        case ConverterSourceType.EXISTING:
          this.downlinkConverterForm.get('downlinkConverterId').enable({emitEvent: false});
          this.downlinkConverterForm.get('newDownlinkConverter').disable({emitEvent: false});
          break;
        case ConverterSourceType.SKIP:
          this.downlinkConverterForm.get('downlinkConverterId').disable({emitEvent: false});
          this.downlinkConverterForm.get('newDownlinkConverter').disable({emitEvent: false});
          break;
        default:
          this.downlinkConverterForm.get('downlinkConverterId').disable({emitEvent: false});
          this.downlinkConverterForm.get('newDownlinkConverter').enable({emitEvent: false});
          this.downlinkDataConverterComponent.updatedValidators();
          break;
      }
    });

    this.updateIntegrationsInfo();
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.uplinkConverterForm.setControl('newUplinkConverter', this.uplinkDataConverterComponent.entityForm, {emitEvent: false});
      this.downlinkConverterForm.setControl('newDownlinkConverter', this.downlinkDataConverterComponent.entityForm, {emitEvent: false});
      this.downlinkConverterForm.get('newDownlinkConverter').disable({emitEvent: false});
    }, 0);
  }

  public createConvertorName(type: ConverterType) {
    const name = this.integrationWizardForm.get('name').value;
    return isDefinedAndNotNull(name) ? this.translate.instant('integration.data-convertor-name', {
      convertorType: type.charAt(0) + type.slice(1).toLowerCase(),
      integrationName: name
    }) : '';
  }

  add(): void {
    if (this.allValid()) {
      forkJoin([
        this.createUplinkConverter(),
        this.createDownlinkConverter()
      ]).pipe(
        mergeMap(([uplinkId, downlinkId]) => this.createdIntegration(uplinkId, downlinkId))
      ).subscribe(
        (device) => {
          this.dialogRef.close(device);
        }
      );
    }
  }

  get helpLinkId(): string {
    return getIntegrationHelpLink(this.integrationWizardForm.value);
  }

  private createUplinkConverter(): Observable<ConverterId> {
    const converterType = this.uplinkConverterForm.get('converterType').value;
    if (converterType === ConverterSourceType.EXISTING) {
      return of(this.uplinkConverterForm.get('uplinkConverterId').value);
    } else {
      const converterConfig: Converter = deepTrim(this.uplinkConverterForm.get('newUplinkConverter').getRawValue());
      converterConfig.edgeTemplate = this.data.edgeTemplate;
      return this.converterService.saveConverter(converterConfig).pipe(
        tap(converter => {
          this.uplinkConverterForm.patchValue({
            converterType: ConverterSourceType.EXISTING,
            uplinkConverterId: converter.id
          });
        }),
        map(converter => converter.id)
      );
    }
  }

  private createDownlinkConverter(): Observable<ConverterId> {
    const converterType = this.downlinkConverterForm.get('converterType').value;
    if (converterType === ConverterSourceType.SKIP) {
      return of(null);
    } else if (converterType === ConverterSourceType.EXISTING) {
      return of(this.downlinkConverterForm.get('downlinkConverterId').value);
    } else {
      const converterConfig: Converter = deepTrim(this.downlinkConverterForm.get('newDownlinkConverter').getRawValue());
      converterConfig.edgeTemplate = this.data.edgeTemplate;
      return this.converterService.saveConverter(converterConfig).pipe(
        tap(converter => {
          this.downlinkConverterForm.patchValue({
            converterType: ConverterSourceType.EXISTING,
            downlinkConverterId: converter.id
          });
        }),
        map(converter => converter.id)
      );
    }
  }

  private getIntegrationData(uplinkConverterId?: ConverterId, downlinkConverterId?: ConverterId): Integration {
    const integrationData: Integration = {
      configuration: {
        metadata: this.integrationConfigurationForm.value.metadata,
        ...this.integrationConfigurationForm.value.configuration,
      },
      routingKey: this.integrationConfigurationForm.getRawValue().routingKey,
      secret: this.integrationConfigurationForm.getRawValue().secret,
      remote: this.integrationConfigurationForm.getRawValue().remote,
      defaultConverterId: uplinkConverterId,
      downlinkConverterId,
      name: this.integrationWizardForm.value.name.trim(),
      type: this.integrationWizardForm.value.type,
      enabled: this.integrationWizardForm.value.enabled,
      debugSettings: this.integrationWizardForm.value.debugSettings,
      allowCreateDevicesOrAssets: this.integrationWizardForm.value.allowCreateDevicesOrAssets,
      edgeTemplate: this.data.edgeTemplate
    };
    if (this.integrationConfigurationForm.value.additionalInfo.description) {
      integrationData.additionalInfo = {
        description: this.integrationConfigurationForm.value.additionalInfo.description
      };
    }
    return integrationData;
  }

  private createdIntegration(uplinkConverterId: ConverterId, downlinkConverterId: ConverterId): Observable<Integration> {
    const integrationData = this.getIntegrationData(uplinkConverterId, downlinkConverterId);
    return this.integrationService.saveIntegration(integrationData);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private get maxStep(): number {
    return this.showDownlinkStep ? 3 : 2;
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
    if (this.isConnectionStep) {
      this.showCheckConnection = false;
    }
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStep) {
      this.add();
    } else {
      this.addIntegrationWizardStepper.next();
    }
  }

  nextStepLabel(): string {
    if (this.showDownlinkStep && this.selectedIndex === 2 && this.downlinkConverterForm.get('converterType').value === ConverterSourceType.SKIP) {
      return 'action.skip';
    }
    if (this.selectedIndex >= this.maxStep) {
      return 'action.add';
    }
    return 'action.next';
  }

  backStep() {
    this.addIntegrationWizardStepper.previous();
  }

  onIntegrationInfoCopied(type: string) {
    const message = type === 'key' ? 'integration.integration-key-copied-message'
      : 'integration.integration-secret-copied-message';
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(message),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
  }

  onIntegrationCheck(): void {
    if (this.allValid()) {
      this.showCheckConnection = true;
      this.showCheckSuccess = false;
      this.checkErrMsg = '';
      setTimeout(() => {
        this.addIntegrationWizardStepper.next();
      }, 0);
      const integrationData = this.getIntegrationData(null, null);
      this.integrationService.checkIntegrationConnection(integrationData, {
        ignoreErrors: true,
        ignoreLoading: true
      }).subscribe(() => {
        this.showCheckSuccess = true;
      }, (error) => {
        this.checkErrMsg = error.error.message;
      });
    }
  }

  get isCheckConnectionAvailable(): boolean {
    return !this.isEdgeTemplate && this.checkConnectionAllow && !this.isRemoteIntegration && this.isConnectionStep;
  }

  private updateIntegrationsInfo(): void {
    this.integrationInfo$ = combineLatest([
      this.integrationWizardForm.get('type').valueChanges,
      this.integrationService.getIntegrationsConvertersInfo()
    ])
      .pipe(
        filter(([selectedType, _]) => !!selectedType),
        map(([selectedType, integrationsInfo]: [IntegrationType, IntegrationsConvertersInfo]) => {
          const convertersInfo = integrationsInfo[selectedType];
          const downlinkConverterTypeControl = this.downlinkConverterForm.get('converterType');
          const uplinkConverterTypeControl = this.uplinkConverterForm.get('converterType');

          if (uplinkConverterTypeControl.value !== ConverterSourceType.NEW && convertersInfo && !convertersInfo.uplink[uplinkConverterTypeControl.value]) {
            uplinkConverterTypeControl.patchValue(ConverterSourceType.NEW);
          }
          if (convertersInfo
            && downlinkConverterTypeControl.value !== ConverterSourceType.SKIP
            && downlinkConverterTypeControl.value !== ConverterSourceType.NEW
            && !convertersInfo.downlink[downlinkConverterTypeControl.value]) {
            downlinkConverterTypeControl.patchValue(ConverterSourceType.SKIP);
          }
          return convertersInfo;
        }),
        shareReplay(1)
      );
  }

  private get isRemoteIntegration(): boolean {
    return this.integrationConfigurationForm.value.remote;
  }

  private get isConnectionStep(): boolean {
    return this.selectedIndex === this.maxStep;
  }

  private generateSecret(length: number = 1): string {
    const l = length > 10 ? 10 : length;
    const str = Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }

  private allValid(): boolean {
    if (this.addIntegrationWizardStepper.steps.find((item, index) => {
      if (item.stepControl?.invalid) {
        item.interacted = true;
        this.addIntegrationWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    } )) {
      return false;
    } else {
      return true;
    }
  }
}
