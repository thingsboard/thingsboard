"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _postcss = _interopRequireDefault(require("postcss"));

var _postcssValueParser = _interopRequireDefault(require("postcss-value-parser"));

var _utils = require("../utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const pluginName = 'postcss-url-parser';
const isUrlFunc = /url/i;
const isImageSetFunc = /^(?:-webkit-)?image-set$/i;
const needParseDecl = /(?:url|(?:-webkit-)?image-set)\(/i;

function getNodeFromUrlFunc(node) {
  return node.nodes && node.nodes[0];
}

function walkUrls(parsed, callback) {
  parsed.walk(node => {
    if (node.type !== 'function') {
      return;
    }

    if (isUrlFunc.test(node.value)) {
      const {
        nodes
      } = node;
      const isStringValue = nodes.length !== 0 && nodes[0].type === 'string';
      const url = isStringValue ? nodes[0].value : _postcssValueParser.default.stringify(nodes);
      callback(getNodeFromUrlFunc(node), url, false, isStringValue); // Do not traverse inside `url`
      // eslint-disable-next-line consistent-return

      return false;
    }

    if (isImageSetFunc.test(node.value)) {
      node.nodes.forEach(nNode => {
        const {
          type,
          value
        } = nNode;

        if (type === 'function' && isUrlFunc.test(value)) {
          const {
            nodes
          } = nNode;
          const isStringValue = nodes.length !== 0 && nodes[0].type === 'string';
          const url = isStringValue ? nodes[0].value : _postcssValueParser.default.stringify(nodes);
          callback(getNodeFromUrlFunc(nNode), url, false, isStringValue);
        }

        if (type === 'string') {
          callback(nNode, value, true, true);
        }
      }); // Do not traverse inside `image-set`
      // eslint-disable-next-line consistent-return

      return false;
    }
  });
}

function getUrlsFromValue(value, result, filter, decl) {
  if (!needParseDecl.test(value)) {
    return;
  }

  const parsed = (0, _postcssValueParser.default)(value);
  const urls = [];
  walkUrls(parsed, (node, url, needQuotes, isStringValue) => {
    if (url.trim().replace(/\\[\r\n]/g, '').length === 0) {
      result.warn(`Unable to find uri in '${decl ? decl.toString() : value}'`, {
        node: decl
      });
      return;
    }

    if (filter && !filter(url)) {
      return;
    }

    const splittedUrl = url.split(/(\?)?#/);
    const [urlWithoutHash, singleQuery, hashValue] = splittedUrl;
    const hash = singleQuery || hashValue ? `${singleQuery ? '?' : ''}${hashValue ? `#${hashValue}` : ''}` : '';
    const normalizedUrl = (0, _utils.normalizeUrl)(urlWithoutHash, isStringValue);
    urls.push({
      node,
      url: normalizedUrl,
      hash,
      needQuotes
    });
  }); // eslint-disable-next-line consistent-return

  return {
    parsed,
    urls
  };
}

function walkDecls(css, result, filter) {
  const items = [];
  css.walkDecls(decl => {
    const item = getUrlsFromValue(decl.value, result, filter, decl);

    if (!item || item.urls.length === 0) {
      return;
    }

    items.push({
      decl,
      parsed: item.parsed,
      urls: item.urls
    });
  });
  return items;
}

function flatten(array) {
  return array.reduce((a, b) => a.concat(b), []);
}

function collectUniqueUrlsWithNodes(array) {
  return array.reduce((accumulator, currentValue) => {
    const {
      url,
      needQuotes,
      hash,
      node
    } = currentValue;
    const found = accumulator.find(item => url === item.url && needQuotes === item.needQuotes && hash === item.hash);

    if (!found) {
      accumulator.push({
        url,
        hash,
        needQuotes,
        nodes: [node]
      });
    } else {
      found.nodes.push(node);
    }

    return accumulator;
  }, []);
}

var _default = _postcss.default.plugin(pluginName, options => function process(css, result) {
  const traversed = walkDecls(css, result, options.filter);
  const flattenTraversed = flatten(traversed.map(item => item.urls));
  const urlsWithNodes = collectUniqueUrlsWithNodes(flattenTraversed);
  const replacers = new Map();
  urlsWithNodes.forEach((urlWithNodes, index) => {
    const {
      url,
      hash,
      needQuotes,
      nodes
    } = urlWithNodes;
    const replacementName = `___CSS_LOADER_URL_REPLACEMENT_${index}___`;
    result.messages.push({
      pluginName,
      type: 'import',
      value: {
        type: 'url',
        replacementName,
        url,
        needQuotes,
        hash
      }
    }, {
      pluginName,
      type: 'replacer',
      value: {
        type: 'url',
        replacementName
      }
    });
    nodes.forEach(node => {
      replacers.set(node, replacementName);
    });
  });
  traversed.forEach(item => {
    walkUrls(item.parsed, node => {
      const replacementName = replacers.get(node);

      if (!replacementName) {
        return;
      } // eslint-disable-next-line no-param-reassign


      node.type = 'word'; // eslint-disable-next-line no-param-reassign

      node.value = replacementName;
    }); // eslint-disable-next-line no-param-reassign

    item.decl.value = item.parsed.toString();
  });
});

exports.default = _default;