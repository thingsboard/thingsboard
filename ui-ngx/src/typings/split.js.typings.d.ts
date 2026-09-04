// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
interface SplitOptions {
  sizes?: number[];
  minSize?: number[] | number;
  gutterSize?: number;
  snapOffset?: number;
  direction?: 'horizontal' | 'vertical';
  cursor?: 'col-resize' | 'row-resize';
  gutter?: (index: number, direction: string) => HTMLElement;
  elementStyle?: (dimension: string, elementSize: number, gutterSize: number) => any;
  gutterStyle?: (dimension: string, gutterSize: number) => any;
  onDrag?: ()  => void;
  onDragStart?: () => void;
  onDragEnd?: () => void;
}

interface SplitObject {
  setSizes: (sizes: number[]) => void;
  getSizes: () => number[];
  collapse: (index: number) => void;
  destroy: () => void;
}

declare function Split(elements: HTMLElement | string[], options?: SplitOptions): SplitObject;
