///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  createMap,
  mapWidgetDefaultSettings,
  MapWidgetSettings
} from '@home/components/widget/lib/maps/map-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { MapSetting } from '@shared/models/widget/maps/map.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tb-map-widget',
  templateUrl: './map-widget.component.html',
  styleUrls: ['./map-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapWidgetComponent implements OnInit, OnDestroy {

  @ViewChild('mapElement', {static: false})
  mapElement: ElementRef<HTMLElement>;

  settings: MapWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  private map: TbMap<MapSetting>;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.mapWidget = this;
    this.settings = {...mapWidgetDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;
  }

  ngOnDestroy() {
    if (this.map) {
      this.map.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
    this.map = createMap(this.ctx, this.settings, this.mapElement.nativeElement);
  }

}
