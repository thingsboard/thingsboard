function hidden(node) {
  return node.style.display === 'none';
}

function visible(node) {
  while (node) {
    if (node === document.body) {
      break;
    }

    if (hidden(node)) {
      return false;
    }

    node = node.parentNode;
  }

  return true;
}

function focusable(node) {
  var nodeName = node.nodeName.toLowerCase();
  var tabIndex = parseInt(node.getAttribute('tabindex'), 10);
  var hasTabIndex = !isNaN(tabIndex) && tabIndex > -1;

  if (visible(node)) {
    if (['input', 'select', 'textarea', 'button'].indexOf(nodeName) > -1) {
      return !node.disabled;
    } else if (nodeName === 'a') {
      return node.getAttribute('href') || hasTabIndex;
    }

    return node.isContentEditable || hasTabIndex;
  }
}

export function getFocusNodeList(node) {
  var res = [].slice.call(node.querySelectorAll('*'), 0).filter(function (child) {
    return focusable(child);
  });

  if (focusable(node)) {
    res.unshift(node);
  }

  return res;
}
var lastFocusElement = null;
export function saveLastFocusNode() {
  lastFocusElement = document.activeElement;
}
export function clearLastFocusNode() {
  lastFocusElement = null;
}
export function backLastFocusNode() {
  if (lastFocusElement) {
    try {
      // 元素可能已经被移动了
      lastFocusElement.focus();
      /* eslint-disable no-empty */
    } catch (e) {} // empty

    /* eslint-enable no-empty */

  }
}
export function limitTabRange(node, e) {
  if (e.keyCode === 9) {
    var tabNodeList = getFocusNodeList(node);
    var lastTabNode = tabNodeList[e.shiftKey ? 0 : tabNodeList.length - 1];
    var leavingTab = lastTabNode === document.activeElement || node === document.activeElement;

    if (leavingTab) {
      var target = tabNodeList[e.shiftKey ? tabNodeList.length - 1 : 0];
      target.focus();
      e.preventDefault();
    }
  }
}