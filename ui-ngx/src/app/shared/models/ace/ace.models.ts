// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Ace } from 'ace-builds';
import type { LanguageProvider } from 'ace-linters';
import { Observable } from 'rxjs/internal/Observable';
import { forkJoin, from, of } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';
import { unwrapModule } from '@core/utils';
import { Renderer2 } from '@angular/core';

let aceDependenciesLoaded = false;
let aceModule: any;
let aceDiffModule: any;
let cssLanguageProvider: LanguageProvider;

const aceModuleLoaders: Record<string, () => Promise<unknown>> = {
  'ace/mode/java': () => import('ace-builds/src-noconflict/mode-java'),
  'ace/mode/css': () => import('ace-builds/src-noconflict/mode-css'),
  'ace/mode/json': () => import('ace-builds/src-noconflict/mode-json'),
  'ace/mode/javascript': () => import('ace-builds/src-noconflict/mode-javascript'),
  'ace/mode/text': () => import('ace-builds/src-noconflict/mode-text'),
  'ace/mode/markdown': () => import('ace-builds/src-noconflict/mode-markdown'),
  'ace/mode/html': () => import('ace-builds/src-noconflict/mode-html'),
  'ace/mode/xml': () => import('ace-builds/src-noconflict/mode-xml'),
  'ace/mode/svg': () => import('ace-builds/src-noconflict/mode-svg'),
  'ace/mode/protobuf': () => import('ace-builds/src-noconflict/mode-protobuf'),
  'ace/mode/yaml': () => import('ace-builds/src-noconflict/mode-yaml'),
  'ace/mode/tbel': () => import('./tbel/mode-tbel'),
  'ace/snippets/java': () => import('ace-builds/src-noconflict/snippets/java'),
  'ace/snippets/css': () => import('ace-builds/src-noconflict/snippets/css'),
  'ace/snippets/json': () => import('ace-builds/src-noconflict/snippets/json'),
  'ace/snippets/javascript': () => import('ace-builds/src-noconflict/snippets/javascript'),
  'ace/snippets/text': () => import('ace-builds/src-noconflict/snippets/text'),
  'ace/snippets/markdown': () => import('ace-builds/src-noconflict/snippets/markdown'),
  'ace/snippets/html': () => import('ace-builds/src-noconflict/snippets/html'),
  'ace/snippets/xml': () => import('ace-builds/src-noconflict/snippets/xml'),
  'ace/snippets/svg': () => import('ace-builds/src-noconflict/snippets/svg'),
  // Snippets of the protobuf mode, which bundles the C/C++ mode
  'ace/snippets/c_cpp': () => import('ace-builds/src-noconflict/snippets/c_cpp'),
  'ace/theme/github': () => import('ace-builds/src-noconflict/theme-github')
};

const loadedAceModules = new Map<string, Promise<unknown>>();

function loadAceModule(ace: any, moduleName: string): Promise<unknown> {
  let loaded = loadedAceModules.get(moduleName);
  if (!loaded) {
    loaded = aceModuleLoaders[moduleName]().then((module) => {
      // Ace resolves a module that has a loader asynchronously on every use; dropping the loader once the module
      // is defined lets Ace resolve it synchronously.
      delete ace.config.dynamicModules[moduleName];
      return module;
    });
    loaded.catch(() => loadedAceModules.delete(moduleName));
    loadedAceModules.set(moduleName, loaded);
  }
  return loaded;
}

function loadAceDependencies(ace: any): Observable<any> {
  if (aceDependenciesLoaded) {
    return of(null);
  } else {
    for (const moduleName of Object.keys(aceModuleLoaders)) {
      ace.config.setModuleLoader(moduleName, () => loadAceModule(ace, moduleName));
    }
    // Extensions register editor options (autocompletion, search box), so they must be loaded before an editor is created
    return forkJoin([
      import('ace-builds/src-noconflict/ext-language_tools'),
      import('ace-builds/src-noconflict/ext-searchbox')
    ]).pipe(
      tap(() => {
        aceDependenciesLoaded = true;
      })
    );
  }
}

// Modes, snippets and themes load on first use, so the first `setMode` of a mode completes asynchronously.
// Pass the modes a caller reads synchronously right after `setMode` (e.g. `session.$mode`) as `preloadModules`.
export function getAce(...preloadModules: string[]): Observable<any> {
  const ace$ = aceModule ? of(aceModule) : from(import('ace-builds/src-noconflict/ace')).pipe(
    mergeMap((module) => {
      const ace = unwrapModule(module);
      return loadAceDependencies(ace).pipe(
        map(() => ace)
      );
    }),
    tap((module) => {
      aceModule = module;
    })
  );
  if (!preloadModules.length) {
    return ace$;
  }
  return ace$.pipe(
    mergeMap((ace) => forkJoin(preloadModules.map(moduleName => loadAceModule(ace, moduleName))).pipe(
      map(() => ace)
    ))
  );
}

export function getAceDiff(): Observable<any> {
  if (aceDiffModule) {
    return of(aceDiffModule);
  } else {
    return getAce().pipe(
      mergeMap((ace) => {
        return from(import('ace-diff')).pipe(
          map((module) => unwrapModule(module))
        );
      }),
      tap((module) => {
        aceDiffModule = module;
      })
    );
  }
}

export function getCssLanguageProvider(): Observable<LanguageProvider> {
  if (cssLanguageProvider) {
    return of(cssLanguageProvider);
  } else {
    return from(import('ace-linters')).pipe(
      map((module) => {
        if (!cssLanguageProvider) {
          const aceLinters: typeof module = unwrapModule(module);
          const worker = new Worker(new URL('../../../core/worker/css-linter.worker', import.meta.url), { type: 'module' });
          cssLanguageProvider = aceLinters.LanguageProvider.create(worker, {
            functionality: {
              completion: {
                overwriteCompleters: false
              }
            }
          });
        }
        return cssLanguageProvider;
      })
    );
  }
}

export function updateEditorSize(editorElement: any, content: string, editor: Ace.Editor, renderer: Renderer2, options?: {
  showGutter?: boolean,
  ignoreHeight?: boolean,
  ignoreWidth?: boolean,
  setMinHeight?: boolean
}): void {
  let newHeight = 200;
  let newWidth = 600;
  if (content && content.length > 0) {
    if (editor.renderer.lineHeight <= 0) {
      editor.renderer.updateFull(true);
    }
    const lines = content.split('\n');
    newHeight = editor.renderer.lineHeight * lines.length + 16;
    let maxLineLength = 0;
    lines.forEach((row) => {
      const line = row.replace(/\t/g, '  ').replace(/\n/g, '');
      const lineLength = line.length;
      maxLineLength = Math.max(maxLineLength, lineLength);
    });
    if (options?.showGutter) {
      maxLineLength += lines.length.toString().length;
    }
    newWidth = 10 * maxLineLength + 16;
    if (options?.showGutter) {
      newWidth += 32;
    }
  }
  const setHeight = (height: number) => {
    if (!options.ignoreHeight) {
      renderer.setStyle(editorElement, 'height', height.toString() + 'px');
    }
    if (options.setMinHeight) {
      renderer.setStyle(editorElement, 'minHeight', height.toString() + 'px');
    }
  };
  setHeight(newHeight);
  if (!options.ignoreWidth) {
    renderer.setStyle(editorElement, 'width', newWidth.toString() + 'px');
  }
  editor.resize(true);
  // Ace's horizontal scrollbar overlaps the last row; grow the editor by its height so no vertical scrollbar appears
  const scrollBarHeight: number = editor.renderer.scrollBarH.getHeight();
  if (scrollBarHeight) {
    setHeight(newHeight + scrollBarHeight);
    editor.resize();
  }
}

export class Range implements Ace.Range {

  public start: Ace.Point;
  public end: Ace.Point;

  constructor(startRow: number, startColumn: number, endRow: number, endColumn: number) {
    this.start = {
      row: startRow,
      column: startColumn
    };

    this.end = {
      row: endRow,
      column: endColumn
    };
  }

  static fromPoints(start: Ace.Point, end: Ace.Point): Ace.Range {
    return new Range(start.row, start.column, end.row, end.column);
  }

  clipRows(firstRow: number, lastRow: number): Ace.Range {
    let end: Ace.Point;
    let start: Ace.Point;
    if (this.end.row > lastRow) {
      end = {row: lastRow + 1, column: 0};
    } else if (this.end.row < firstRow) {
      end = {row: firstRow, column: 0};
    }

    if (this.start.row > lastRow) {
      start = {row: lastRow + 1, column: 0};
    } else if (this.start.row < firstRow) {
      start = {row: firstRow, column: 0};
    }
    return Range.fromPoints(start || this.start, end || this.end);
  }

  clone(): Ace.Range {
    return Range.fromPoints(this.start, this.end);
  }

  collapseRows(): Ace.Range {
    if (this.end.column === 0) {
      return new Range(this.start.row, 0, Math.max(this.start.row, this.end.row - 1), 0);
    } else {
      return new Range(this.start.row, 0, this.end.row, 0);
    }
  }

  compare(row: number, column: number): number {
    if (!this.isMultiLine()) {
      if (row === this.start.row) {
        return column < this.start.column ? -1 : (column > this.end.column ? 1 : 0);
      }
    }

    if (row < this.start.row) {
      return -1;
    }

    if (row > this.end.row) {
      return 1;
    }

    if (this.start.row === row) {
      return column >= this.start.column ? 0 : -1;
    }

    if (this.end.row === row) {
      return column <= this.end.column ? 0 : 1;
    }

    return 0;
  }

  compareEnd(row: number, column: number): number {
    if (this.end.row === row && this.end.column === column) {
      return 1;
    } else {
      return this.compare(row, column);
    }
  }

  compareInside(row: number, column: number): number {
    if (this.end.row === row && this.end.column === column) {
      return 1;
    } else if (this.start.row === row && this.start.column === column) {
      return -1;
    } else {
      return this.compare(row, column);
    }
  }

  comparePoint(p: Ace.Point): number {
    return this.compare(p.row, p.column);
  }

  compareRange(range: Ace.Range): number {
    let cmp: number;
    const end = range.end;
    const start = range.start;

    cmp = this.compare(end.row, end.column);
    if (cmp === 1) {
      cmp = this.compare(start.row, start.column);
      if (cmp === 1) {
        return 2;
      } else if (cmp === 0) {
        return 1;
      } else {
        return 0;
      }
    } else if (cmp === -1) {
      return -2;
    } else {
      cmp = this.compare(start.row, start.column);
      if (cmp === -1) {
        return -1;
      } else if (cmp === 1) {
        return 42;
      } else {
        return 0;
      }
    }
  }

  compareStart(row: number, column: number): number {
    if (this.start.row === row && this.start.column === column) {
      return -1;
    } else {
      return this.compare(row, column);
    }
  }

  contains(row: number, column: number): boolean {
    return this.compare(row, column) === 0;
  }

  containsRange(range: Ace.Range): boolean {
    return this.comparePoint(range.start) === 0 && this.comparePoint(range.end) === 0;
  }

  extend(row: number, column: number): Ace.Range {
    const cmp = this.compare(row, column);
    let end: Ace.Point;
    let start: Ace.Point;
    if (cmp === 0) {
      return this;
    } else if (cmp === -1) {
      start = {row, column};
    } else {
      end = {row, column};
    }
    return Range.fromPoints(start || this.start, end || this.end);
  }

  inside(row: number, column: number): boolean {
    if (this.compare(row, column) === 0) {
      if (this.isEnd(row, column) || this.isStart(row, column)) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  insideEnd(row: number, column: number): boolean {
    if (this.compare(row, column) === 0) {
      if (this.isStart(row, column)) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  insideStart(row: number, column: number): boolean {
    if (this.compare(row, column) === 0) {
      if (this.isEnd(row, column)) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  intersects(range: Ace.Range): boolean {
    const cmp = this.compareRange(range);
    return (cmp === -1 || cmp === 0 || cmp === 1);
  }

  isEmpty(): boolean {
    return (this.start.row === this.end.row && this.start.column === this.end.column);
  }

  isEnd(row: number, column: number): boolean {
    return this.end.row === row && this.end.column === column;
  }

  isEqual(range: Ace.Range): boolean {
    return this.start.row === range.start.row &&
      this.end.row === range.end.row &&
      this.start.column === range.start.column &&
      this.end.column === range.end.column;
  }

  isMultiLine(): boolean {
    return (this.start.row !== this.end.row);
  }

  isStart(row: number, column: number): boolean {
    return this.start.row === row && this.start.column === column;
  }

  moveBy(row: number, column: number): void {
    this.start.row += row;
    this.start.column += column;
    this.end.row += row;
    this.end.column += column;
  }

  setEnd(row: number, column: number): void {
    if (typeof row === 'object') {
      this.end.column = (row as Ace.Point).column;
      this.end.row = (row as Ace.Point).row;
    } else {
      this.end.row = row;
      this.end.column = column;
    }
  }

  setStart(row: number, column: number): void {
    if (typeof row === 'object') {
      this.start.column = (row as Ace.Point).column;
      this.start.row = (row as Ace.Point).row;
    } else {
      this.start.row = row;
      this.start.column = column;
    }
  }

  toScreenRange(session: Ace.EditSession): Ace.Range {
    const screenPosStart = session.documentToScreenPosition(this.start);
    const screenPosEnd = session.documentToScreenPosition(this.end);

    return new Range(
      screenPosStart.row, screenPosStart.column,
      screenPosEnd.row, screenPosEnd.column
    );
  }

}

export interface AceHighlightRules {
  [group: string]: Array<AceHighlightRule>;
}

export interface AceHighlightRule {
  regex: RegExp | string;
  token: string;
  next?: string;
}

export const dotOperatorHighlightRule: AceHighlightRule = {
  token: 'punctuation.operator',
  regex: /[.](?![.])/,
};

export const endGroupHighlightRule: AceHighlightRule = {
  regex: '',
  token: 'empty',
  next: 'no_regex'
};


