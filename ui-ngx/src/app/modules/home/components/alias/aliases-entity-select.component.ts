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
  Component, HostBinding,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { TooltipPosition } from '@angular/material/tooltip';
import { AliasInfo, IAliasController } from '@core/api/widget-api.models';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  ALIASES_ENTITY_SELECT_PANEL_DATA,
  AliasesEntitySelectPanelComponent,
  AliasesEntitySelectPanelData
} from './aliases-entity-select-panel.component';
import { deepClone } from '@core/utils';
import { AliasFilterType } from '@shared/models/alias.models';

@Component({
    selector: 'tb-aliases-entity-select',
    templateUrl: './aliases-entity-select.component.html',
    styleUrls: ['./aliases-entity-select.component.scss'],
    standalone: false
})
export class AliasesEntitySelectComponent implements OnInit, OnDestroy {

  @HostBinding('class')
  aliasesEntitySelectClass = 'tb-hide';

  aliasControllerValue: IAliasController;

  @Input()
  set aliasController(aliasController: IAliasController) {
    this.aliasControllerValue = aliasController;
    this.setupAliasController(this.aliasControllerValue);
  }

  get aliasController(): IAliasController {
    return this.aliasControllerValue;
  }

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input() disabled: boolean;

  @ViewChild('aliasEntitySelectPanelOrigin') aliasEntitySelectPanelOrigin: CdkOverlayOrigin;

  displayValue: string;
  entityAliasesInfo: {[aliasId: string]: AliasInfo} = {};
  hasSelectableAliasEntities = false;

  private rxSubscriptions = new Array<Subscription>();

  constructor(private translate: TranslateService,
              private overlay: Overlay,
              private cd: ChangeDetectorRef,
              private breakpointObserver: BreakpointObserver,
              private viewContainerRef: ViewContainerRef) {
  }

  private setupAliasController(aliasController: IAliasController) {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
    if (aliasController) {
      this.rxSubscriptions.push(aliasController.entityAliasesChanged.subscribe(
        () => {
          setTimeout(() => {
            this.updateDisplayValue();
            this.updateEntityAliasesInfo();
            this.cd.detectChanges();
          }, 0);
        }
      ));
      this.rxSubscriptions.push(aliasController.entityAliasResolved.subscribe(
        () => {
          setTimeout(() => {
            this.updateDisplayValue();
            this.updateEntityAliasesInfo();
            this.cd.detectChanges();
          }, 0);
        }
      ));
    }
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  openEditMode() {
    if (this.disabled || !this.hasSelectableAliasEntities) {
      return;
    }
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-aliases-entity-select-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
    });
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top'
    };
    config.positionStrategy = position.flexibleConnectedTo(this.aliasEntitySelectPanelOrigin.elementRef)
      .withPositions([connectedPosition]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createAliasesEntitySelectPanelInjector(
      overlayRef,
      {
        aliasController: this.aliasController,
        entityAliasesInfo: deepClone(this.entityAliasesInfo)
      }
    );
    overlayRef.attach(new ComponentPortal(AliasesEntitySelectPanelComponent, this.viewContainerRef, injector));
  }

  private _createAliasesEntitySelectPanelInjector(overlayRef: OverlayRef, data: AliasesEntitySelectPanelData): Injector {
    const providers: StaticProvider[] = [
      {provide: ALIASES_ENTITY_SELECT_PANEL_DATA, useValue: data},
      {provide: OverlayRef, useValue: overlayRef}
    ];
    return Injector.create({parent: this.viewContainerRef.injector, providers});
  }

  private updateDisplayValue() {
    let displayValue;
    let singleValue = true;
    let currentAliasId;
    const entityAliases = this.aliasController.getEntityAliases();
    for (const aliasId of Object.keys(entityAliases)) {
      const entityAlias = entityAliases[aliasId];
      if (!entityAlias.filter.resolveMultiple) {
        const resolvedAlias = this.aliasController.getInstantAliasInfo(aliasId);
        if (resolvedAlias && resolvedAlias.currentEntity) {
          if (!currentAliasId) {
            currentAliasId = aliasId;
          } else {
            singleValue = false;
            break;
          }
        }
      }
    }
    if (singleValue && currentAliasId) {
      const aliasInfo = this.aliasController.getInstantAliasInfo(currentAliasId);
      displayValue = aliasInfo.currentEntity.name;
    } else {
      displayValue = this.translate.instant('entity.entities');
    }
    this.displayValue = displayValue;
  }

  private updateEntityAliasesInfo() {
    const allEntityAliases = this.aliasController.getEntityAliases();
    this.entityAliasesInfo = {};
    this.hasSelectableAliasEntities = false;
    this.aliasesEntitySelectClass = 'tb-hide';
    for (const aliasId of Object.keys(allEntityAliases)) {
      const aliasInfo = this.aliasController.getInstantAliasInfo(aliasId);
      if (aliasInfo && !aliasInfo.resolveMultiple && aliasInfo.currentEntity
        && aliasInfo.entityFilter && aliasInfo.entityFilter.type !== AliasFilterType.singleEntity) {
        this.entityAliasesInfo[aliasId] = deepClone(aliasInfo);
        this.hasSelectableAliasEntities = true;
        this.aliasesEntitySelectClass = '';
      }
    }
  }

}
