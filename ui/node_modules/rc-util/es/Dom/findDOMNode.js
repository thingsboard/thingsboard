import ReactDOM from 'react-dom';
/**
 * Return if a node is a DOM node. Else will return by `findDOMNode`
 */

export default function findDOMNode(node) {
  if (node instanceof HTMLElement) {
    return node;
  }

  return ReactDOM.findDOMNode(node);
}