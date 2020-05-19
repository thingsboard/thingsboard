'use strict';

var _chalk = require('chalk');

var _chalk2 = _interopRequireDefault(_chalk);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var utils = require('../lib/utils');
var debug = utils.debug;
var fetch = require('node-fetch');

var fetchStats = function fetchStats(collectiveUrl) {
  var url = collectiveUrl + '.json';
  return fetch(url, { timeout: 1500 }).then(function (res) {
    return res.json();
  }).then(function (json) {
    return {
      currency: json.currency,
      balance: json.balance,
      yearlyIncome: json.yearlyIncome,
      backersCount: json.backersCount,
      contributorsCount: json.contributorsCount
    };
  }).catch(function (e) {
    var collectiveSlug = collectiveUrl.substr(collectiveUrl.lastIndexOf('/') + 1);
    console.error(_chalk2.default.red('[server error]') + ' Cannot load the stats for ' + collectiveSlug + ' \u2013 please try again later');
    debug("Error while fetching ", url);
  });
};

var fetchBanner = function fetchBanner(slug) {
  var url = 'https://opencollective.com/' + slug + '/banner.md';
  return fetch(url).then(function (res) {
    return res.text();
  }).catch(function (e) {
    debug("Error while fetching ", url);
  });
};

var fetchLogo = function fetchLogo(logoUrl) {
  if (!logoUrl.match(/^https?:\/\//)) {
    return "";
  }
  return fetch(logoUrl, { timeout: 1500 }).then(function (res) {
    if (res.status === 200 && res.headers.get('content-type').match(/^text\/plain/)) return res.text();else return "";
  }).catch(function (e) {
    debug("Error while fetching ", logoUrl);
  });
};

module.exports = {
  fetchLogo: fetchLogo, fetchStats: fetchStats, fetchBanner: fetchBanner
};