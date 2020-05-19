import React from 'react';
export default function toArray(children) {
  var ret = [];
  React.Children.forEach(children, function (c) {
    ret.push(c);
  });
  return ret;
}