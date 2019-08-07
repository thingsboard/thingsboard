// functional re-impl of L.Point.distanceTo,
// with no dependency on Leaflet for easier testing
function pointDistance(ptA, ptB) {
	const x = ptB.x - ptA.x;
	const y = ptB.y - ptA.y;
	return Math.sqrt(x * x + y * y);
}

const computeSegmentHeading = (a, b) =>
    ((Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI) + 90 + 360) % 360;

const asRatioToPathLength = ({ value, isInPixels }, totalPathLength) =>
    isInPixels ? value / totalPathLength : value;

function parseRelativeOrAbsoluteValue(value) {
    if (typeof value === 'string' && value.indexOf('%') !== -1) {
        return {
            value: parseFloat(value) / 100,
            isInPixels: false,
        };
    }
    const parsedValue = value ? parseFloat(value) : 0;
    return {
        value: parsedValue,
        isInPixels: parsedValue > 0,
    };
}

const pointsEqual = (a, b) => a.x === b.x && a.y === b.y;

function pointsToSegments(pts) {
    return pts.reduce((segments, b, idx, points) => {
        // this test skips same adjacent points
        if (idx > 0 && !pointsEqual(b, points[idx - 1])) {
            const a = points[idx - 1];
            const distA = segments.length > 0 ? segments[segments.length - 1].distB : 0;
            const distAB = pointDistance(a, b);
            segments.push({
                a,
                b,
                distA,
                distB: distA + distAB,
                heading: computeSegmentHeading(a, b),
            });
        }
        return segments;
    }, []);
}

function projectPatternOnPointPath(pts, pattern) {
    // 1. split the path into segment infos
    const segments = pointsToSegments(pts);
    const nbSegments = segments.length;
    if (nbSegments === 0) { return []; }

    const totalPathLength = segments[nbSegments - 1].distB;

    const offset = asRatioToPathLength(pattern.offset, totalPathLength);
    const endOffset = asRatioToPathLength(pattern.endOffset, totalPathLength);
    const repeat = asRatioToPathLength(pattern.repeat, totalPathLength);

    const repeatIntervalPixels = totalPathLength * repeat;
    const startOffsetPixels = offset > 0 ? totalPathLength * offset : 0;
    const endOffsetPixels = endOffset > 0 ? totalPathLength * endOffset : 0;

    // 2. generate the positions of the pattern as offsets from the path start
    const positionOffsets = [];
    let positionOffset = startOffsetPixels;
    do {
        positionOffsets.push(positionOffset);
        positionOffset += repeatIntervalPixels;
    } while(repeatIntervalPixels > 0 && positionOffset < totalPathLength - endOffsetPixels);

    // 3. projects offsets to segments
    let segmentIndex = 0;
    let segment = segments[0];
    return positionOffsets.map(positionOffset => {
        // find the segment matching the offset,
        // starting from the previous one as offsets are ordered
        while (positionOffset > segment.distB && segmentIndex < nbSegments - 1) {
            segmentIndex++;
            segment = segments[segmentIndex];
        }

        const segmentRatio = (positionOffset - segment.distA) / (segment.distB - segment.distA);
        return {
            pt: interpolateBetweenPoints(segment.a, segment.b, segmentRatio),
            heading: segment.heading,
        };
    });
}

/**
* Finds the point which lies on the segment defined by points A and B,
* at the given ratio of the distance from A to B, by linear interpolation.
*/
function interpolateBetweenPoints(ptA, ptB, ratio) {
    if (ptB.x !== ptA.x) {
        return {
            x: ptA.x + ratio * (ptB.x - ptA.x),
            y: ptA.y + ratio * (ptB.y - ptA.y),
        };
    }
    // special case where points lie on the same vertical axis
    return {
        x: ptA.x,
        y: ptA.y + (ptB.y - ptA.y) * ratio,
    };
}

export {
    projectPatternOnPointPath,
    parseRelativeOrAbsoluteValue,
    // the following function are exported only for unit testing purpose
    computeSegmentHeading,
    asRatioToPathLength,
};
