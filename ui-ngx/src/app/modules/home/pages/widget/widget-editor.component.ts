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

import { PageComponent } from '@shared/components/page.component';
import {
  Component,
  ElementRef,
  EventEmitter,
  Inject,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { detailsToWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';
import {
  migrateWidgetTypeToDynamicForms,
  TargetDeviceType,
  Widget,
  WidgetConfig,
  widgetType,
  WidgetTypeDetails,
  widgetTypesData
} from '@shared/models/widget.models';
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
import { forkJoin, mergeMap, of, Subscription, throwError } from 'rxjs';
import { widgetEditorCompleter } from '@home/pages/widget/widget-editor.models';
import { Observable } from 'rxjs/internal/Observable';
import { catchError, map, tap } from 'rxjs/operators';
import { beautifyCss, beautifyHtml, beautifyJs } from '@shared/models/beautify.models';
import { HttpClient, HttpStatusCode } from '@angular/common/http';
import { loadModulesCompleter } from '@shared/models/js-function.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { JsFuncModulesComponent } from '@shared/components/js-func-modules.component';
import { MatIconButton } from '@angular/material/button';
import { formPropertyCompletions } from '@shared/models/dynamic-form.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import Timeout = NodeJS.Timeout;

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

  widgetTypeDetails: WidgetTypeDetails;
  widget: WidgetInfo;
  origWidget: WidgetInfo;

  private isEditModeWidget = false;
  private _isDirty = false;

  get isDirty(): boolean {
    return this._isDirty || this.isEditModeWidget;
  }

  set isDirty(value: boolean) {
    if (!value) {
      this.isEditModeWidget = false;
    }
    this._isDirty = value;
  }

  fullscreen = false;
  htmlFullscreen = false;
  cssFullscreen = false;
  javascriptFullscreen = false;
  iFrameFullscreen = false;

  aceEditors: Ace.Editor[] = [];
  editorsResizeCafs: {[editorId: string]: CancelAnimationFrame} = {};
  htmlEditor: Ace.Editor;
  cssEditor: Ace.Editor;
  jsEditor: Ace.Editor;
  private initialCompleters: Ace.Completer[];
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

  updateBreadcrumbs = new EventEmitter();

  private rxSubscriptions = new Array<Subscription>();

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private route: ActivatedRoute,
              private router: Router,
              private widgetService: WidgetService,
              private translate: TranslateService,
              private raf: RafService,
              private dialog: MatDialog,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private customTranslate: CustomTranslatePipe,
              private http: HttpClient) {
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
    this.widgetTypeDetails = data.widgetEditorData.widgetTypeDetails;
    this.widget = data.widgetEditorData.widget;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = this.widgetTypeDetails && this.widgetTypeDetails.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
    if (this.widgetTypeDetails) {
      const config = JSON.parse(this.widget.defaultConfig);
      this.widget.defaultConfig = JSON.stringify(config);
    }
    this.origWidget = deepClone(this.widget);
    if (!this.widgetTypeDetails) {
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
    this.aceEditors.forEach(editor => editor.destroy());
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

    editorsObservables.push(this.createAceEditor(this.javascriptInputElmRef, 'javascript').pipe(
      tap((editor) => {
        this.jsEditor = editor;
        this.jsEditor.on('input', () => {
          const editorValue = this.jsEditor.getValue();
          if (this.controllerScriptBody !== editorValue) {
            this.controllerScriptBody = editorValue;
            this.isDirty = true;
          }
        });
        this.jsEditor.on('change', () => {
          this.cleanupJsErrors();
        });
        if (!(this.jsEditor as any).completer) {
          this.jsEditor.execCommand("startAutocomplete");
          (this.jsEditor as any).completer.detach();
        }
        (this.jsEditor as any).completer.popup.container.style.width = '500px';
        this.initialCompleters = this.jsEditor.completers || [];
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
    this.jsEditor.setValue(this.controllerScriptBody ? this.controllerScriptBody : '', -1);
    this.updateControllerScriptCompleters();
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
          this.onWidgetEditModeToggled(false);
          break;
        case 'widgetEditModeToggle':
          this.onWidgetEditModeToggled(message.data);
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

  private onWidgetEditModeToggled(mode: boolean) {
    this.isEditModeWidget = mode;
  }

  private onWidgetException(details: ExceptionData) {
    if (!this.gotError) {
      this.gotError = true;
      let errorInfo = 'Error:';
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
    const id = (this.widgetTypeDetails && this.widgetTypeDetails.id) ? this.widgetTypeDetails.id : undefined;
    const version = this.widgetTypeDetails?.version ?? null;
    const createdTime = (this.widgetTypeDetails && this.widgetTypeDetails.createdTime) ? this.widgetTypeDetails.createdTime : undefined;
    this.saveWidgetPending = false;
    this.widgetService.saveWidgetTypeDetails(this.widget, id, createdTime, version).pipe(
      mergeMap((widgetTypeDetails) => {
        this.saveWidgetPending = true;
        const widgetsBundleId = this.route.snapshot.params.widgetsBundleId as string;
        if (widgetsBundleId && !id) {
          return this.widgetService.addWidgetFqnToWidgetBundle(widgetsBundleId, widgetTypeDetails.fqn).pipe(
            map(() => widgetTypeDetails)
          );
        }
        return of(widgetTypeDetails);
      }),
      catchError((err) => {
        if (id && err.status === HttpStatusCode.Conflict) {
          return this.widgetService.getWidgetTypeById(id.id).pipe(
            map((details) => {
              return migrateWidgetTypeToDynamicForms(details);
            })
          );
        }
        return throwError(() => err);
      }),
    ).subscribe({
      next: (widgetTypeDetails) => {
        this.saveWidgetPending = false;
        if (!this.widgetTypeDetails?.id) {
          this.isDirty = false;
          this.router.navigate(['..', widgetTypeDetails.id.id], {relativeTo: this.route});
        } else {
          this.setWidgetTypeDetails(widgetTypeDetails);
        }
        this.store.dispatch(new ActionNotificationShow(
          {message: this.translate.instant('widget.widget-saved'), type: 'success', duration: 500}));
      },
      error: () => {
        this.saveWidgetPending = false;
      }
    });
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
          this.widget.fullFqn = undefined;
          const config = JSON.parse(this.widget.defaultConfig);
          config.title = this.widget.widgetName;
          this.widget.defaultConfig = JSON.stringify(config);
          this.isDirty = false;
          this.widgetService.saveWidgetTypeDetails(this.widget, undefined, undefined, undefined).pipe(
            mergeMap((widget) => {
              if (saveWidgetAsData.widgetBundleId) {
                return this.widgetService.addWidgetFqnToWidgetBundle(saveWidgetAsData.widgetBundleId, widget.fqn).pipe(
                  map(() => widget)
                );
              }
              return of(widget);
            })
          ).subscribe(
            {
              next: (widgetTypeDetails) => {
                this.saveWidgetAsPending = false;
                if (saveWidgetAsData.widgetBundleId) {
                  this.router.navigate(['resources', 'widgets-library', 'widgets-bundles',
                    saveWidgetAsData.widgetBundleId, widgetTypeDetails.id.id]);
                } else {
                  this.router.navigate(['resources', 'widgets-library', 'widget-types', widgetTypeDetails.id.id]);
                }
              },
              error: () => {
                this.saveWidgetAsPending = false;
              }
            });
        } else {
          this.saveWidgetAsPending = false;
        }
      }
    );
  }

  private setWidgetTypeDetails(widgetTypeDetails: WidgetTypeDetails) {
    this.widgetTypeDetails = widgetTypeDetails;
    this.widget = detailsToWidgetInfo(this.widgetTypeDetails);
    const config = JSON.parse(this.widget.defaultConfig);
    this.widget.defaultConfig = JSON.stringify(config);
    this.origWidget = deepClone(this.widget);
    this.isDirty = false;
    this.updateBreadcrumbs.emit();
  }

  applyWidgetScript(): void {
    this.cleanupJsErrors();
    this.gotError = false;
    this.iframeWidgetEditModeInited = false;
    const config: WidgetConfig = JSON.parse(this.widget.defaultConfig);
    if (!config.title) {
      config.title = this.widget.widgetName;
    }
    this.widget.defaultConfig = JSON.stringify(config);
    this.iframe.attr('data-widget', JSON.stringify(this.widget));
    // @ts-ignore
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
    return !this._isDirty
    || !this.iframeWidgetEditModeInited
    || this.saveWidgetPending
    || this.saveWidgetAsPending;
  }

  saveDisabled(): boolean {
    return this.isReadOnly
      || !this._isDirty
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

  beautifyJs(): void {
    beautifyJs(this.controllerScriptBody, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        if (this.controllerScriptBody !== res) {
          this.isDirty = true;
          this.controllerScriptBody = res;
          this.jsEditor.setValue(this.controllerScriptBody ? this.controllerScriptBody : '', -1);
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

  widgetTypeChanged() {
    const config: WidgetConfig = JSON.parse(this.widget.defaultConfig);
    if (this.widget.type !== widgetType.rpc &&
        this.widget.type !== widgetType.alarm) {
      if (config.targetDevice) {
        delete config.targetDevice;
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
      if (!config.targetDevice) {
        config.targetDevice = {
          type: TargetDeviceType.device
        };
      }
    } else { // alarm
      if (config.datasources) {
        delete config.datasources;
      }
      if (config.targetDevice) {
        delete config.targetDevice;
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

  settingsFormUpdated() {
    this.isDirty = true;
    this.updateControllerScriptCompleters();
  }

  dataKeySettingsFormUpdated() {
    this.isDirty = true;
  }

  latestDataKeySettingsFormUpdated() {
    this.isDirty = true;
  }

  editControllerScriptModules($event: Event, button: MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = button._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        modules: deepClone(this.controllerScriptModules)
      };
      const modulesPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: JsFuncModulesComponent,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: ctx,
        isModal: true
      });
      modulesPanelPopover.tbComponentRef.instance.popover = modulesPanelPopover;
      modulesPanelPopover.tbComponentRef.instance.modulesApplied.subscribe((modules) => {
        modulesPanelPopover.hide();
        this.controllerScriptModules = modules;
        this.updateControllerScriptCompleters();
        this.isDirty = true;
      });
    }
  }

  get controllerScriptBody(): string {
    if (typeof this.widget.controllerScript === 'string') {
      return this.widget.controllerScript;
    } else {
      return this.widget.controllerScript.body;
    }
  }

  set controllerScriptBody(controllerScriptBody) {
    if (typeof this.widget.controllerScript === 'string') {
      this.widget.controllerScript = controllerScriptBody;
    } else {
      this.widget.controllerScript.body = controllerScriptBody;
    }
  }

  get controllerScriptModules(): {[alias: string]: string} {
    if (typeof this.widget.controllerScript === 'string') {
      return null;
    } else {
      return this.widget.controllerScript.modules;
    }
  }

  set controllerScriptModules(modules: {[alias: string]: string}) {
    if (modules && Object.keys(modules).length) {
      if (typeof this.widget.controllerScript === 'string') {
        this.widget.controllerScript = {
          body: this.widget.controllerScript,
          modules
        };
      } else {
        this.widget.controllerScript.modules = modules;
      }
    } else {
      if (typeof this.widget.controllerScript !== 'string') {
        this.widget.controllerScript = this.widget.controllerScript.body;
      }
    }
  }

  get confirmOnExitMessage(): string {
    if (this.isEditModeWidget && !this._isDirty) {
      return this.translate.instant('widget.confirm-to-exit-editor-html');
    }
    return '';
  }

  private updateControllerScriptCompleters() {
    const modulesCompleterObservable = loadModulesCompleter(this.http, this.controllerScriptModules);
    modulesCompleterObservable.subscribe((modulesCompleter) => {
      const completers: Ace.Completer[] = [];
      const formPropertiesCompletions = formPropertyCompletions(this.widget.settingsForm || [], this.customTranslate);
      completers.push(widgetEditorCompleter(formPropertiesCompletions));
      if (modulesCompleter) {
        completers.push(modulesCompleter);
      }
      completers.push(...this.initialCompleters);
      this.jsEditor.completers = completers;
    });
  }
}
