'use strict';

/* global browser */
/* global element */
/* global by */

describe('Controller: GridsterCtrl', function() {
	var items,
		firstItem;

	beforeEach(function() {
		browser.get('test.html');
		browser.driver.manage()
			.window()
			.setSize(1000, 1000);
		items = element.all(by.css('[gridster-item]'));
		firstItem = items.get(0);
	});

	it('should have a page with elements', function() {
		element.all(by.repeater('item in standardItems')).then(function(items) {
			expect(items.length).toEqual(11);
		});

		browser.findElement(by.css('h2:first-child')).then(function(el) {
			return el.getText().then(function(text) {
				expect(text).toBe('Standard Items');
			});
		});
	});

	it('should allow the user to enter a size', function() {
		var width = 0;

		firstItem.getSize().then(function(size) {
			expect(size.width).toBeGreaterThan(0);
			width = size.width;
		}).then(function() {
			return firstItem.element(by.model('item.sizeX'));
		}).then(function(input) {
			return input.sendKeys('2').then(function() {
				input.sendKeys(protractor.Key.TAB);
			});
		}).then(function() {
			return firstItem.getSize();
		}).then(function(size) {
			expect(size.width).toBeGreaterThan(width);
		});
	});

	it('should resize the row widths and heights', function() {
		var initialSize;

		browser.driver.manage().window().setSize(1200, 1200);
		firstItem.getSize()
			.then(function setInitialSize(size) {
				initialSize = size;
			})
			.then(function() {
				browser.driver.manage().window().setSize(1000, 1000);
				firstItem.getSize().then(function(newSize) {
					expect(newSize.width).toBeLessThan(initialSize.width);
					expect(newSize.height).toBeLessThan(initialSize.height);
				});
			});
	});
});
