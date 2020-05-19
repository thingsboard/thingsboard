const prefix = 'order';

module.exports = function namespace(ruleName) {
	return `${prefix}/${ruleName}`;
};
