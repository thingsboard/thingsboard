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

declare module 'ace-code' {
  export type EditSession = any;
  export type Editor = any;
  export type Point = { row: number; column: number };
  export type Range = any;

  export namespace Ace {
    export type EditSession = any;
    export type Editor = any;
    export type Point = { row: number; column: number };
    export type Range = any;
    export type Completion = {
      value: string;
      meta?: string;
      type?: string;
      caption?: string;
      snippet?: string;
      score?: number;
      exactMatch?: number;
      docHTML?: string;
      [key: string]: any;
    };
    export type Document = any;
    export type MarkerGroupItem = any;
  }
}

declare module 'ace-code/src/autocomplete' {
  export class CompletionProvider {
    registerCompleter(completer: any): void;
    [key: string]: any;
  }
}

declare module 'ace-code/src/ext/command_bar' {
  export class CommandBarTooltip {
    constructor(editor: any);
    [key: string]: any;
  }
}

declare module 'ace-code/src/ext/inline_autocomplete' {
  export class InlineAutocomplete {
    constructor(editor: any);
    [key: string]: any;
  }
}
