///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  HostBinding,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { map, switchMap, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { ScadaSymbolData } from '@home/pages/scada-symbol/scada-symbol.models';
import {
  IotSvgMetadata, IotSvgObjectSettings,
  parseIotSvgMetadataFromContent,
  updateIotSvgMetadataInContent
} from '@home/components/widget/lib/svg/iot-svg.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { createFileFromContent, deepClone } from '@core/utils';
import {
  ScadaSymbolEditorComponent,
  ScadaSymbolEditorData
} from '@home/pages/scada-symbol/scada-symbol-editor.component';
import { ImageService } from '@core/http/image.service';
import { imageResourceType, IMAGES_URL_PREFIX } from '@shared/models/resource.models';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { IAliasController, IStateController, StateParams } from '@core/api/widget-api.models';
import { EntityAliases } from '@shared/models/alias.models';
import { Filters } from '@shared/models/query/query.models';
import { AliasController } from '@core/api/alias-controller';
import { EntityService } from '@core/http/entity.service';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { Widget, widgetType } from '@shared/models/widget.models';
import {
  iotSvgWidgetDefaultSettings,
  IotSvgWidgetSettings
} from '@home/components/widget/lib/svg/iot-svg-widget.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';

@Component({
  selector: 'tb-scada-symbol',
  templateUrl: './scada-symbol.component.html',
  styleUrls: ['./scada-symbol.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit, HasDirtyFlag {

  widgetType = widgetType;

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  @ViewChild('symbolEditor', {static: false})
  symbolEditor: ScadaSymbolEditorComponent;

  symbolData: ScadaSymbolData;
  symbolEditorData: ScadaSymbolEditorData;

  previewMode = false;

  scadaSymbolFormGroup: UntypedFormGroup;

  scadaPreviewFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  private origSymbolData: ScadaSymbolData;

  updateBreadcrumbs = new EventEmitter();

  aliasController: IAliasController;

  widgetActionCallbacks: WidgetActionCallbacks = {
    fetchDashboardStates: () => [],
    fetchCellClickColumns: () => []
  };

  previewWidgets: Array<Widget> = [];

  tags: string[];

  private previewIotSvgObjectSettings: IotSvgObjectSettings;

  private previewWidget: Widget;

  private forcePristine = false;

  get isDirty(): boolean {
    return (this.scadaSymbolFormGroup.dirty || this.symbolEditor?.dirty) && !this.forcePristine;
  }

  set isDirty(value: boolean) {
    this.forcePristine = !value;
  }

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private entityService: EntityService,
              private utils: UtilsService,
              private translate: TranslateService,
              private imageService: ImageService) {
    super(store);
  }

  ngOnInit(): void {
    this.scadaSymbolFormGroup = this.fb.group({
      metadata: [null]
    });
    this.scadaPreviewFormGroup = this.fb.group({
      iotSvgObject: [null]
    });

    const entitiAliases: EntityAliases = {};

    // @ts-ignore
    const stateController: IStateController = {
      getStateParams: (): StateParams => ({})
    };

    const filters: Filters = {};

    this.aliasController = new AliasController(this.utils,
      this.entityService,
      this.translate,
      () => stateController, entitiAliases, filters);

    this.route.data.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.reset();
        this.init(this.route.snapshot.data.symbolData);
      }
    );
  }

  ngAfterViewInit() {
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  onApplyScadaSymbolConfig() {
    if (this.scadaSymbolFormGroup.valid) {
      const svgContent = this.prepareSvgContent();
      const file = createFileFromContent(svgContent, this.symbolData.imageResource.fileName,
        this.symbolData.imageResource.descriptor.mediaType);
      const type = imageResourceType(this.symbolData.imageResource);
      let imageInfoObservable =
        this.imageService.updateImage(type, this.symbolData.imageResource.resourceKey, file);
      const metadata: IotSvgMetadata = this.scadaSymbolFormGroup.get('metadata').value;
      if (metadata.title !== this.symbolData.imageResource.title) {
        imageInfoObservable = imageInfoObservable.pipe(
          switchMap(imageInfo => {
            imageInfo.title = metadata.title;
            return this.imageService.updateImageInfo(imageInfo);
          })
        );
      }
      imageInfoObservable.pipe(
        switchMap(imageInfo => this.imageService.getImageString(
            `${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(imageInfo.resourceKey)}`).pipe(
              map(content => ({
                imageResource: imageInfo,
                svgContent: content
              }))
          ))
      ).subscribe(data => {
        this.init(data);
        this.updateBreadcrumbs.emit();
      });
    }
  }

  onRevertScadaSymbolConfig() {
    this.init(this.origSymbolData);
  }

  enterPreviewMode() {
    this.symbolData.svgContent = this.prepareSvgContent();
    this.previewIotSvgObjectSettings = {};
    this.scadaPreviewFormGroup.patchValue({
      iotSvgObject: this.previewIotSvgObjectSettings
    }, {emitEvent: false});
    this.scadaPreviewFormGroup.markAsPristine();
    const settings: IotSvgWidgetSettings = {...iotSvgWidgetDefaultSettings,
      ...{iotSvg: null, iotSvgContent: this.symbolData.svgContent, iotSvgObject: this.previewIotSvgObjectSettings}};
    this.previewWidget = {
      typeFullFqn: 'system.iot_svg',
      type: widgetType.rpc,
      sizeX: 24,
      sizeY: 24,
      row: 0,
      col: 0,
      config: {
        settings,
        showTitle: false,
        dropShadow: false,
        padding: '0',
        margin: '0'
      }
    };
    this.previewWidgets = [this.previewWidget];
    this.previewMode = true;
    /*setTimeout(() => {
      this.updatePreviewWidgetSettings();
    }, 2000);*/
  }

  exitPreviewMode() {
    this.symbolEditorData = {
      svgContent: this.symbolData.svgContent
    };
    this.previewMode = false;
  }

  onRevertPreviewSettings() {
    this.scadaPreviewFormGroup.patchValue({
      iotSvgObject: this.previewIotSvgObjectSettings
    }, {emitEvent: false});
    this.scadaPreviewFormGroup.markAsPristine();
  }

  onApplyPreviewSettings() {
    this.scadaPreviewFormGroup.markAsPristine();
    this.previewIotSvgObjectSettings = this.scadaPreviewFormGroup.get('iotSvgObject').value;
    this.updatePreviewWidgetSettings();
  }

  private updatePreviewWidgetSettings() {
    this.previewWidget = deepClone(this.previewWidget);
    this.previewWidget.config.settings.iotSvgObject = this.previewIotSvgObjectSettings;
    this.previewWidgets = [this.previewWidget];
  }

  private prepareSvgContent(): string {
    const svgContent = this.symbolEditor.getContent();
    const metadata: IotSvgMetadata = this.scadaSymbolFormGroup.get('metadata').value;
    return updateIotSvgMetadataInContent(svgContent, metadata);
  }

  private reset(): void {
    this.previewMode = false;
  }

  private init(data: ScadaSymbolData) {
    this.origSymbolData = data;
    this.symbolData = deepClone(data);
    this.symbolEditorData = {
      svgContent: this.symbolData.svgContent
    };
    const metadata = parseIotSvgMetadataFromContent(this.symbolData.svgContent);
    this.scadaSymbolFormGroup.patchValue({
      metadata
    }, {emitEvent: false});
    this.scadaSymbolFormGroup.markAsPristine();
    this.cd.markForCheck();
  }
}

