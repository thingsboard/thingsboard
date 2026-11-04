// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UtilsService } from '@core/services/utils.service';
import { DashboardReportParams, DashboardReportType } from '@shared/models/dashboard-report.models';
import { getDefaultTimezone, Timewindow } from '@shared/models/time/time.models';
import { from, Observable, of, Subject } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';

import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import {
  OpenReportMessage,
  ReportResultMessage,
  WaitWidgetsMessage,
  WindowMessage
} from '@shared/models/window-message.model';
import { CmdUpdateMsg, WebsocketCmd, WebsocketDataMsg } from '@shared/models/telemetry/telemetry.models';
import { CmdWrapper } from '@shared/models/websocket/websocket.models';
import { getFilenameFromHttpHeader } from '@core/utils';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class DashboardReportService {

  reportView = false;
  reportTimewindow: Timewindow = null;
  openReportSubject: Subject<void> = new Subject<void>();

  accessToken: string;
  publicId: string;

  private readonly onWindowMessageListener = this.onWindowMessage.bind(this);

  private receiveWsData: Map<number, boolean> = new Map<number, boolean>();
  private lastWsCommandTimeMs = 0;
  private waitForWidgets: Set<string> = new Set<string>();
  private lastWaitWidgetTimeMs = 0;
  private widgetsCount = 0;
  private lastWaitWidgetsTimeMs = 0;

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private utils: UtilsService,
    private http: HttpClient,
    private router: Router,
    private authService: AuthService
  ) {
  }

  public loadReportParams(): boolean {
    const reportView = this.utils.getQueryParam('reportView') === 'true';
    if (reportView) {
      this.reportView = true;
      this.authService.loadUserFromAccessToken(null).subscribe(
        () => {
          window.addEventListener('message', this.onWindowMessageListener);
          if ((this.window as any).postWebReportResult) {
            this.postReportResult({success: true});
          } else {
            const interval = setInterval(() => {
              if ((this.window as any).postWebReportResult) {
                clearInterval(interval);
                this.postReportResult({success: true});
              }
            }, 20);
          }
        }
      );
    }
    return this.reportView;
  }

  public onSendWsCommands(cmds: CmdWrapper) {
    for (const cmdComand of cmds.cmds as Array<WebsocketCmd>) {
      if (!cmdComand.type.toLowerCase().includes('unsubscribe')
        && typeof cmdComand.cmdId === 'number'
        && !this.receiveWsData.has(cmdComand.cmdId)) {
        this.receiveWsData.set(cmdComand.cmdId, false);
        this.lastWsCommandTimeMs = this.utils.currentPerfTime();
      }
    }
  }

  public onWsCmdUpdateMessage(message: CmdUpdateMsg) {
    const wsDataMsg = message as WebsocketDataMsg;
    if ('cmdId' in wsDataMsg && wsDataMsg.cmdId) {
      this.receiveWsData.set(wsDataMsg.cmdId, true);
    } else if ('subscriptionId' in wsDataMsg && wsDataMsg.subscriptionId) {
      this.receiveWsData.set(wsDataMsg.subscriptionId, true);
    }
  }

  public onDashboardLoaded(widgetsCount: number) {
    this.widgetsCount = widgetsCount;
    this.lastWaitWidgetsTimeMs = this.utils.currentPerfTime();
  }

  public onWaitForMap(): string {
    return this.onWaitForWidget();
  }

  public onWaitForWidget(): string {
    const uuid = this.utils.guid();
    this.waitForWidgets.add(uuid);
    this.lastWaitWidgetTimeMs = this.utils.currentPerfTime();
    return uuid;
  }

  public onMapLoaded(uuid: string): void {
    this.onWidgetLoaded(uuid);
  }

  public onWidgetLoaded(uuid: string): void {
    this.waitForWidgets.delete(uuid);
  }

  public downloadDashboardReport(dashboardId: string, reportType: DashboardReportType, state?: string, timewindow?: Timewindow): Observable<any> {
    const url = `/api/report/${dashboardId}/download`;
    const defaultTz = getDefaultTimezone();
    const reportParams: DashboardReportParams = {
      type: reportType,
      timezone: defaultTz
    };
    if (state) {
      reportParams.state = state;
    }
    if (timewindow) {
      reportParams.timewindow = timewindow;
    }
    return this.downloadReport(url, reportParams);
  }

  public downloadTestReport(reportConfig: DashboardReportParams, reportsServerEndpointUrl?: string): Observable<any> {
    const url = '/api/report/test';
    const params: {[param: string]: string} = {};
    if (reportsServerEndpointUrl) {
      params.reportsServerEndpointUrl = reportsServerEndpointUrl;
    }
    return this.downloadReport(url, reportConfig, params);
  }

  private onWindowMessage(event: MessageEvent) {
    if (event.data) {
      let message: WindowMessage;
      try {
        message = JSON.parse(event.data);
      } catch (e) { /* empty */ }
      if (message && message.type) {
        switch (message.type) {
          case 'openReport':
            const openReportMessage: OpenReportMessage = message.data;
            this.openReport(openReportMessage).subscribe((result) => {
              this.postReportResult(result);
            });
            break;
          case 'waitReportWidgets':
            const waitWidgetsMessage: WaitWidgetsMessage = message.data;
            this.waitWidgets(waitWidgetsMessage.timeout).subscribe((result) => {
              this.postReportResult(result);
            });
            break;
          case 'clearReport':
            this.clearReport().subscribe((result) => {
              const resultMessage: ReportResultMessage = {
                success: result
              };
              if (!result) {
                resultMessage.error = 'Navigation failed while clear report!';
              }
              this.postReportResult(resultMessage);
            });
            break;
        }
      }
    }
  }

  private postReportResult(result: ReportResultMessage) {
    if ((this.window as any).postWebReportResult) {
      (this.window as any).postWebReportResult(result);
    } else {
      this.window.postMessage(JSON.stringify({ type: 'reportResult', data: result}), '*');
    }
  }

  private openReport(openReportMessage: OpenReportMessage): Observable<ReportResultMessage> {
    if (openReportMessage && (openReportMessage.accessToken || openReportMessage.publicId) && openReportMessage.dashboardId) {
      return this.loadUser(openReportMessage.accessToken, openReportMessage.publicId).pipe(
        mergeMap((authenticated) => {
          if (authenticated) {
            if (openReportMessage.reportTimewindow) {
              this.reportTimewindow = openReportMessage.reportTimewindow;
            } else {
              this.reportTimewindow = null;
            }
            let url = `/dashboard/${openReportMessage.dashboardId}`;
            const params = [];
            if (openReportMessage.state) {
              params.push(`state=${openReportMessage.state}`);
            }
            if (params.length) {
              url += `?${params.join('&')}`;
            }
            this.openReportSubject.next();
            this.widgetsCount = 0;
            this.receiveWsData.clear();
            this.waitForWidgets.clear();
            this.lastWaitWidgetsTimeMs = 0;
            this.lastWsCommandTimeMs = this.utils.currentPerfTime();
            this.lastWaitWidgetTimeMs = this.utils.currentPerfTime();
            return from(this.router.navigateByUrl(url, {replaceUrl: true})).pipe(
              mergeMap((result) => {
                if (result) {
                  return this.waitForLayoutReady(openReportMessage.timeout).pipe(
                    map(() => ({ success: true, pageHeight: this.pageHeight() })),
                    catchError((e) => of({ success: false, error: e }))
                  );
                } else {
                  return of({
                    success: false,
                    error: 'Failed to navigate to target dashboard!'
                  });
                }
              }),
              catchError((e) => of({success: false, error: e?.error?.message}))
            );
          } else {
            return of({ success: false, error: 'Authentication failed!' });
          }
        })
      );
    } else {
      return of({ success: false, error: 'Invalid message arguments provided!' });
    }
  }

  private pageHeight(): number {
    let height = 0;
    const gridsterChild = document.getElementById('gridster-child');
    if (gridsterChild) {
      height = Math.round(gridsterChild.scrollHeight);
      const dashboardTitleElements = document.querySelector<HTMLElement>('.tb-dashboard-title');
      if (dashboardTitleElements) {
        height += Math.round(dashboardTitleElements.offsetHeight);
      }
    }
    return height;
  }

  private waitForLayoutReady(timeout = 3000): Observable<any> {
    return from(this.waitForReportPage(timeout)).pipe(
      mergeMap(() => from(this.waitForWebsocketData(timeout)))
    );
  }

  private waitForReportPage(timeout = 3000): Promise<void> {
    return new Promise<void>(
      (resolve, reject) => {
        let waitTime = 0;
        const waitInterval = setInterval(() => {
          if (this.lastWaitWidgetsTimeMs && this.isReportPageDomReady()
            && (this.utils.currentPerfTime() - this.lastWaitWidgetsTimeMs >= 300)) {
            clearInterval(waitInterval);
            resolve();
          } else {
            waitTime += 10;
            if (waitTime >= timeout) {
              clearInterval(waitInterval);
              reject('Wait for report page timed out!');
            }
          }
        }, 10);
      }
    );
  }

  private waitForWebsocketData(timeout = 3000): Promise<void> {
    return new Promise<void>(
      (resolve, reject) => {
        let waitTime = 0;
        const waitInterval = setInterval(() => {
          if ((!this.receiveWsData.size || Array.from(this.receiveWsData.values()).every(val => val)) &&
            (this.utils.currentPerfTime() - this.lastWsCommandTimeMs >= 100)) {
            clearInterval(waitInterval);
            resolve();
          } else {
            waitTime += 10;
            if (waitTime >= timeout) {
              clearInterval(waitInterval);
              reject('Wait for websocket data timed out!');
            }
          }
        }, 10);
      }
    );
  }

  private waitWidgets(timeout = 3000): Observable<ReportResultMessage> {
    return from(this.waitForWidgetsLoaded(timeout)).pipe(
      mergeMap(() => this.waitForLayoutReady(timeout)),
      mergeMap(() => this.waitForWidgetsLoaded(timeout)),
      map(() => ({ success: true })),
      catchError((e) => of({ success: false, error: e }))
    );
  }

  private waitForWidgetsLoaded(timeout = 3000): Promise<void> {
    return new Promise<void>(
      (resolve, reject) => {
        let waitTime = 0;
        const waitInterval = setInterval(() => {
          if (!this.waitForWidgets.size && (this.utils.currentPerfTime() - this.lastWaitWidgetTimeMs >= 100)) {
            clearInterval(waitInterval);
            resolve();
          } else {
            waitTime += 10;
            if (waitTime >= timeout) {
              clearInterval(waitInterval);
              reject('Wait for map tiles timed out!');
            }
          }
        }, 10);
      }
    );
  }

  private isReportPageDomReady(): boolean {
    if ($('section.tb-dashboard-container gridster#gridster-child').not('tb-widget-container gridster#gridster-child').length) {
      const widgets = Array.from($('tb-widget>div.tb-widget-loading'));
      if (widgets.length >= this.widgetsCount && widgets.every(item => item.classList.contains('!hidden'))) {
        return true;
      }
    }
    return false;
  }

  private clearReport(): Observable<boolean> {
    if (this.publicId) {
      this.publicId = null;
      this.authService.logout();
      return of (true);
    } else {
      return from(this.router.navigateByUrl('/empty-page', {replaceUrl: true}));
    }
  }

  private loadUser(accessToken: string, publicId?: string): Observable<boolean> {
    if (publicId && publicId.length) {
      if (this.publicId !== publicId) {
        return this.authService.loadUserFromPublicId(publicId).pipe(
          map((payload) => payload !== null),
          tap((authenticated) => {
            if (authenticated) {
              this.publicId = publicId;
              this.accessToken = null;
            }
          })
        );
      } else {
        return of(true);
      }
    } else if (this.accessToken !== accessToken) {
      return this.authService.loadUserFromAccessToken(accessToken).pipe(
        map((payload) => payload !== null),
        tap((authenticated) => {
          if (authenticated) {
            this.accessToken = accessToken;
            this.publicId = null;
          }
        })
      );
    } else {
      return of(true);
    }
  }

  private downloadReport(url: string, reportParams: DashboardReportParams, params?: {[param: string]: string}): Observable<any> {
    if (!params) {
      params = {};
    }
    return this.http.post(url, reportParams, {
      params,
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = getFilenameFromHttpHeader(headers);
        const contentType = headers.get('content-type');
        const linkElement = this.document.createElement('a');
        const blob = new Blob([response.body], { type: contentType });
        const href = URL.createObjectURL(blob);
        linkElement.setAttribute('href', href);
        linkElement.setAttribute('download', filename);
        linkElement.click();
        setTimeout(() => URL.revokeObjectURL(href), 0);
        return null;
      })
    );
  }
}
