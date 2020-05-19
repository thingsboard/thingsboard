"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.addCommentBefore = addCommentBefore;
exports.default = addComment;

function addCommentBefore(str, indent, comment) {
  if (!comment) return str;
  var cc = comment.replace(/[\s\S]^/gm, "$&".concat(indent, "#"));
  return "#".concat(cc, "\n").concat(indent).concat(str);
}

function addComment(str, indent, comment) {
  return !comment ? str : comment.indexOf('\n') === -1 ? "".concat(str, " #").concat(comment) : "".concat(str, "\n") + comment.replace(/^/gm, "".concat(indent || '', "#"));
}