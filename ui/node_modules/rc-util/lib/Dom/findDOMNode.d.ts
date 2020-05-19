/// <reference types="react" />
/**
 * Return if a node is a DOM node. Else will return by `findDOMNode`
 */
export default function findDOMNode<T = Element | Text>(node: React.ReactInstance | HTMLElement): T;
