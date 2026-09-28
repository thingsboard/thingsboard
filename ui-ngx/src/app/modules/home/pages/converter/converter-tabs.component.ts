// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { Converter, ConverterDebugInput, ConverterType } from '@shared/models/converter.models';
import { DebugConverterEventBody } from '@shared/models/event.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-converter-tabs',
    templateUrl: './converter-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class ConverterTabsComponent extends EntityTabsComponent<Converter> {

  constructor(protected store: Store<AppState>,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  getConverterFunctionName() {
    return this.translate.instant(this.entity.type === ConverterType.UPLINK ?
      'converter.test-decoder-fuction' : 'converter.test-encoder-fuction')
  }

  onDebugEventSelected(event: DebugConverterEventBody) {
    let metadata = '';
    let msgContent = '';
    let msgType = '';
    let inIntegrationMetadata = '';
    if (this.entity.type === ConverterType.DOWNLINK) {
      const msg = JSON.parse(event.in)[0];
      if (msg?.metadata) {
        metadata = JSON.stringify(msg.metadata);
      }
      if (msg?.msg) {
        msgContent = JSON.stringify(msg.msg);
      }
      if (msg?.msgType) {
        msgType = msg.msgType;
      }
      inIntegrationMetadata = event.metadata;
    } else {
      msgContent = event.in;
      metadata = event.metadata;
      msgType = event.inMessageType;
    }
    const debugIn: ConverterDebugInput = {
      inContentType: event.inMessageType,
      inContent: msgContent,
      inMetadata: metadata,
      inMsgType: msgType,
      inIntegrationMetadata: inIntegrationMetadata
    };
    const convertersTable = this.entitiesTableConfig.getTable();
    const converterComponent = convertersTable ? convertersTable.entityDetailsPanel.entityComponent :
      this.entitiesTableConfig.getEntityDetailsPage().entityComponent;
    (converterComponent as ConverterComponent).showConverterTestDialog(debugIn, true);
  }

}
