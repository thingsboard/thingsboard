// @flow

import defineProperty from './define-property';

// Inspired by https://github.com/facebook/fbjs/blob/master/packages/fbjs/src/core/ExecutionEnvironment.js
export const canUseDOM = !!(
  typeof window !== 'undefined' &&
  window.document &&
  window.document.createElement
);

export const addEventListener = canUseDOM && 'addEventListener' in window;
export const removeEventListener = canUseDOM && 'removeEventListener' in window;

// IE8+ Support
export const attachEvent = canUseDOM && 'attachEvent' in window;
export const detachEvent = canUseDOM && 'detachEvent' in window;

// Passive options
// Inspired by https://github.com/Modernizr/Modernizr/blob/master/feature-detects/dom/passiveeventlisteners.js
export const passiveOption = (() => {
  let cache = null;

  return (() => {
    if (cache !== null) {
      return cache;
    }

    let supportsPassiveOption = false;

    try {
      window.addEventListener('test', null, defineProperty({}, 'passive', {
        get() {
          supportsPassiveOption = true;
        },
      }));
    } catch (e) {} // eslint-disable-line no-empty

    cache = supportsPassiveOption;

    return supportsPassiveOption;
  })();
})();
