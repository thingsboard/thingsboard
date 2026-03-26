/**
 * Flot re-exports legacy paths via package.json "exports". TypeScript with
 * moduleResolution "node" does not resolve those; these declarations satisfy
 * the compiler for dynamic imports used at runtime by the bundler.
 */
declare module 'flot/src/jquery.flot.js';
declare module 'flot/lib/jquery.colorhelpers.js';
declare module 'flot/src/plugins/jquery.flot.time.js';
declare module 'flot/src/plugins/jquery.flot.selection.js';
declare module 'flot/src/plugins/jquery.flot.pie.js';
declare module 'flot/src/plugins/jquery.flot.crosshair.js';
declare module 'flot/src/plugins/jquery.flot.stack.js';
declare module 'flot/src/plugins/jquery.flot.symbol.js';
