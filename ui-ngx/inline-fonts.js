// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

// Inlines Segment7Standard.otf as a base64 data: URI so canvas digital gauges have the font
// synchronously on first paint, while keeping the raw .otf in src/assets/fonts/ for fonts-sbom.
const fs = require('node:fs');
const path = require('node:path');

const fontPath = path.join(__dirname, 'src', 'assets', 'fonts', 'Segment7Standard.otf');
const outPath = path.join(__dirname, 'target', 'generated-sources', 'fonts.css');
const base64 = fs.readFileSync(fontPath).toString('base64');

fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(
  outPath,
  `@font-face {\n  font-family: "Segment7Standard";\n  font-style: italic;\n  font-weight: 400;\n  src: url("data:font/opentype;base64,${base64}") format("opentype");\n}\n`,
  'utf8',
);
