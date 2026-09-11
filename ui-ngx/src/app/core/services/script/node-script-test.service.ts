// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RuleChainService } from '@core/http/rule-chain.service';
import { switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  NodeScriptTestDialogComponent,
  NodeScriptTestDialogData
} from '@shared/components/dialog/node-script-test-dialog.component';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { DebugRuleNodeEventBody } from '@shared/models/event.models';

@Injectable({
  providedIn: 'root'
})
export class NodeScriptTestService {

  constructor(private ruleChainService: RuleChainService,
              public dialog: MatDialog) {
  }

  testNodeScript(script: string, scriptType: string, functionTitle: string,
                 functionName: string, argNames: string[], ruleNodeId: string, helpId?: string,
                 scriptLang?: ScriptLanguage, debugEventBody?: DebugRuleNodeEventBody): Observable<string> {
    if (ruleNodeId && !debugEventBody) {
      return this.ruleChainService.getLatestRuleNodeDebugInput(ruleNodeId).pipe(
        switchMap((debugIn) => {
          return this.openTestScriptDialog(script, scriptType, functionTitle,
            functionName, argNames, debugIn, helpId, scriptLang);
        })
      );
    } else {
      return this.openTestScriptDialog(script, scriptType, functionTitle,
        functionName, argNames, debugEventBody, helpId, scriptLang);
    }
  }

  private openTestScriptDialog(script: string, scriptType: string, functionTitle: string, functionName: string,
                               argNames: string[], eventBody: DebugRuleNodeEventBody, helpId?: string,
                               scriptLang?: ScriptLanguage): Observable<string> {
    let msg: any;
    let metadata: {[key: string]: string};
    let msgType: string;
    if (eventBody && eventBody.data) {
      try {
        msg = JSON.parse(eventBody.data);
      } catch (e) {}
    }
    if (!msg) {
      msg = {
        temperature: 22.4,
        humidity: 78
      };
    }
    if (eventBody && eventBody.metadata) {
      try {
        metadata = JSON.parse(eventBody.metadata);
      } catch (e) {}
    }
    if (!metadata) {
      metadata = {
        deviceName: 'Test Device',
        deviceType: 'default',
        ts: new Date().getTime() + ''
      };
    }
    if (eventBody && eventBody.msgType) {
      msgType = eventBody.msgType;
    } else {
      msgType = 'POST_TELEMETRY_REQUEST';
    }
    return this.dialog.open<NodeScriptTestDialogComponent, NodeScriptTestDialogData, string>(NodeScriptTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          msg,
          metadata,
          msgType,
          functionTitle,
          functionName,
          script,
          scriptType,
          argNames,
          helpId,
          scriptLang
        }
      }).afterClosed();
  }

}
