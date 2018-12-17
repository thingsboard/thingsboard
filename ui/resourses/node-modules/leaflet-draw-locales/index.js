const en = require('./locales/en')
const fr = require('./locales/fr')
const es = require('./locales/es')
const zh = require('./locales/zh')

/**
 * Localization for Leaflet.draw, changing between languages is now effortless.
 *
 * @param {string} [language='en'] Language to localize Leaflet.draw
 * @example
 * var L = require('leaflet')
 * var drawLocales = require('leaflet-draw-locales')
 *
 * // Automatically defines Leaflet.draw locale
 * drawLocales('fr')
 *
 * // Customize locale language
 * var locale = drawLocales('fr')
 * locale.draw.toolbar.buttons.polygon = 'Awesome polygon!'
 * L.drawLocal = locale
 */
module.exports = function (language) {
  var locale
  switch (language) {
    case 'en':
    case 'en_US':
    case 'en_US.UTF-8':
    case 'english': {
      locale = en
      break
    }
    case 'fr':
    case 'fr_US':
    case 'fr_US.UTF-8':
    case 'french': {
      locale = fr
      break
    }
    case 'zh':
    case 'zh_CN':
    case 'zh_CN.UTF-8':
    case 'chinese': {
      locale = zh
      break
    }    
    case 'es':
    case 'es_US':
    case 'es_US.UTF-8':
    case 'spanish': {
      locale = es
      break
    }
    default:
      locale = en
      break
  }
  // Automatically defines Leaflet.draw locale
  try {
    if (L && L.drawLocal) L.drawLocal = locale // eslint-disable-line
  } catch (e) {
    // Did not modify Leaflet.draw global
  }
  return locale
}
