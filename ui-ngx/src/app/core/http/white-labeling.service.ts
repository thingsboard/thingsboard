// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Inject, Injectable, Renderer2, RendererFactory2, RendererStyleFlags2, DOCUMENT } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  checkWlParams, defaultCollapsedImageUrl, defaultImageUrl,
  defaultLoginWlParams,
  LoginWhiteLabelingParams,
  mergeDefaults,
  Palette,
  PaletteSettings,
  tbAccentPalette,
  tbLoginAccentPalette,
  tbLoginPrimaryPalette,
  tbPrimaryPalette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';
import { Observable, of, ReplaySubject, throwError } from 'rxjs';
import {
  ColorPalette,
  extendDefaultPalette,
  getContrastColor,
  materialColorPalette
} from '@shared/models/material.models';
import { deepClone, isEqual, mergeDeep } from '@core/utils';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { environment as env } from '@env/environment';
import { ActionSettingsChangeWhiteLabeling } from '@core/settings/settings.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import cssjs from '@core/css/css';

import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { MailTemplatesSettings } from '@shared/models/settings.models';
import { docPlatformPrefix } from '@shared/models/constants';
import { MenuId, menuSectionMap } from '@core/services/menu.models';
import { MenuService } from '@core/services/menu.service';

const cssParser = new cssjs();
cssParser.testMode = false;

const applyCustomCss = (customCss: string, isLoginTheme: boolean) => {
  const target = isLoginTheme ? 'tb-login-custom-css' : 'tb-app-custom-css';
  let targetStyle = $(`#${target}`);
  if (!targetStyle.length) {
    targetStyle = $(`<style id="${target}"></style>`);
    $('head').append(targetStyle);
  }
  let css;
  if (customCss && customCss.length) {
    const parsedCss = cssParser.parseCSS(customCss);
    for (const cssObject of parsedCss) {
      if (cssObject.selector.includes(':root')) {
        cssObject.selector = cssObject.selector.replace(':root', '');
      }
    }
    cssParser.cssPreviewNamespace = isLoginTheme ? 'tb-custom-css' : 'tb-default';
    css = cssParser.applyNamespacing(parsedCss);
    if (typeof css !== 'string') {
      css = cssParser.getCSSForEditor(css);
    }
  } else {
    css = '';
  }
  targetStyle.text(css);
};

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingService {

  private changeWhiteLabelingSubject = new ReplaySubject<void>(1);

  private loginLogo: string;
  private loginLogoHeight: number;
  private loginPageBackgroundColor: string;
  private loginShowNameVersion: boolean;
  private loginDarkForeground: boolean;
  private showNameBottom: boolean;
  private platformName: string;
  private platformVersion: string;
  private whiteLabelingEnabled = false;

  public loginLogo$ = this.asWhiteLabelingObservable(() => this.loginLogo);
  public loginLogoHeight$ = this.asWhiteLabelingObservable(() => this.loginLogoHeight);
  public loginPageBackgroundColor$ = this.asWhiteLabelingObservable(() => this.loginPageBackgroundColor);
  public loginShowNameVersion$ = this.asWhiteLabelingObservable(() => this.loginShowNameVersion);
  public loginDarkForeground$ = this.asWhiteLabelingObservable(() => this.loginDarkForeground);
  public showNameBottom$ = this.asWhiteLabelingObservable(() => this.showNameBottom);
  public platformName$ = this.asWhiteLabelingObservable(() => this.platformName);
  public platformVersion$ = this.asWhiteLabelingObservable(() => this.platformVersion);
  public whiteLabelingEnabled$ = this.asWhiteLabelingObservable(() => this.whiteLabelingEnabled);

  private currentWLParams: WhiteLabelingParams;
  private currentLoginWLParams: LoginWhiteLabelingParams;
  private loginWlParams: LoginWhiteLabelingParams;
  private userWlParams: WhiteLabelingParams;

  public isUserWlMode = false;
  private isPreviewWlMode = false;

  public primaryPalette: Palette = {
    type: 'tb-primary',
    colors: tbPrimaryPalette,
    extends: 'teal'
  };

  public accentPalette: Palette = {
    type: 'tb-accent',
    colors: tbAccentPalette,
    extends: 'deep-orange'
  };

  private loginPrimaryPalette: Palette = {
    type: 'tb-primary',
    colors: tbLoginPrimaryPalette,
    extends: 'teal'
  };

  private loginAccentPalette: Palette = {
    type: 'tb-accent',
    colors: tbAccentPalette,
    extends: 'deep-orange'
  };

  private renderer: Renderer2;
  private readonly ROOT: HTMLElement;
  private readonly BODY: HTMLElement;

  constructor(
    private http: HttpClient,
    private store: Store<AppState>,
    rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document: Document,
    private menuService: MenuService
  ) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.ROOT = this.document.documentElement;
    this.BODY = this.document.body;
  }

  public logoImageUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageUrl : '';
  }

  public logoImageUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.logoImageUrl());
  }

  public isDefaultLogoImageUrl$(): Observable<boolean> {
    return this.logoImageUrl$().pipe(
      map((url) => url === defaultImageUrl)
    );
  }

  public logoImageHeight(): number {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageHeight : null;
  }

  public logoImageHeight$(): Observable<number> {
    return this.asWhiteLabelingObservable(() => this.logoImageHeight());
  }

  public collapsedLogoImageUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().collapsedLogoImageUrl : '';
  }

  public collapsedLogoImageUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.collapsedLogoImageUrl());
  }

  public isDefaultCollapsedLogoImageUrl$(): Observable<boolean> {
    return this.collapsedLogoImageUrl$().pipe(
      map((url) => url === defaultCollapsedImageUrl)
    );
  }

  public appTitle(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().appTitle : '';
  }

  public appTitle$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.appTitle());
  }

  public faviconUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().favicon.url : '';
  }

  public getPrimaryPalette(): ColorPalette {
    return this.primaryPalette.colors;
  }

  public getPrimaryColor(hue: string): string {
    return this.primaryPalette.colors[hue];
  }

  public getAccentPalette(): ColorPalette {
    return this.accentPalette.colors;
  }

  public getLoginPrimaryPalette(): ColorPalette {
    return this.loginPrimaryPalette.colors;
  }

  public getLoginAccentPalette(): ColorPalette {
    return this.loginAccentPalette.colors;
  }

  public isPrimaryColorPanels(): boolean {
    return !!this.getCurrentWlParams()?.primaryColorPanels;
  }

  public isPrimaryColorPanels$(): Observable<boolean> {
    return this.asWhiteLabelingObservable(() => this.isPrimaryColorPanels());
  }

  public getDocsUrl(): string {
    return `${this.getHelpLinkBaseUrl()}/docs${docPlatformPrefix}/`;
  }

  public getHelpLinkBaseUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().helpLinkBaseUrl : '';
  }

  public getHelpLinkBaseUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getHelpLinkBaseUrl());
  }

  public getUiHelpBaseUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().uiHelpBaseUrl : '';
  }

  public getUiHelpBaseUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getUiHelpBaseUrl());
  }

  public isEnableHelpLinks(): boolean {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().enableHelpLinks : true;
  }

  public isEnableHelpLinks$(): Observable<boolean> {
    return this.asWhiteLabelingObservable(() => this.isEnableHelpLinks());
  }

  public isShowVersion(): boolean {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().showNameVersion : false;
  }

  public isShowVersion$(): Observable<boolean> {
    return this.asWhiteLabelingObservable(() => this.isShowVersion());
  }

  public getPlatformName(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().platformName : '';
  }

  public getTrendzName(): string {
    const isOverrideTrendzName = this.getCurrentWlParams() ? this.getCurrentWlParams().overrideTrendzName : false;
    return isOverrideTrendzName ? 'trendz-analytics.advanced-analytics' : 'trendz-analytics.trendz-analytics';
  }

  public getPlatformName$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getPlatformName());
  }

  public getPlatformVersion(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().platformVersion : '';
  }

  public getPlatformVersion$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getPlatformVersion());
  }

  public getHideConnectivityDialog(): boolean {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().hideConnectivityDialog : false;
  }

  public loadLoginWhiteLabelingParams(): Observable<LoginWhiteLabelingParams> {
    return this.http.get<LoginWhiteLabelingParams>('/api/noauth/whiteLabel/loginWhiteLabelParams').pipe(
      mergeMap((loginWlParams) => {
        this.loginWlParams = mergeDefaults(loginWlParams, defaultLoginWlParams);
        return this.onLoginWlParamsLoaded().pipe(map(() => this.loginWlParams));
      }),
      catchError((err) => {
        if (this.loginWlParams) {
          return this.onLoginWlParamsLoaded().pipe(map(() => this.loginWlParams));
        } else {
          return throwError(err);
        }
      })
    );
  }

  private onLoginWlParamsLoaded(): Observable<any> {
    const loginWlChanged = this.setLoginWlParams(this.loginWlParams);
    let observable: Observable<any>;
    if (loginWlChanged) {
      this.applyLoginWlParams(this.currentLoginWLParams);
      applyCustomCss(this.currentLoginWLParams.customCss, true);
      observable = this.applyLoginThemePalettes(this.currentLoginWLParams.paletteSettings);
    } else {
      observable = of(null);
    }
    if (loginWlChanged || this.isUserWlMode) {
      this.isUserWlMode = false;
      observable = observable.pipe(tap(() => this.notifyWlChanged()));
    }
    return observable;
  }

  public loadUserWhiteLabelingParams(): Observable<WhiteLabelingParams> {
    return this.http.get<WhiteLabelingParams>('/api/whiteLabel/whiteLabelParams').pipe(
      mergeMap((userWlParams) => {
        this.whiteLabelingEnabled = userWlParams.whiteLabelingEnabled;
        this.userWlParams = mergeDefaults(userWlParams);
        return this.onUserWlParamsLoaded().pipe(map(() => this.userWlParams));
      }),
      catchError((err) => {
        if (this.userWlParams) {
          return this.onUserWlParamsLoaded().pipe(map(() => this.userWlParams));
        } else {
          return throwError(err);
        }
      })
    );
  }

  private onUserWlParamsLoaded(): Observable<any> {
    if (this.setWlParams(this.userWlParams) || !this.isUserWlMode) {
      this.isUserWlMode = true;
      return this.wlChanged();
    } else {
      return of(null);
    }
  }

  public whiteLabelPreview(wLParams: WhiteLabelingParams): Observable<WhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/previewWhiteLabelParams', wLParams).pipe(
      mergeMap((previewWlParams) => {
        this.currentWLParams = mergeDefaults(previewWlParams);
        this.isPreviewWlMode = true;
        return this.wlChanged().pipe(map(() => previewWlParams));
      })
    );
  }

  public cancelWhiteLabelPreview(): Observable<any> {
    if (this.isPreviewWlMode) {
      this.isPreviewWlMode = false;
      this.currentWLParams = this.userWlParams;
      return this.wlChanged();
    } else {
      return of(null);
    }
  }

  public getCurrentWhiteLabelParams(): Observable<WhiteLabelingParams> {
    return this.http.get<WhiteLabelingParams>('/api/whiteLabel/currentWhiteLabelParams').pipe(
      map((wlParams) => checkWlParams(wlParams))
    );
  }

  public getCurrentLoginWhiteLabelParams(): Observable<LoginWhiteLabelingParams> {
    return this.http.get<LoginWhiteLabelingParams>('/api/whiteLabel/currentLoginWhiteLabelParams').pipe(
      map((wlParams) => checkWlParams(wlParams))
    );
  }

  public saveWhiteLabelParams(wlParams: WhiteLabelingParams): Observable<WhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/whiteLabelParams', wlParams).pipe(
      mergeMap(() => this.loadUserWhiteLabelingParams())
    );
  }

  public saveLoginWhiteLabelParams(wlParams: LoginWhiteLabelingParams): Observable<LoginWhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/loginWhiteLabelParams', wlParams);
  }

  public deleteCurrentLoginWhiteLabelParams(config?: RequestConfig): Observable<void> {
    return this.http.delete<void>('/api/whiteLabel/currentLoginWhiteLabelParams', defaultHttpOptionsFromConfig(config))
  }

  public deleteCurrentWhiteLabelParams(config?: RequestConfig) {
    return this.http.delete<void>('/api/whiteLabel/currentWhiteLabelParams', defaultHttpOptionsFromConfig(config)).pipe(
      mergeMap(() => this.loadUserWhiteLabelingParams())
    );
  }

  public isWhiteLabelingAllowed(): Observable<boolean> {
    return this.http.get<boolean>('/api/whiteLabel/isWhiteLabelingAllowed');
  }

  public isCustomerWhiteLabelingAllowed(): Observable<boolean> {
    return this.http.get<boolean>('/api/whiteLabel/isCustomerWhiteLabelingAllowed');
  }


  public saveMailTemplates(mailTemplates: MailTemplatesSettings, config?: RequestConfig): Observable<MailTemplatesSettings> {
    return this.http.post<MailTemplatesSettings>('/api/whiteLabel/mailTemplates', mailTemplates, defaultHttpOptionsFromConfig(config));
  }

  public getMailTemplates(systemByDefault = false, config?: RequestConfig): Observable<MailTemplatesSettings> {
    return this.http.get<MailTemplatesSettings>(`/api/whiteLabel/mailTemplates?systemByDefault=${systemByDefault}`,
      defaultHttpOptionsFromConfig(config));
  }

  private wlChanged(): Observable<any> {
    applyCustomCss(this.currentWLParams.customCss, false);
    const menu = menuSectionMap.get(MenuId.trendz_analytics);
    const trendzMenuName = this.getTrendzName();
    if (menu.name !== trendzMenuName) {
      menu.name = trendzMenuName;
      this.menuService.buildMenu();
    }
    this.applyPrimaryPanelsColor(this.currentWLParams.primaryColorPanels);
    return this.applyThemePalettes(this.currentWLParams.paletteSettings).pipe(
      tap(() => {
        this.notifyWlChanged();
      })
    );
  }

  private notifyWlChanged() {
    this.store.dispatch(new ActionSettingsChangeWhiteLabeling({}));
    this.changeWhiteLabelingSubject.next();
  }

  private getCurrentWlParams(): WhiteLabelingParams {
    return this.isUserWlMode ? this.currentWLParams : this.currentLoginWLParams;
  }

  private setLoginWlParams(newWlParams: LoginWhiteLabelingParams): boolean {
    if (!isEqual(this.currentLoginWLParams, newWlParams)) {
      this.currentLoginWLParams = newWlParams;
      return true;
    } else {
      return false;
    }
  }

  private setWlParams(newWlParams: WhiteLabelingParams) {
    if (!isEqual(this.currentWLParams, newWlParams)) {
      this.currentWLParams = newWlParams;
      return true;
    } else {
      return false;
    }
  }

  private applyThemePalettes(paletteSettings: PaletteSettings): Observable<any> {
    this.primaryPalette = this.configurePalette(paletteSettings.primaryPalette, tbPrimaryPalette, 'tb-primary', 'teal');
    this.accentPalette = this.configurePalette(paletteSettings.accentPalette, tbAccentPalette, 'tb-accent', 'deep-orange');
    this.applyThemeColors(false);
    return of(null);
  }

  private applyLoginThemePalettes(paletteSettings: PaletteSettings): Observable<any> {
    this.loginPrimaryPalette = this.configurePalette(paletteSettings.primaryPalette, tbLoginPrimaryPalette,  'tb-primary', 'teal');
    this.loginAccentPalette = this.configurePalette(paletteSettings.accentPalette, tbLoginAccentPalette, 'tb-accent', 'deep-orange');
    this.applyThemeColors(true);
    return of(null);
  }


  private applyLoginWlParams(wlParams: LoginWhiteLabelingParams) {
    this.loginLogo = wlParams.logoImageUrl;
    this.loginLogoHeight = wlParams.logoImageHeight;
    this.loginPageBackgroundColor = wlParams.pageBackgroundColor;
    this.loginShowNameVersion = wlParams.showNameVersion;
    this.showNameBottom = wlParams.showNameBottom;
    this.platformName = !wlParams.platformName ? 'ThingsBoard' : wlParams.platformName;
    this.platformVersion = !wlParams.platformVersion ? env.tbVersion : wlParams.platformVersion;
    this.loginDarkForeground = wlParams.darkForeground;
  }

  private configurePalette(paletteConfig: Palette, defaultColors: ColorPalette,
                           defaultType: string, defaultExtends: string): Palette {
    if (paletteConfig.type === defaultType) {
      return {
        type: defaultType,
        extends: defaultExtends,
        colors: defaultColors
      };
    } else {
      if (paletteConfig.type !== 'custom') {
        return {
          type: paletteConfig.type,
          extends: paletteConfig.type,
          colors: materialColorPalette[paletteConfig.type]
        };
      } else {
        let extendsPalette: string;
        if (paletteConfig.extends === 'default') {
          extendsPalette = defaultExtends;
          paletteConfig = deepClone(paletteConfig);
          paletteConfig.colors = mergeDeep({}, defaultColors, paletteConfig.colors);
        } else {
          extendsPalette = paletteConfig.extends;
        }
        return {
          type: 'custom',
          extends: extendsPalette,
          colors: extendDefaultPalette(extendsPalette, paletteConfig.colors)
        };
      }
    }
  }

  private applyThemeColors(isLoginTheme: boolean) {
    const primaryPalette = isLoginTheme ? this.loginPrimaryPalette : this.primaryPalette;
    const accentPalette = isLoginTheme ? this.loginAccentPalette : this.accentPalette;
    const primaryPrefix = isLoginTheme ? '--tb-login-primary-' : '--tb-primary-';
    const accentPrefix = isLoginTheme ? '--tb-login-accent-' : '--tb-accent-';
    this.applyPaletteColors(primaryPalette, primaryPrefix);
    this.applyPaletteColors(accentPalette, accentPrefix);
  }

  private applyPaletteColors(palette: Palette, cssVarPrefix: string) {
    for (const hue of Object.keys(palette.colors)) {
      const cssVar = `${cssVarPrefix}${hue}`;
      const color = palette.colors[hue];
      this.renderer.setStyle(this.ROOT, cssVar, color, RendererStyleFlags2.DashCase);
      const contrastCssVar = `${cssVarPrefix}contrast-${hue}`;
      const contrastColor = getContrastColor(palette.extends, hue);
      this.renderer.setStyle(this.ROOT, contrastCssVar, contrastColor, RendererStyleFlags2.DashCase);
    }
  }

  private applyPrimaryPanelsColor(primaryColor = false) {
    if (primaryColor) {
      this.renderer.addClass(this.BODY, 'tb-primary-panels');
    } else {
      this.renderer.removeClass(this.BODY, 'tb-primary-panels');
    }
  }

  private asWhiteLabelingObservable<T>(valueSource: () => T): Observable<T> {
    return this.changeWhiteLabelingSubject.pipe(
      map(() => valueSource())
    );
  }

}
