"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = isNativeCssFunction;
var nativeCssFunctions = new Set(["annotation", "attr", "blur", "brightness", "calc", "character-variant", "circle", "contrast", "cross-fade", "cubic-bezier", "drop-shadow", "element", "ellipse", "fit-content", "format", "frames", "grayscale", "hsl", "hsla", "hue-rotate", "image", "image-set", "inset", "invert", "leader", "linear-gradient", "local", "matrix", "matrix3d", "minmax", "opacity", "ornaments", "perspective", "polygon", "radial-gradient", "rect", "repeat", "repeating-linear-gradient", "repeating-radial-gradient", "rgb", "rgba", "rotate", "rotate3d", "rotateX", "rotatex", "rotateY", "rotatey", "rotateZ", "rotatez", "saturate", "scale", "scale3d", "scaleX", "scalex", "scaleY", "scaley", "scaleZ", "scalez", "sepia", "skew", "skewX", "skewY", "steps", "styleset", "stylistic", "swash", "symbols", "target-counter", "target-counters", "target-text", "translate", "translate3d", "translateX", "translatex", "translateY", "translatey", "translateZ", "translatez", "url", "var"]);
/**
 * Check if a function name is a native CSS function name.
 *
 * @param {string} functionName The name to check.
 * @returns {boolean} Whether or not the given function name is a native CSS function name.
 */

function isNativeCssFunction(functionName) {
  return nativeCssFunctions.has(functionName);
}