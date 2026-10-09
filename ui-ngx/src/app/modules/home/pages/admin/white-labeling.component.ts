// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute } from '@angular/router';
import { FormGroupDirective, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import {
  defaultLoginWlParams, defaultWLParams,
  LoginWhiteLabelingParams, Palette, tbAccentPalette, tbLoginAccentPalette, tbLoginPrimaryPalette, tbPrimaryPalette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { environment as env } from '@env/environment';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { merge, mergeMap, Observable } from 'rxjs';
import { isDefined, isEqual, isUndefinedOrNull } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { CustomCssDialogComponent, CustomCssDialogData } from '@home/pages/admin/custom-css-dialog.component';
import { UiSettingsService } from '@core/http/ui-settings.service';
import { share } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { DomainDialogComponent } from '@home/pages/admin/oauth2/domains/domain-dialog.component';
import { Domain } from '@shared/models/oauth2.models';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { ColorPalette, materialColorPalette } from '@shared/models/material.models';

@Component({
    selector: 'tb-white-labeling',
    templateUrl: './white-labeling.component.html',
    styleUrls: ['./settings-card.scss', './white-labeling.component.scss'],
    standalone: false
})
export class WhiteLabelingComponent extends PageComponent implements OnInit, HasConfirmForm {

  wlSettings: UntypedFormGroup;
  private whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams;

  authUser = getCurrentAuthUser(this.store);

  isSysAdmin = this.authUser.authority === Authority.SYS_ADMIN;
  isTenant = this.authUser.authority === Authority.TENANT_ADMIN;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);
  isLoginWl: boolean = this.route.snapshot.data.isLoginWl;

  uiHelpBaseUrlPlaceholder$ = this.uiSettingsService.getHelpBaseUrl().pipe(
    share()
  );

  thingsboardVersion = env.tbVersion;

  showPosition = [
    {
      name: 'white-labeling.position.under-logo',
      value: false
    },
    {
      name: 'white-labeling.position.bottom',
      value: true
    }
  ];

  readonly EntityType = EntityType;
  readonly operation = Operation;
  readonly resource = Resource;

  get logoHeightPlaceholder(): string {
    return this.defaultLogoHeight + '';
  }

  private get defaultLogoHeight(): number {
    return (this.isLoginWl ? defaultLoginWlParams.logoImageHeight : defaultWLParams.logoImageHeight) ?? 0;
  }

  get hasCustomCSS(): boolean {
    return this.whiteLabelingParams?.customCss?.trim().length > 0;
  }

  get defaultLoginPageBackgroundColor(): string {
    return defaultLoginWlParams.pageBackgroundColor;
  }

  selectedPrimaryColor: string;
  selectedAccentColor: string;

  selectedLoginPageBackgroundColor: string;
  selectedLoginPageForegroundColor: string;

  private primaryColorPanelsUnset = false;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService,
              private whiteLabelingService: WhiteLabelingService,
              private uiSettingsService: UiSettingsService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super();
  }

  ngOnInit() {
    this.buildWhiteLabelingSettingsForm();
    this.loadWhiteLabelingParams();
  }

  onLogoHeightKeydown(event: KeyboardEvent): void {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') {
      return;
    }
    if (this.hasLogoHeightValue()) {
      return;
    }
    event.preventDefault();
    const delta = event.key === 'ArrowUp' ? 1 : -1;
    this.wlSettings.get('logoImageHeight').setValue(this.defaultLogoHeight + delta);
  }

  onLogoHeightMouseDown(event: MouseEvent): void {
    if (this.hasLogoHeightValue()) {
      return;
    }
    const input = event.currentTarget as HTMLInputElement;
    const rect = input.getBoundingClientRect();
    const spinnerWidth = 20;
    if (event.clientX < rect.right - spinnerWidth) {
      return;
    }
    event.preventDefault();
    const isUp = event.clientY < rect.top + rect.height / 2;
    this.wlSettings.get('logoImageHeight').setValue(this.defaultLogoHeight + (isUp ? 1 : -1));
  }

  private hasLogoHeightValue(): boolean {
    const value = this.wlSettings.get('logoImageHeight').value;
    return value !== null && value !== undefined && value !== '' && !Number.isNaN(value);
  }

  private loadWhiteLabelingParams() {
    (this.isLoginWl
        ? this.whiteLabelingService.getCurrentLoginWhiteLabelParams()
        : this.whiteLabelingService.getCurrentWhiteLabelParams()
    ).subscribe((whiteLabelingParams) => {
      this.setWhiteLabelingParams(whiteLabelingParams);
    });
  }

  buildWhiteLabelingSettingsForm() {
    this.wlSettings = this.fb.group({
      appTitle: ['', [Validators.maxLength(256)]],
      favicon: this.fb.group(
      {
        url: [null, []],
        type: [null, []]
      }),
      faviconChecksum: [null, []],
      logoImageUrl: [null, []],
      logoImageChecksum: [null, []],
      logoImageHeight: [null, [Validators.min(1)]],
      collapsedLogoImageUrl: [null, []],
      paletteSettings: this.fb.group(
        {
          primaryPalette: [null, []],
          accentPalette: [null, []]
        }),
      showNameVersion: [null, []],
      platformName: [null, []],
      platformVersion: [null, []]
    });

    if (this.isLoginWl) {
      this.wlSettings.addControl('baseUrl',
        this.fb.control('', [Validators.required, Validators.pattern(/^(https?:\/\/)?(localhost|([\w\-]+\.)+[\w\-]+)(:\d+)?(\/[\w\-._~:\/?#[\]@!$&'()*+,;=%]*)?$/)])
      );
      this.wlSettings.addControl('prohibitDifferentUrl',
        this.fb.control('', [])
      );
    } else {
      this.wlSettings.addControl('primaryColorPanels', this.fb.control(false, []));
      this.wlSettings.addControl('overrideTrendzName', this.fb.control(false, []));
    }

    if (this.isLoginWl && !this.isSysAdmin) {
      this.wlSettings.addControl('domainId',
        this.fb.control(null, Validators.required)
      );
    } else {
      this.wlSettings.addControl('enableHelpLinks',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('helpLinkBaseUrl',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('uiHelpBaseUrl',
        this.fb.control(null, [])
      );
    }
    if (this.isLoginWl) {
      this.wlSettings.addControl('darkForeground',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('pageBackgroundColor',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('showNameBottom',
        this.fb.control(null, [])
      );
    }
    if (!this.isLoginWl && this.isTenant) {
      this.wlSettings.addControl('hideConnectivityDialog',
        this.fb.control(false, [])
      );
    }
    if (this.readonly) {
      this.wlSettings.disable();
    } else {
      this.wlSettings.get('showNameVersion').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators();
      });
      merge(this.wlSettings.get('paletteSettings').get('primaryPalette').valueChanges,
            this.wlSettings.get('paletteSettings').get('accentPalette').valueChanges).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updatePaletteColors();
      });
      if (this.isLoginWl) {
        merge(this.wlSettings.get('pageBackgroundColor').valueChanges,
              this.wlSettings.get('darkForeground').valueChanges).pipe(
           takeUntilDestroyed(this.destroyRef)
        ).subscribe(() => {
          this.updateLoginPageColors();
        });
      }
    }
  }

  private updateValidators() {
    const showNameVersion: boolean = this.wlSettings.get('showNameVersion').value;
    if (showNameVersion) {
      this.wlSettings.get('platformName').setValidators([Validators.required]);
      this.wlSettings.get('platformVersion').setValidators([Validators.required]);
    } else {
      this.wlSettings.get('platformName').setValidators([]);
      this.wlSettings.get('platformVersion').setValidators([]);
    }
    this.wlSettings.get('platformName').updateValueAndValidity();
    this.wlSettings.get('platformVersion').updateValueAndValidity();
  }

  editCustomCss(): void {
    this.dialog.open<CustomCssDialogComponent, CustomCssDialogData, string>(CustomCssDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        minWidth: 'min(600px, 100%)',
        data: {
          customCss: this.whiteLabelingParams.customCss,
          readonly: this.readonly
        }
      }).afterClosed().subscribe((customCss) => {
      if (isDefined(customCss)) {
        if (!isEqual(this.whiteLabelingParams.customCss, customCss)) {
          this.whiteLabelingParams.customCss = customCss;
          this.wlSettings.markAsDirty();
        }
      }
    });
  }

  preview(): void {
    this.whiteLabelingParams = {...this.whiteLabelingParams, ...this.wlSettings.value};
    this.whiteLabelingService.whiteLabelPreview(this.whiteLabelingParams).subscribe();
  }

  save(): void {
    const whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams = {...this.whiteLabelingParams, ...this.wlSettings.value};
    if (whiteLabelingParams.platformName === 'ThingsBoard') {
      whiteLabelingParams.platformName = null;
    }
    if (whiteLabelingParams.platformVersion === env.tbVersion) {
      whiteLabelingParams.platformVersion = null;
    }
    if (this.primaryColorPanelsUnset && this.wlSettings.get('primaryColorPanels').pristine) {
      // Seeded value was not touched, keep it unset to not break inheritance
      whiteLabelingParams.primaryColorPanels = null;
    }
    (this.isLoginWl ? this.whiteLabelingService.saveLoginWhiteLabelParams(whiteLabelingParams) :
        this.whiteLabelingService.saveWhiteLabelParams(whiteLabelingParams)).subscribe(() => {
          this.whiteLabelingParams = whiteLabelingParams;
          if (this.isLoginWl) {
            this.loadWhiteLabelingParams();
          } else {
            this.primaryColorPanelsUnset = isUndefinedOrNull(whiteLabelingParams.primaryColorPanels);
            this.wlSettings.markAsPristine();
          }
    });
  }

  confirmForm(): UntypedFormGroup {
    return this.wlSettings;
  }

  onExit(): Observable<any> {
    return this.whiteLabelingService.cancelWhiteLabelPreview();
  }

  delete(form: FormGroupDirective) {
    const title = this.isLoginWl ? 'white-labeling.reset-login-white-label-title' : 'white-labeling.reset-white-label-title';
    const text = this.isLoginWl ? 'white-labeling.reset-login-white-label-text' : 'white-labeling.reset-white-label-text';
    this.dialogService.confirm(this.translate.instant(title), this.translate.instant(text)).subscribe((res) => {
      if (res) {
        let deleteParams: Observable<LoginWhiteLabelingParams | WhiteLabelingParams>;
        if (this.isLoginWl) {
          deleteParams = this.whiteLabelingService.deleteCurrentLoginWhiteLabelParams().pipe(
            mergeMap(() => this.whiteLabelingService.getCurrentLoginWhiteLabelParams())
          );
        } else {
          deleteParams =  this.whiteLabelingService.deleteCurrentWhiteLabelParams().pipe(
            mergeMap(() => this.whiteLabelingService.getCurrentWhiteLabelParams())
          );
        }
        deleteParams.subscribe((value) => {
          this.setWhiteLabelingParams(value);
          form.resetForm(value);
        })
      }
    })
  }

  private setWhiteLabelingParams(whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams) {
    this.whiteLabelingParams = whiteLabelingParams;
    if (!this.isLoginWl) {
      // API returns the edited level unmerged, so seed the unset value with the applied one
      this.primaryColorPanelsUnset = isUndefinedOrNull(this.whiteLabelingParams.primaryColorPanels);
      if (this.primaryColorPanelsUnset) {
        this.whiteLabelingParams.primaryColorPanels = this.whiteLabelingService.isPrimaryColorPanels();
      }
    }
    this.wlSettings.reset(this.whiteLabelingParams, {emitEvent: false});
    this.updatePaletteColors();
    if (this.isLoginWl) {
      this.updateLoginPageColors();
    } else {
      this.updatePrimaryColorPanels();
    }
    if (!this.readonly) {
      this.updateValidators();
    }
  }

  domainChange(domain: BaseData<EntityId>) {
    const baseUrlFormControl = this.wlSettings.get('baseUrl');
    if (baseUrlFormControl.pristine && !this.whiteLabelingParams?.baseUrl) {
      baseUrlFormControl.patchValue(domain?.name ? this.window.location.protocol + '//' + domain.name : '');
    }
  }

  createDomain(name?: string) {
    this.dialog.open<DomainDialogComponent, {name?: string}, Domain>(DomainDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        name
      }
    }).afterClosed()
      .subscribe((domain) => {
        if (domain) {
          this.wlSettings.get('domainId').patchValue(domain.id);
          this.wlSettings.get('domainId').markAsDirty();
        }
      });
  }

  private updatePaletteColors(): void {
    this.selectedPrimaryColor = this.colorFromPalette(this.wlSettings.get('paletteSettings').get('primaryPalette').value);
    this.selectedAccentColor = this.colorFromPalette(this.wlSettings.get('paletteSettings').get('accentPalette').value, false);
  }

  private colorFromPalette(palette: Palette, isPrimary = true): string {
    const key = !palette?.type ? 'default' : (palette.type === 'custom' ? palette.extends : palette.type);
    let paletteInfo: ColorPalette;
    if (key === 'default') {
      paletteInfo = isPrimary ? (this.isLoginWl ? tbLoginPrimaryPalette : tbPrimaryPalette) : (this.isLoginWl ? tbLoginAccentPalette : tbAccentPalette);
    } else {
      paletteInfo = materialColorPalette[key];
    }
    return palette?.colors && palette.colors['500']
      ? palette.colors['500'] : paletteInfo['500'];
  }

  private updateLoginPageColors(): void {
    this.selectedLoginPageBackgroundColor = this.wlSettings.get('pageBackgroundColor').value || this.defaultLoginPageBackgroundColor;
    this.selectedLoginPageForegroundColor = this.wlSettings.get('darkForeground').value ? '#212121' : '#ffffff';
  }

  private updatePrimaryColorPanels(): void {
    if (this.wlSettings.get('primaryColorPanels').value === null) {
      this.wlSettings.get('primaryColorPanels').patchValue(false, {emitEvent: false});
    }
  }
}
