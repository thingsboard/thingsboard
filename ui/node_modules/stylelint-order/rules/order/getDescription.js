const _ = require('lodash');

module.exports = function getDescription(item) {
	const descriptions = {
		'custom-properties': 'custom property',
		'dollar-variables': '$-variable',
		'at-variables': '@-variable',
		'less-mixins': 'Less mixin',
		declarations: 'declaration',
	};

	if (_.isPlainObject(item)) {
		let text;

		if (item.type === 'at-rule') {
			text = 'at-rule';

			if (item.name) {
				text = `@${item.name}`;
			}

			if (item.parameter) {
				text += ` "${item.parameter}"`;
			}

			if (item.hasOwnProperty('hasBlock')) {
				if (item.hasBlock) {
					text += ' with a block';
				} else {
					text = `blockless ${text}`;
				}
			}
		}

		if (item.type === 'rule') {
			text = 'rule';

			if (item.selector) {
				text += ` with selector matching "${item.selector}"`;
			}
		}

		return text;
	}

	// Return description for keyword patterns
	return descriptions[item];
};
