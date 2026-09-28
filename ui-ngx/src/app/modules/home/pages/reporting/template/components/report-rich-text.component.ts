// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Editor, EditorEvent, EditorOptions } from 'hugerte';
import { GetImageSrcCallback, HtmlWithImagePipe, SetImageSrcCallback } from '@shared/pipe/html-with-image.pipe';
import { TranslateService } from '@ngx-translate/core';
import {
  ReportImageData,
  ReportImageDialogComponent
} from '@home/pages/reporting/template/components/report-image-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import {
  extractKeyFromVariable,
  imagePlaceholder,
  isKeyVariable,
  keyImage,
  ReportVariable
} from '@home/pages/reporting/template/components/report-component.models';
import { CustomImageUrlCallback } from '@shared/pipe/image.pipe';
import { forkJoin, of } from 'rxjs';
import { EditorComponent } from '@hugerte/hugerte-angular';
import { MatIconRegistry } from '@angular/material/icon';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { defaultHugeRteOptions } from '@shared/models/hugerte/hugerte.models';

const TB_SRC_ATTRIBUTE = 'data-tb-src';
const MCE_SRC_ATTRIBUTE = 'data-mce-src';

const findReportComponentCssLink = (): string => {
  const stylesLink = Array.from(document.getElementsByTagName('link'))
  .find((link: HTMLLinkElement) => link.rel === 'stylesheet' && link.getAttribute('href').startsWith('report-component'));
  return stylesLink ? '/' + stylesLink.getAttribute('href') : '';
}

@Component({
    selector: 'tb-report-rich-text',
    templateUrl: './report-rich-text.component.html',
    styleUrls: ['./report-component-config.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportRichTextComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportRichTextComponent implements OnInit, ControlValueAccessor, OnChanges, OnDestroy {

  editorComponent = viewChild('editor', {
    read: EditorComponent,
  });

  @Input()
  disabled: boolean;

  @Input()
  variables: ReportVariable[] = [];

  @Input()
  background: string;

  hugeRteOptions: Partial<EditorOptions> = defaultHugeRteOptions({
    body_class: 'tb-report-component',
    content_css: [findReportComponentCssLink()],
    formats: {
      'tb-medium': {
        inline: 'span',
        styles: { fontWeight: '500' }
      }
    },
    plugins: ['table', 'lists', 'code', 'fullscreen'],
    menubar: 'edit customInsert tools view format table',
    menu: {
      customInsert: {
        title: 'Insert',
        items: 'tb-image inserttable variables | hr'
      }
    },
    font_family_formats: 'Roboto=Roboto; Monospaced=monospace; Sans Serif=sans-serif; Serif=serif;',
    font_size_formats: '8pt 9pt 10pt 11pt 12pt 14pt 16pt 18pt 20pt 21pt 22pt 24pt 36pt 48pt',
    toolbar: 'undo redo | fontfamily fontsizeinput blocks ' +
      '| bold tb-medium italic strikethrough | forecolor backcolor ' +
      '| table tb-image | alignleft aligncenter alignright alignjustify ' +
      '| numlist bullist | outdent indent | removeformat | code | fullscreen',
    toolbar_mode: 'wrap',
    height: '100%',
    automatic_uploads: false,
    images_replace_blob_uris: false,
    contextmenu: ['tb-image', 'variables'],
    setup: (editor) => this.setupEditor(editor)
  });

  richTextFormControl: UntypedFormControl;

  private modelValue: string;

  private propagateChange = null;

  private domParser = new DOMParser();

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private dialog: MatDialog,
              private iconRegistry: MatIconRegistry,
              private htmlWithImagePipe: HtmlWithImagePipe,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.richTextFormControl = this.fb.control(null);
    this.richTextFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'background') {
          const editorComponent = this.editorComponent();
          if (editorComponent) {
            this.updateEditorBackground(editorComponent.editor);
          }
        }
      }
    }
  }

  ngOnDestroy() {
    this.editorComponent().editor.destroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.richTextFormControl.disable({emitEvent: false});
    } else {
      this.richTextFormControl.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.richTextFormControl.setValue(value, {emitEvent: false});
  }

  private updateModel() {
    this.modelValue = this.richTextFormControl.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private setupEditor(editor: Editor) {
    editor.on('PreInit', () => {
      this.updateEditorBackground(editor);
    });
    this.setupTbImagePlugin(editor);
    this.setupMediumFontWeight(editor);
    this.setupVariables(editor);
  }

  private updateEditorBackground(editor: Editor) {
    if (this.background) {
      editor.getBody().style.background = this.background;
    } else {
      editor.getBody().style.background = '';
    }
  }

  private setupTbImagePlugin(editor: Editor) {
    editor.ui.registry.addToggleButton('tb-image', {
      icon: 'image',
      tooltip: this.translate.instant('report-template.insert-update-image'),
      onSetup: (api)  => {
        const imgElm = editor.selection.getNode() as HTMLElement;
        api.setActive(imgElm && imgElm.nodeName === 'IMG');
        const editorEventCallback = (eventApi: EditorEvent<any>) => {
          api.setActive(eventApi.element.nodeName === 'IMG');
        };
        editor.on('NodeChange', editorEventCallback);
        return () => editor.off('NodeChange', editorEventCallback);
      },
      onAction: () => {
        this.editorImageAction(editor);
      }
    });

    editor.ui.registry.addButton('tb-edit-image', {
      icon: 'image',
      tooltip: this.translate.instant('report-template.edit-image'),
      onAction: () => {
        this.editorImageAction(editor);
      }
    });

    editor.ui.registry.addContextToolbar('tb-image', {
      position: 'node',
      type: 'contexttoolbar',
      predicate: elem => elem.nodeName === 'IMG',
      items: 'tb-edit-image alignleft aligncenter alignright'
    });

    editor.ui.registry.addMenuItem('tb-image', {
      icon: 'image',
      text: this.translate.instant('report-template.image') + '...',
      onAction: () => {
        this.editorImageAction(editor);
      }
    });

    editor.ui.registry.addContextMenu('tb-image', {
      update: (_element) => {
        return [
          {
            icon: 'image',
            text: this.translate.instant('report-template.image') + '...',
            onAction: () => {
              this.editorImageAction(editor);
            }
          }
        ];
      }
    });

    editor.on('BeforeSetContent', (event) => {
      event.content = this.processImages(event.content, (image) => {
        let src: string;
        if (image.hasAttribute(MCE_SRC_ATTRIBUTE)) {
          src = image.getAttribute(MCE_SRC_ATTRIBUTE);
        } else {
          src = image.getAttribute('src');
        }
        if (!image.hasAttribute(TB_SRC_ATTRIBUTE) || src !== '#') {
          image.setAttribute(TB_SRC_ATTRIBUTE, src);
        }
        image.setAttribute('src', '#');
      });
    });

    editor.on('SetContent', (_event) => {
      this.htmlWithImagePipe.transform(editor.getBody(), {
        getImageSrcCallback: this.getImageSrcCallback.bind(this),
        setImageSrcCallback: this.setImageSrcCallback.bind(this),
        customImageUrlCallback: this.customImageUrlCallback.bind(this)
      }).subscribe();
    });

    editor.on('GetContent', (event) => {
      event.content = this.processImages(event.content, (image) => {
        const src = this.getImageSrcCallback(image);
        image.setAttribute('src', src);
        image.removeAttribute(TB_SRC_ATTRIBUTE);
      });
    });
  }

  private setupMediumFontWeight(editor: Editor) {
    editor.ui.registry.addIcon('medium-weight', '<svg width="24" height="24" focusable="false" version="1.1" xmlns="http://www.w3.org/2000/svg">\n' +
      ' <path d="m12 15.536 3.819-10.172h2.9896v13.271h-2.2969v-4.375l0.21875-5.8333-3.9102 10.208h-1.6497l-3.9102-10.217 0.22786 5.8425v4.375h-2.2969v-13.271h2.9805z"/>\n' +
      '</svg>');
    editor.addCommand('TbMedium', ui => {
      this.toggleMediumWeightFormat(editor);
    });
    editor.addShortcut('Meta+M', 'Set medium weight', 'TbMedium');
    editor.ui.registry.addToggleButton('tb-medium', {
      icon: 'medium-weight',
      tooltip: 'Medium',
      onSetup: (api)  => {
        const ref = editor.formatter.formatChanged('tb-medium', (state) => {
          api.setActive(state);
        });
        return () => ref.unbind();
      },
      onAction: () => {
        editor.execCommand('TbMedium');
      }
    });
    editor.ui.registry.addMenuItem('tb-medium', {
      icon: 'medium-weight',
      text: 'Medium',
      shortcut: 'Meta+M',
      onAction: () => {
        editor.execCommand('TbMedium');
      }
    });
  }

  private toggleMediumWeightFormat(editor: Editor) {
    editor.undoManager.transact(() => {
      if (editor.formatter.match('bold')) {
        editor.formatter.remove('bold');
      }
      editor.formatter.toggle('tb-medium');
    });
  }

  private setupVariables(editor: Editor) {
    forkJoin({
      variable: this.iconRegistry.getNamedSvgIcon('variable', 'mdi'),
      attribute: this.iconRegistry.getNamedSvgIcon('alpha-a-circle-outline', 'mdi'),
      timeseries: this.iconRegistry.getNamedSvgIcon('chart-timeline-variant', 'mdi'),
      entity: this.iconRegistry.getNamedSvgIcon('alpha-e-circle-outline', 'mdi'),
      page: this.iconRegistry.getNamedSvgIcon('variable-box', 'mdi')
    }).subscribe((icons) => {
      editor.ui.registry.addIcon('variable', icons.variable.outerHTML);
      editor.ui.registry.addIcon('tb-key-attribute', icons.attribute.outerHTML);
      editor.ui.registry.addIcon('tb-key-timeseries', icons.timeseries.outerHTML);
      editor.ui.registry.addIcon('tb-key-entity', icons.entity.outerHTML);
      editor.ui.registry.addIcon('tb-page-var', icons.page.outerHTML);
    });
    editor.ui.registry.addAutocompleter('variables', {
      trigger: '$',
      minChars: 0,
      columns: 1,
      onAction: (autocompleteApi, rng, value) => {
        editor.selection.setRng(rng);
        editor.insertContent(value);
        autocompleteApi.hide();
      },
      fetch: (pattern) => {
        return new Promise((resolve) => {
          const results= this.variables.filter(variable => variable.name.toUpperCase().includes(pattern.toUpperCase()))
          .map((val) => ({
            type: 'autocompleteitem',
            value: `\${${val.name}}`,
            text: val.name,
            icon: this.variableIcon(val)
          }) as any);
          resolve(results);
        });
      }
    });
    editor.ui.registry.addNestedMenuItem('variables', {
      icon: 'variable',
      text: 'Variable...',
      getSubmenuItems: () => {
        return this.variables.map(variable => ({
            icon: this.variableIcon(variable),
            text: variable.name,
            type: 'menuitem',
            onAction: () => {
            editor.insertContent(`\${${variable.name}}`);
          }
        }));
      }
    });

    editor.ui.registry.addContextMenu('variables', {
      update: (element) => {
        if (!element || element.nodeName !== 'IMG') {
          return [
            {
              icon: 'variable',
              text: 'Variable...',
              type: 'submenu',
              getSubmenuItems: () => {
                return this.variables.map(variable => ({
                  icon: this.variableIcon(variable),
                  text: variable.name,
                  type: 'item',
                  onAction: () => {
                    editor.insertContent(`\${${variable.name}}`);
                  }
                }));
              }
            }
          ];
        }
      }
    });
  }

  private setImageSrcCallback: SetImageSrcCallback = (image, origUrl, newUrl) => {
    if (origUrl !== newUrl) {
      image.setAttribute(TB_SRC_ATTRIBUTE, origUrl);
    }
  }

  private getImageSrcCallback: GetImageSrcCallback = (image) => {
    if (image.hasAttribute(TB_SRC_ATTRIBUTE)) {
      return image.getAttribute(TB_SRC_ATTRIBUTE);
    } else {
      return image.getAttribute('src');
    }
  }

  private customImageUrlCallback: CustomImageUrlCallback = (url) => {
    if (!url) {
      return of(imagePlaceholder);
    } else if (isKeyVariable(url)) {
      const key = extractKeyFromVariable(url);
      return of(keyImage(key));
    } else {
      return null;
    }
  }

  private processImages(content: string, imageElementCallback: (image: HTMLImageElement) => void): string {
    const document = this.domParser.parseFromString(content, "text/html");
    const images = document.images;
    for (let i= 0; i < images.length; i++) {
      const image = images.item(i);
      imageElementCallback(image);
    }
    return document.body.innerHTML;
  }

  private editorImageAction(editor: Editor) {
    const imageData = this.extractImageData(editor);
    this.dialog.open<ReportImageDialogComponent, ReportImageData,
      ReportImageData>(ReportImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {...imageData, entityKeys: this.variables.filter(variable => variable.type === 'entityKey')}
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.insertOrUpdateImage(editor, result);
      }
    });
  }

  private extractImageData(editor: Editor): ReportImageData {
    const elm = editor.selection.getNode() as HTMLElement;
    const imgElm = elm.nodeName === 'IMG' ? elm as HTMLImageElement : null;
    let imageUrl: string = null;
    let width: number = null;
    let height: number = null;
    if (imgElm) {
      imageUrl = this.getImageSrcCallback(imgElm);
      if (imgElm.hasAttribute('width')) {
        const value = imgElm.getAttribute('width').replace(/px$/, '');
        width = Number.parseInt(value);
      }
      if (imgElm.hasAttribute('height')) {
        const value = imgElm.getAttribute('height').replace(/px$/, '');
        height = Number.parseInt(value);
      }
    }
    return {
      imageUrl,
      width,
      height
    }
  }

  private insertOrUpdateImage(editor: Editor, data: ReportImageData) {
    editor.undoManager.transact(() => {
      const elm = editor.selection.getNode() as HTMLElement;
      const imgElm = elm.nodeName === 'IMG' ? elm : null;
      const imageUrl = data.imageUrl ? data.imageUrl : '';
      if (imgElm) {
       // if (data.imageUrl) {
          imgElm.setAttribute('src', '#');
          imgElm.setAttribute(TB_SRC_ATTRIBUTE, imageUrl);
          imgElm.setAttribute('width', data.width ? (data.width + 'px') : null);
          imgElm.setAttribute('height', data.height ? (data.height + 'px') : null);
          editor.dom.setAttrib(imgElm, 'data-mce-id', '__mceupd');
          editor.focus();
          editor.selection.setContent(imgElm.outerHTML);
          const updatedElm = editor.dom.select('*[data-mce-id="__mceupd"]')[0];
          editor.dom.setAttrib(updatedElm, 'data-mce-id', null);
          editor.selection.select(updatedElm);
        /* } else {
           editor.dom.remove(imgElm);
           editor.focus();
           editor.nodeChanged();
           if (editor.dom.isEmpty(editor.getBody())) {
             editor.setContent('');
             editor.selection.setCursorLocation();
           }
         }*/
      } else /* if (data.imageUrl)*/ {
        const image = document.createElement('img');
        image.setAttribute('src', '#');
        image.setAttribute(TB_SRC_ATTRIBUTE, imageUrl);
        if (data.width) {
          image.setAttribute('width', data.width + 'px');
        }
        if (data.height) {
          image.setAttribute('height', data.height + 'px');
        }
        editor.dom.setAttrib(image, 'data-mce-id', '__mcenew');
        editor.focus();
        editor.selection.setContent(image.outerHTML);
        const insertedElm = editor.dom.select('*[data-mce-id="__mcenew"]')[0];
        editor.dom.setAttrib(insertedElm, 'data-mce-id', null);
        editor.selection.select(insertedElm);
      }
    });
  }

  private variableIcon(variable: ReportVariable): string {
    if (variable.type === 'pageVariable') {
      return 'tb-page-var';
    } else if (variable.dataKey?.type) {
      switch (variable.dataKey.type) {
        case DataKeyType.timeseries:
          return 'tb-key-timeseries'
        case DataKeyType.attribute:
          return 'tb-key-attribute'
        case DataKeyType.entityField:
          return 'tb-key-entity'
      }
    }
    return 'tb-page-var';
  }

}
