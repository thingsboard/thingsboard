const _ = require('lodash');

module.exports = function checkOption(opts, primaryOption, secondaryOption, value) {
	const secondaryOptionValues = _.get(opts[primaryOption][1], secondaryOption);

	return _.includes(secondaryOptionValues, value);
};
