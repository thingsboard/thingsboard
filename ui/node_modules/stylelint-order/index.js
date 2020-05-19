const { createPlugin } = require('stylelint');
const { namespace } = require('./utils');
const rules = require('./rules');

const rulesPlugins = Object.keys(rules).map(ruleName => {
	return createPlugin(namespace(ruleName), rules[ruleName]);
});

module.exports = rulesPlugins;
