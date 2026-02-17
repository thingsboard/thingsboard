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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EditorOptions } from 'tinymce';

@Component({
    selector: 'tb-release-notes-panel',
    templateUrl: './editor-panel.component.html',
    styleUrls: ['./editor-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditorPanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  content: string;

  @Input()
  title: string;

  @Input()
  popover: TbPopoverComponent<EditorPanelComponent>;

  @Output()
  editorContentApplied = new EventEmitter<string>();

  editorControl: FormControl<string>;

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link', 'table', 'image', 'lists', 'fullscreen'],
    menubar: 'edit insert view format',
    toolbar: ['fontfamily fontsize | bold italic underline strikethrough forecolor backcolor',
      'alignleft aligncenter alignright alignjustify | bullist | link table image | fullscreen'],
    toolbar_mode: 'sliding',
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false,
    resize: false,
    setup: (editor) => {
      editor.on('PostRender', function() {
        const container = document.querySelector('.tox.tox-tinymce-aux');
        const styleSheet = document.createElement('style');
        styleSheet.innerText = `
          .tox-tiered-menu .tox-menu {
            width: fit-content;
            max-width: min(80%, 440px);
            @media screen and (max-width: 510px) {
              max-width: calc(100% - 64px);
            }
            media screen and (min-width: 511px) and (max-width: 548px) {
              max-width: calc(100% - 84px);
            }
            media screen and (min-width: 549px) and (max-width: 599px) {
              max-width: calc(100% - 104px);
            }
          }
          .tox-tiered-menu .tox-menu .tox-collection__item-label {
            word-break: normal;
          }
          @media screen and (max-width: 890px) {
            .tox-tiered-menu > .tox-collection--list:not(:first-child) {
              left: auto !important;
              right: 0 !important;
            }
          }
        `;
        container.prepend(styleSheet);
      });
    },
    relative_urls: false,
    urlconverter_callback: (url) => url
  };

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.editorControl = this.fb.control(this.content);
    if (this.disabled) {
      this.editorControl.disable({emitEvent: false});
      this.tinyMceOptions.toolbar = false;
      this.tinyMceOptions.menubar = false;
      this.tinyMceOptions.statusbar = false;
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.editorControl.valid) {
      this.editorContentApplied.emit(this.editorControl.value);
    }
  }
}
