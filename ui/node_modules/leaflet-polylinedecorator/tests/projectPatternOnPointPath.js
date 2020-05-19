import test from 'ava';
import {
	projectPatternOnPointPath,
} from '../src/patternUtils.js'

const A = { x: 0, y: 0 };
const B = { x: 1, y: 0 };
const C = { x: 1, y: 1 };
const D = { x: 0, y: 1 };

const patternStart = {
	offset: { value: 0, isInPixels: true },
	endOffset: { value: 0, isInPixels: true },
	repeat: { value: 0, isInPixels: true },
}

const patternEnd = {
	offset: { value: 1, isInPixels: false },
	endOffset: { value: 0, isInPixels: true },
	repeat: { value: 0, isInPixels: true },
}

const patternQuarter = {
	offset: { value: 0.25, isInPixels: false },
	endOffset: { value: 0, isInPixels: true },
	repeat: { value: 0, isInPixels: true },
}

const pattern5Repeat = {
	offset: { value: 0.1, isInPixels: false },
	endOffset: { value: 0, isInPixels: true },
	repeat: { value: 0.2, isInPixels: false },
}

const pattern3Repeat = {
	offset: { value: 0.1, isInPixels: false },
	endOffset: { value: 0.4, isInPixels: false },
	repeat: { value: 0.2, isInPixels: false },
}


test('returns an empty array if the line has a zero-length', t => {
	t.deepEqual(projectPatternOnPointPath([B, B, B], patternStart), []);
});

test('returns an empty array if the line has zero segments', t => {
	t.deepEqual(projectPatternOnPointPath([A], patternStart), []);
});

test('returns a single position with start point and heading of the first segment for a pattern with offset 0', t => {
	const positions = projectPatternOnPointPath([A, B, C], patternStart);
	t.deepEqual(positions, [{
		pt: A,
		heading: 90,
	}]);
});

test('returns a single position with end point and heading of the last segment for a pattern with offset 100%', t => {
	const positions = projectPatternOnPointPath([A, B, C], patternEnd);
	t.deepEqual(positions, [{
		pt: C,
		heading: 180,
	}]);
});

test('computes the position as ratio of the line length', t => {
	const positions = projectPatternOnPointPath([A, B], patternQuarter);
	t.deepEqual(positions, [{
		pt: {
			x: 0.25,
			y: 0,
		},
		heading: 90,
	}]);
});

test('returns multiple positions if repeats are specified', t => {
	const positions = projectPatternOnPointPath([A, B], pattern5Repeat);
	t.is(positions.length, 5);
});

test('does not repeat positions beyond the end offset', t => {
	const positions = projectPatternOnPointPath([A, B], pattern3Repeat);
	t.is(positions.length, 3);
});

test('ignores empty segments', t => {
	t.deepEqual(
		projectPatternOnPointPath([A, A, B, B, B, C, D, D, D], patternStart),
		projectPatternOnPointPath([A, B, C, D], patternStart)
	);
	t.deepEqual(
		projectPatternOnPointPath([A, A, B, B, B, C, D, D, D], patternQuarter),
		projectPatternOnPointPath([A, B, C, D], patternQuarter)
	);
	t.deepEqual(
		projectPatternOnPointPath([A, A, B, B, B, C, D, D, D], pattern3Repeat),
		projectPatternOnPointPath([A, B, C, D], pattern3Repeat)
	);
});
