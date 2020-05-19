/* eslint-disable max-len */
var attributes = "accept acceptCharset accessKey action allowFullScreen allowTransparency\n    alt async autoComplete autoFocus autoPlay capture cellPadding cellSpacing challenge\n    charSet checked classID className colSpan cols content contentEditable contextMenu\n    controls coords crossOrigin data dateTime default defer dir disabled download draggable\n    encType form formAction formEncType formMethod formNoValidate formTarget frameBorder\n    headers height hidden high href hrefLang htmlFor httpEquiv icon id inputMode integrity\n    is keyParams keyType kind label lang list loop low manifest marginHeight marginWidth max maxLength media\n    mediaGroup method min minLength multiple muted name noValidate nonce open\n    optimum pattern placeholder poster preload radioGroup readOnly rel required\n    reversed role rowSpan rows sandbox scope scoped scrolling seamless selected\n    shape size sizes span spellCheck src srcDoc srcLang srcSet start step style\n    summary tabIndex target title type useMap value width wmode wrap".replace(/\s+/g, ' ').replace(/\t|\n|\r/g, '').split(' ');
var eventsName = "onCopy onCut onPaste onCompositionEnd onCompositionStart onCompositionUpdate onKeyDown\n    onKeyPress onKeyUp onFocus onBlur onChange onInput onSubmit onClick onContextMenu onDoubleClick\n    onDrag onDragEnd onDragEnter onDragExit onDragLeave onDragOver onDragStart onDrop onMouseDown\n    onMouseEnter onMouseLeave onMouseMove onMouseOut onMouseOver onMouseUp onSelect onTouchCancel\n    onTouchEnd onTouchMove onTouchStart onScroll onWheel onAbort onCanPlay onCanPlayThrough\n    onDurationChange onEmptied onEncrypted onEnded onError onLoadedData onLoadedMetadata\n    onLoadStart onPause onPlay onPlaying onProgress onRateChange onSeeked onSeeking onStalled onSuspend onTimeUpdate onVolumeChange onWaiting onLoad onError".replace(/\s+/g, ' ').replace(/\t|\n|\r/g, '').split(' ');
/* eslint-enable max-len */

var attrsPrefix = ['data', 'aria'];
export default function pickAttrs(props) {
  var attrs = {};

  var _loop = function _loop(key) {
    if (attributes.indexOf(key) > -1 || eventsName.indexOf(key) > -1) {
      attrs[key] = props[key];
      /* eslint-disable no-loop-func */
    } else if (attrsPrefix.map(function (prefix) {
      return new RegExp("^".concat(prefix));
    }).some(function (reg) {
      return key.replace(reg, '') !== key;
    })) {
      /* eslint-enable no-loop-func */
      attrs[key] = props[key];
    }
  };

  for (var key in props) {
    _loop(key);
  }

  return attrs;
}