///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { PageComponent } from '@shared/components/page.component';
import { Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { toWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';
import { Widget, WidgetConfig, WidgetType, widgetType, widgetTypesData } from '@shared/models/widget.models';
import { ActivatedRoute, Router } from '@angular/router';
import { deepClone } from '@core/utils';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Hotkey } from 'angular2-hotkeys';
import { TranslateService } from '@ngx-translate/core';
import { getCurrentIsLoading } from '@app/core/interceptors/load.selectors';
import { Ace } from 'ace-builds';
import { getAce, Range } from '@shared/models/ace/ace.models';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { WINDOW } from '@core/services/window.service';
import { WindowMessage } from '@shared/models/window-message.model';
import { ExceptionData } from '@shared/models/error.models';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { MatDialog } from '@angular/material/dialog';
import {
  SaveWidgetTypeAsDialogComponent,
  SaveWidgetTypeAsDialogResult
} from '@home/pages/widget/save-widget-type-as-dialog.component';
import { forkJoin, from, Subscription } from 'rxjs';
import { ResizeObserver } from '@juggle/resize-observer';
import Timeout = NodeJS.Timeout;
import { widgetEditorCompleter } from '@home/pages/widget/widget-editor.models';
import { Observable } from 'rxjs/internal/Observable';
import { map, tap } from 'rxjs/operators';
import { beautifyCss, beautifyHtml, beautifyJs } from '@shared/models/beautify.models';

// @dynamic
@Component({
  selector: 'tb-widget-editor',
  templateUrl: './widget-editor.component.html',
  styleUrls: ['./widget-editor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WidgetEditorComponent extends PageComponent implements OnInit, OnDestroy, HasDirtyFlag {

  @ViewChild('topPanel', {static: true})
  topPanelElmRef: ElementRef;

  @ViewChild('topLeftPanel', {static: true})
  topLeftPanelElmRef: ElementRef;

  @ViewChild('topRightPanel', {static: true})
  topRightPanelElmRef: ElementRef;

  @ViewChild('bottomPanel', {static: true})
  bottomPanelElmRef: ElementRef;

  @ViewChild('javascriptPanel', {static: true})
  javascriptPanelElmRef: ElementRef;

  @ViewChild('framePanel', {static: true})
  framePanelElmRef: ElementRef;

  @ViewChild('htmlInput', {static: true})
  htmlInputElmRef: ElementRef;

  @ViewChild('cssInput', {static: true})
  cssInputElmRef: ElementRef;

  @ViewChild('settingsJsonInput', {static: true})
  settingsJsonInputElmRef: ElementRef;

  @ViewChild('dataKeySettingsJsonInput', {static: true})
  dataKeySettingsJsonInputElmRef: ElementRef;

  @ViewChild('javascriptInput', {static: true})
  javascriptInputElmRef: ElementRef;

  @ViewChild('widgetIFrame', {static: true})
  widgetIFrameElmRef: ElementRef<HTMLIFrameElement>;

  iframe: JQuery<HTMLIFrameElement>;

  widgetTypes = widgetType;
  allWidgetTypes = Object.keys(widgetType);
  widgetTypesDataMap = widgetTypesData;

  authUser: AuthUser;

  isReadOnly: boolean;

  widgetsBundle: WidgetsBundle;
  widgetType: WidgetType;
  widget: WidgetInfo;
  origWidget: WidgetInfo;

  isDirty = false;

  fullscreen = false;
  htmlFullscreen = false;
  cssFullscreen = false;
  jsonSettingsFullscreen = false;
  jsonDataKeySettingsFullscreen = false;
  javascriptFullscreen = false;
  iFrameFullscreen = false;

  aceEditors: Ace.Editor[] = [];
  editorsResizeCafs: {[editorId: string]: CancelAnimationFrame} = {};
  htmlEditor: Ace.Editor;
  cssEditor: Ace.Editor;
  jsonSettingsEditor: Ace.Editor;
  dataKeyJsonSettingsEditor: Ace.Editor;
  jsEditor: Ace.Editor;
  aceResize$: ResizeObserver;

  onWindowMessageListener = this.onWindowMessage.bind(this);

  iframeWidgetEditModeInited = false;
  saveWidgetPending = false;
  saveWidgetAsPending = false;

  gotError = false;
  errorMarkers: number[] = [];
  errorAnnotationId = -1;

  saveWidgetTimeout: Timeout;

  hotKeys: Hotkey[] = [];

  private rxSubscriptions = new Array<Subscription>();

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private route: ActivatedRoute,
              private router: Router,
              private widgetService: WidgetService,
              private translate: TranslateService,
              private raf: RafService,
              private dialog: MatDialog) {
    super(store);

    this.authUser = getCurrentAuthUser(store);

    this.rxSubscriptions.push(this.route.data.subscribe(
      (data) => {
        this.init(data);
      }
    ));

    this.initHotKeys();
  }

  private init(data: any) {
    this.widgetsBundle = data.widgetsBundle;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = !this.widgetsBundle || this.widgetsBundle.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
    this.widgetType = data.widgetEditorData.widgetType;
    this.widget = data.widgetEditorData.widget;
    if (this.widgetType) {
      const config = JSON.parse(this.widget.defaultConfig);
      this.widget.defaultConfig = JSON.stringify(config);
    }
    this.origWidget = deepClone(this.widget);
    if (!this.widgetType) {
      this.isDirty = true;
    }
  }

  ngOnInit(): void {
    this.initSplitLayout();
    this.initAceEditors();
    this.iframe = $(this.widgetIFrameElmRef.nativeElement);
    this.window.addEventListener('message', this.onWindowMessageListener);
    this.iframe.attr('data-widget', JSON.stringify(this.widget));
    this.iframe.attr('src', '/widget-editor');
  }

  ngOnDestroy(): void {
    this.window.removeEventListener('message', this.onWindowMessageListener);
    this.aceResize$.disconnect();
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  private initHotKeys(): void {
    this.hotKeys.push(
      new Hotkey('ctrl+q', (event: KeyboardEvent) => {
        if (!getCurrentIsLoading(this.store) && !this.undoDisabled()) {
          event.preventDefault();
          this.undoWidget();
        }
        return false;
      }, ['INPUT', 'SELECT', 'TEXTAREA'],
        this.translate.instant('widget.undo'))
    );
    this.hotKeys.push(
      new Hotkey('ctrl+s', (event: KeyboardEvent) => {
          if (!getCurrentIsLoading(this.store) && !this.saveDisabled()) {
            event.preventDefault();
            this.saveWidget();
          }
          return false;
        }, ['INPUT', 'SELECT', 'TEXTAREA'],
        this.translate.instant('widget.save'))
    );
    this.hotKeys.push(
      new Hotkey('shift+ctrl+s', (event: KeyboardEvent) => {
          if (!getCurrentIsLoading(this.store) && !this.saveAsDisabled()) {
            event.preventDefault();
            this.saveWidgetAs();
          }
          return false;
        }, ['INPUT', 'SELECT', 'TEXTAREA'],
        this.translate.instant('widget.saveAs'))
    );
    this.hotKeys.push(
      new Hotkey('shift+ctrl+f', (event: KeyboardEvent) => {
          event.preventDefault();
          this.fullscreen = !this.fullscreen;
          return false;
        }, ['INPUT', 'SELECT', 'TEXTAREA'],
        this.translate.instant('widget.toggle-fullscreen'))
    );
    this.hotKeys.push(
      new Hotkey('ctrl+enter', (event: KeyboardEvent) => {
          event.preventDefault();
          this.applyWidgetScript();
          return false;
        }, ['INPUT', 'SELECT', 'TEXTAREA'],
        this.translate.instant('widget.run'))
    );
  }

  private initSplitLayout() {
    Split([this.topPanelElmRef.nativeElement, this.bottomPanelElmRef.nativeElement], {
      sizes: [35, 65],
      gutterSize: 8,
      cursor: 'row-resize',
      direction: 'vertical'
    });
    Split([this.topLeftPanelElmRef.nativeElement, this.topRightPanelElmRef.nativeElement], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
    Split([this.javascriptPanelElmRef.nativeElement, this.framePanelElmRef.nativeElement], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
  }

  private initAceEditors() {
    this.aceResize$ = new ResizeObserver((entries) => {
      entries.forEach((entry) => {
        const editor = this.aceEditors.find(aceEditor => aceEditor.container === entry.target);
        this.onAceEditorResize(editor);
      });
    });

    const editorsObservables: Observable<any>[] = [];


    editorsObservables.push(this.createAceEditor(this.htmlInputElmRef, 'html').pipe(
      tap((editor) => {
        this.htmlEditor = editor;
        this.htmlEditor.on('input', () => {
          const editorValue = this.htmlEditor.getValue();
          if (this.widget.templateHtml !== editorValue) {
            this.widget.templateHtml = editorValue;
            this.isDirty = true;
          }
        });
      })
    ));

    editorsObservables.push(this.createAceEditor(this.cssInputElmRef, 'css').pipe(
      tap((editor) => {
        this.cssEditor = editor;
        this.cssEditor.on('input', () => {
          const editorValue = this.cssEditor.getValue();
          if (this.widget.templateCss !== editorValue) {
            this.widget.templateCss = editorValue;
            this.isDirty = true;
          }
        });
      })
    ));

    editorsObservables.push(this.createAceEditor(this.settingsJsonInputElmRef, 'json').pipe(
      tap((editor) => {
        this.jsonSettingsEditor = editor;
        this.jsonSettingsEditor.on('input', () => {
          const editorValue = this.jsonSettingsEditor.getValue();
          if (this.widget.settingsSchema !== editorValue) {
            this.widget.settingsSchema = editorValue;
            this.isDirty = true;
          }
        });
      })
    ));

    editorsObservables.push(this.createAceEditor(this.dataKeySettingsJsonInputElmRef, 'json').pipe(
      tap((editor) => {
        this.dataKeyJsonSettingsEditor = editor;
        this.dataKeyJsonSettingsEditor.on('input', () => {
          const editorValue = this.dataKeyJsonSettingsEditor.getValue();
          if (this.widget.dataKeySettingsSchema !== editorValue) {
            this.widget.dataKeySettingsSchema = editorValue;
            this.isDirty = true;
          }
        });
      })
    ));

    editorsObservables.push(this.createAceEditor(this.javascriptInputElmRef, 'javascript').pipe(
      tap((editor) => {
        this.jsEditor = editor;
        this.jsEditor.on('input', () => {
          const editorValue = this.jsEditor.getValue();
          if (this.widget.controllerScript !== editorValue) {
            this.widget.controllerScript = editorValue;
            this.isDirty = true;
          }
        });
        this.jsEditor.on('change', () => {
          this.cleanupJsErrors();
        });
        this.jsEditor.completers = [widgetEditorCompleter, ...(this.jsEditor.completers || [])];
      })
    ));

    forkJoin(editorsObservables).subscribe(
      () => {
        this.setAceEditorValues();
      }
    );

  }

  private setAceEditorValues() {
    this.htmlEditor.setValue(this.widget.templateHtml ? this.widget.templateHtml : '', -1);
    this.cssEditor.setValue(this.widget.templateCss ? this.widget.templateCss : '', -1);
    this.jsonSettingsEditor.setValue(this.widget.settingsSchema ? this.widget.settingsSchema : '', -1);
    this.dataKeyJsonSettingsEditor.setValue(this.widget.dataKeySettingsSchema ? this.widget.dataKeySettingsSchema : '', -1);
    this.jsEditor.setValue(this.widget.controllerScript ? this.widget.controllerScript : '', -1);
  }

  private createAceEditor(editorElementRef: ElementRef, mode: string): Observable<Ace.Editor> {
    const editorElement = editorElementRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/${mode}`,
      showGutter: true,
      showPrintMargin: true
    };
    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };
    editorOptions = {...editorOptions, ...advancedOptions};
    return getAce().pipe(
      map((ace) => {
        const aceEditor = ace.edit(editorElement, editorOptions);
        aceEditor.session.setUseWrapMode(true);
        this.aceEditors.push(aceEditor);
        this.aceResize$.observe(editorElement);
        return aceEditor;
      })
    );
  }

  private onAceEditorResize(aceEditor: Ace.Editor) {
    if (this.editorsResizeCafs[aceEditor.id]) {
      this.editorsResizeCafs[aceEditor.id]();
      delete this.editorsResizeCafs[aceEditor.id];
    }
    this.editorsResizeCafs[aceEditor.id] = this.raf.raf(() => {
      aceEditor.resize();
      aceEditor.renderer.updateFull();
    });
  }

  private onWindowMessage(event: MessageEvent) {
    let message: WindowMessage;
    if (event.data) {
      try {
        message = JSON.parse(event.data);
      } catch (e) {}
    }
    if (message) {
      switch (message.type) {
        case 'widgetException':
          this.onWidgetException(message.data);
          break;
        case 'widgetEditModeInited':
          this.onWidgetEditModeInited();
          break;
        case 'widgetEditUpdated':
          this.onWidgetEditUpdated(message.data);
          break;
      }
    }
  }

  private onWidgetEditModeInited() {
    this.iframeWidgetEditModeInited = true;
    if (this.saveWidgetPending || this.saveWidgetAsPending) {
      if (!this.saveWidgetTimeout) {
        this.saveWidgetTimeout = setTimeout(() => {
          if (!this.gotError) {
            if (this.saveWidgetPending) {
              this.commitSaveWidget();
            } else if (this.saveWidgetAsPending) {
              this.commitSaveWidgetAs();
            }
          } else {
            this.store.dispatch(new ActionNotificationShow(
              {message: this.translate.instant('widget.unable-to-save-widget-error'), type: 'error'}));
            this.saveWidgetPending = false;
            this.saveWidgetAsPending = false;
          }
          this.saveWidgetTimeout = undefined;
        }, 1500);
      }
    }
  }

  private onWidgetEditUpdated(widget: Widget) {
    this.widget.sizeX = widget.sizeX / 2;
    this.widget.sizeY = widget.sizeY / 2;
    this.widget.defaultConfig = JSON.stringify(widget.config);
    this.iframe.attr('data-widget', JSON.stringify(this.widget));
    this.isDirty = true;
  }

  private onWidgetException(details: ExceptionData) {
    if (!this.gotError) {
      this.gotError = true;
      let errorInfo = 'Error:';
      if (details.name) {
        errorInfo += ' ' + details.name + ':';
      }
      if (details.message) {
        errorInfo += ' ' + details.message;
      }
      if (details.lineNumber) {
        errorInfo += '<br>Line ' + details.lineNumber;
        if (details.columnNumber) {
          errorInfo += ' column ' + details.columnNumber;
        }
        errorInfo += ' of script.';
      }
      if (!this.saveWidgetPending && !this.saveWidgetAsPending) {
        this.store.dispatch(new ActionNotificationShow(
          {message: errorInfo, type: 'error', target: 'javascriptPanel'}));
      }
      if (details.lineNumber) {
        const line = details.lineNumber - 1;
        let column = 0;
        if (details.columnNumber) {
          column = details.columnNumber;
        }
        const errorMarkerId = this.jsEditor.session.addMarker(new Range(line, 0, line, Infinity),
          'ace_active-line', 'screenLine');
        this.errorMarkers.push(errorMarkerId);
        const annotations = this.jsEditor.session.getAnnotations();
        const errorAnnotation: Ace.Annotation = {
          row: line,
          column,
          text: details.message,
          type: 'error'
        };
        this.errorAnnotationId = annotations.push(errorAnnotation) - 1;
        this.jsEditor.session.setAnnotations(annotations);
      }
    }
  }

  private cleanupJsErrors() {
    this.store.dispatch(new ActionNotificationHide({}));
    this.errorMarkers.forEach((errorMarker) => {
      this.jsEditor.session.removeMarker(errorMarker);
    });
    this.errorMarkers.length = 0;
    if (this.errorAnnotationId > -1) {
      const annotations = this.jsEditor.session.getAnnotations();
      annotations.splice(this.errorAnnotationId, 1);
      this.jsEditor.session.setAnnotations(annotations);
      this.errorAnnotationId = -1;
    }
  }

  private commitSaveWidget() {
    const id = (this.widgetType && this.widgetType.id) ? this.widgetType.id : undefined;
    const createdTime = (this.widgetType && this.widgetType.createdTime) ? this.widgetType.createdTime : undefined;
    this.widgetService.saveWidgetType(this.widget, id, this.widgetsBundle.alias, createdTime).subscribe(
      (widgetTypeInstance) => {
        this.setWidgetType(widgetTypeInstance);
        this.saveWidgetPending = false;
        this.store.dispatch(new ActionNotificationShow(
          {message: this.translate.instant('widget.widget-saved'), type: 'success', duration: 500}));
      },
      () => {
        this.saveWidgetPending = false;
      }
    );
  }

  private commitSaveWidgetAs() {
    this.dialog.open<SaveWidgetTypeAsDialogComponent, any,
      SaveWidgetTypeAsDialogResult>(SaveWidgetTypeAsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (saveWidgetAsData) => {
        if (saveWidgetAsData) {
          this.widget.widgetName = saveWidgetAsData.widgetName;
          this.widget.alias = undefined;
          const config = JSON.parse(this.widget.defaultConfig);
          config.title = this.widget.widgetName;
          this.widget.defaultConfig = JSON.stringify(config);
          this.isDirty = false;
          this.widgetService.saveWidgetType(this.widget, undefined, saveWidgetAsData.bundleAlias, undefined).subscribe(
            (widgetTypeInstance) => {
              this.router.navigateByUrl(`/widgets-bundles/${saveWidgetAsData.bundleId}/widgetTypes/${widgetTypeInstance.id.id}`);
            }
          );
        }
        this.saveWidgetAsPending = false;
      }
    );
  }

  private setWidgetType(widgetTypeInstance: WidgetType) {
    this.widgetType = widgetTypeInstance;
    this.widget = toWidgetInfo(this.widgetType);
    const config = JSON.parse(this.widget.defaultConfig);
    this.widget.defaultConfig = JSON.stringify(config);
    this.origWidget = deepClone(this.widget);
    this.isDirty = false;
  }

  applyWidgetScript(): void {
    this.cleanupJsErrors();
    this.gotError = false;
    this.iframeWidgetEditModeInited = false;
    const config: WidgetConfig = JSON.parse(this.widget.defaultConfig);
    config.title = this.widget.widgetName;
    this.widget.defaultConfig = JSON.stringify(config);
    this.iframe.attr('data-widget', JSON.stringify(this.widget));
    this.iframe[0].contentWindow.location.reload(true);
  }

  undoWidget(): void {
    this.widget = deepClone(this.origWidget);
    this.setAceEditorValues();
    this.isDirty = false;
    this.applyWidgetScript();
  }

  saveWidget(): void {
    if (!this.widget.widgetName) {
      this.store.dispatch(new ActionNotificationShow(
        {message: this.translate.instant('widget.missing-widget-title-error'), type: 'error'}));
    } else {
      this.saveWidgetPending = true;
      this.applyWidgetScript();
    }
  }

  saveWidgetAs(): void {
    this.saveWidgetAsPending = true;
    this.applyWidgetScript();
  }

  undoDisabled(): boolean {
    return !this.isDirty
    || !this.iframeWidgetEditModeInited
    || this.saveWidgetPending
    || this.saveWidgetAsPending;
  }

  saveDisabled(): boolean {
    return this.isReadOnly
      || !this.isDirty
      || !this.iframeWidgetEditModeInited
      || this.saveWidgetPending
      || this.saveWidgetAsPending;
  }

  saveAsDisabled(): boolean {
    return !this.iframeWidgetEditModeInited
      || this.saveWidgetPending
      || this.saveWidgetAsPending;
  }

  beautifyCss(): void {
    beautifyCss(this.widget.templateCss, {indent_size: 4}).subscribe(
      (res) => {
        if (this.widget.templateCss !== res) {
          this.isDirty = true;
          this.widget.templateCss = res;
          this.cssEditor.setValue(this.widget.templateCss ? this.widget.templateCss : '', -1);
        }
      }
    );
  }

  beautifyHtml(): void {
    beautifyHtml(this.widget.templateHtml, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        if (this.widget.templateHtml !== res) {
          this.isDirty = true;
          this.widget.templateHtml = res;
          this.htmlEditor.setValue(this.widget.templateHtml ? this.widget.templateHtml : '', -1);
        }
      }
    );
  }

  beautifyJson(): void {
    beautifyJs(this.widget.settingsSchema, {indent_size: 4}).subscribe(
      (res) => {
        if (this.widget.settingsSchema !== res) {
          this.isDirty = true;
          this.widget.settingsSchema = res;
          this.jsonSettingsEditor.setValue(this.widget.settingsSchema ? this.widget.settingsSchema : '', -1);
        }
      }
    );
  }

  beautifyDataKeyJson(): void {
    beautifyJs(this.widget.dataKeySettingsSchema, {indent_size: 4}).subscribe(
      (res) => {
        if (this.widget.dataKeySettingsSchema !== res) {
          this.isDirty = true;
          this.widget.dataKeySettingsSchema = res;
          this.dataKeyJsonSettingsEditor.setValue(this.widget.dataKeySettingsSchema ? this.widget.dataKeySettingsSchema : '', -1);
        }
      }
    );
  }

  beautifyJs(): void {
    beautifyJs(this.widget.controllerScript, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        if (this.widget.controllerScript !== res) {
          this.isDirty = true;
          this.widget.controllerScript = res;
          this.jsEditor.setValue(this.widget.controllerScript ? this.widget.controllerScript : '', -1);
        }
      }
    );
  }

  removeResource(index: number) {
    if (index > -1) {
      if (this.widget.resources.splice(index, 1).length > 0) {
        this.isDirty = true;
      }
    }
  }

  addResource() {
    this.widget.resources.push({url: ''});
    this.isDirty = true;
  }

  widetTypeChanged() {
    const config: WidgetConfig = JSON.parse(this.widget.defaultConfig);
    if (this.widget.type !== widgetType.rpc &&
        this.widget.type !== widgetType.alarm) {
      if (config.targetDeviceAliases) {
        delete config.targetDeviceAliases;
      }
      if (config.alarmSource) {
        delete config.alarmSource;
      }
      if (!config.datasources) {
        config.datasources = [];
      }
      if (!config.timewindow) {
        config.timewindow = {
          realtime: {
            timewindowMs: 60000
          }
        };
      }
    } else if (this.widget.type === widgetType.rpc) {
      if (config.datasources) {
        delete config.datasources;
      }
      if (config.alarmSource) {
        delete config.alarmSource;
      }
      if (config.timewindow) {
        delete config.timewindow;
      }
      if (!config.targetDeviceAliases) {
        config.targetDeviceAliases = [];
      }
    } else { // alarm
      if (config.datasources) {
        delete config.datasources;
      }
      if (config.targetDeviceAliases) {
        delete config.targetDeviceAliases;
      }
      if (!config.alarmSource) {
        config.alarmSource = {};
      }
      if (!config.timewindow) {
        config.timewindow = {
          realtime: {
            timewindowMs: 24 * 60 * 60 * 1000
          }
        };
      }
    }
    this.widget.defaultConfig = JSON.stringify(config);
    this.isDirty = true;
  }
}
