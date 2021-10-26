import {
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { ResizeObserver } from '@juggle/resize-observer';
import { guid } from '@core/utils';
import { ContentType } from '@shared/models/constants';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getAce } from '@shared/models/ace/ace.models';
import { beautifyJs } from '@shared/models/beautify.models';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-protobuf-content',
  templateUrl: './protobuf-content.component.html',
  styleUrls: ['./protobuf-content.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProtobufContentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ProtobufContentComponent),
      multi: true,
    }
  ]
})
export class ProtobufContentComponent implements OnInit, ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @ViewChild('protobufEditor', {static: true})
  protobufEditorElmRef: ElementRef;

  private protobufEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  toastTargetId = `protobufContentEditor-${guid()}`;

  @Input() label: string;

  contentType: ContentType = ContentType.TEXT;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: {[klass: string]: any};

  @Input() tbPlaceholder: string;

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  contentBody: string;

  contentValid: boolean;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService) {
  }

  ngOnInit(): void {
    const editorElement = this.protobufEditorElmRef.nativeElement;
    let mode = 'protobuf';
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/${mode}`,
      showGutter: true,
      showPrintMargin: false,
      readOnly: this.disabled || this.readonly,
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true,
      autoScrollEditorIntoView: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.protobufEditor = ace.edit(editorElement, editorOptions);
        this.protobufEditor.session.setUseWrapMode(true);
        this.protobufEditor.setValue(this.contentBody ? this.contentBody : '', -1);
        this.protobufEditor.setReadOnly(this.disabled || this.readonly);
        this.protobufEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.updateView();
          }
        });
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.protobufEditor.resize();
      this.protobufEditor.renderer.updateFull();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'contentType') {
          if (this.protobufEditor) {
            let mode = 'protobuf';
            this.protobufEditor.session.setMode(`ace/mode/${mode}`);
          }
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.protobufEditor) {
      this.protobufEditor.setReadOnly(this.disabled || this.readonly);
    }
  }

  public validate(c: FormControl) {
    return (this.contentValid) ? null : {
      contentBody: {
        valid: false,
      },
    };
  }

  writeValue(value: string): void {
    this.contentBody = value;
    this.contentValid = true;
    if (this.protobufEditor) {
      this.ignoreChange = true;
      this.protobufEditor.setValue(this.contentBody ? this.contentBody : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.protobufEditor.getValue();
    if (this.contentBody !== editorValue) {
      this.contentBody = editorValue;
      this.propagateChange(this.contentBody);
    }
  }

  beautifyJSON() {
    beautifyJs(this.contentBody, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.protobufEditor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  minifyJSON() {
    const res = JSON.stringify(this.contentBody);
    this.protobufEditor.setValue(res ? res : '', -1);
    this.updateView();
  }

  onFullscreen() {
    if (this.protobufEditor) {
      setTimeout(() => {
        this.protobufEditor.resize();
      }, 0);
    }
  }

}
