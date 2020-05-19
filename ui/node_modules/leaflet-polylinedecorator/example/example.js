
function init() {
    var map = L.map('map', {
        center: [52.0, -11.0],
        zoom: 5,
        layers: [
            L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
            })
        ]
    });

    // --- Simple arrow ---
    var arrow = L.polyline([[57, -19], [60, -12]], {}).addTo(map);
    var arrowHead = L.polylineDecorator(arrow, {
        patterns: [
            {offset: '100%', repeat: 0, symbol: L.Symbol.arrowHead({pixelSize: 15, polygon: false, pathOptions: {stroke: true}})}
        ]
    }).addTo(map);

    // --- Polygon, with an inner ring ---
    var polygon = L.polygon([[[54, -6], [55, -7], [56, -2], [55, 1], [53, 0]], [[54, -3], [54, -2], [55, -1], [55, -5]]], {color: "#ff7800", weight: 1}).addTo(map);
    var pd = L.polylineDecorator(polygon, {
        patterns: [
            {offset: 0, repeat: 10, symbol: L.Symbol.dash({pixelSize: 0})}
        ]
    }).addTo(map);

    // --- Multi-pattern without Polyline ---
    var pathPattern = L.polylineDecorator(
        [ [ 49.543519, -12.469833 ], [ 49.808981, -12.895285 ], [ 50.056511, -13.555761 ], [ 50.217431, -14.758789 ], [ 50.476537, -15.226512 ], [ 50.377111, -15.706069 ], [ 50.200275, -16.000263 ], [ 49.860606, -15.414253 ], [ 49.672607, -15.710152 ], [ 49.863344, -16.451037 ], [ 49.774564, -16.875042 ], [ 49.498612, -17.106036 ], [ 49.435619, -17.953064 ], [ 49.041792, -19.118781 ], [ 48.548541, -20.496888 ], [ 47.930749, -22.391501 ], [ 47.547723, -23.781959 ], [ 47.095761, -24.941630 ], [ 46.282478, -25.178463 ], [ 45.409508, -25.601434 ], [ 44.833574, -25.346101 ], [ 44.039720, -24.988345 ] ],
        {
            patterns: [
                { offset: 12, repeat: 25, symbol: L.Symbol.dash({pixelSize: 10, pathOptions: {color: '#f00', weight: 2}}) },
                { offset: 0, repeat: 25, symbol: L.Symbol.dash({pixelSize: 0}) }
            ]
        }
    ).addTo(map);

    // --- Markers proportionnaly located ---
    var markerLine = L.polyline([[58.44773, -28.65234], [52.9354, -23.33496], [53.01478, -14.32617], [58.1707, -10.37109], [59.68993, -0.65918]], {}).addTo(map);
    var markerPatterns = L.polylineDecorator(markerLine, {
        patterns: [
            { offset: '5%', repeat: '10%', symbol: L.Symbol.marker()}
        ]
    }).addTo(map);

    // --- Example with a rotated marker ---
    var pathPattern = L.polylineDecorator(
        [ [ 42.9, -15 ], [ 44.18, -11.4 ], [ 45.77, -8.0 ], [ 47.61, -6.4 ], [ 49.41, -6.1 ], [ 51.01, -7.2 ] ],
        {
            patterns: [
                { offset: 0, repeat: 10, symbol: L.Symbol.dash({pixelSize: 5, pathOptions: {color: '#000', weight: 1, opacity: 0.2}}) },
                { offset: '16%', repeat: '33%', symbol: L.Symbol.marker({rotate: true, markerOptions: {
                    icon: L.icon({
                        iconUrl: 'icon_plane.png',
                        iconAnchor: [16, 16]
                    })
                }})}
            ]
        }
    ).addTo(map);

    // --- Example with an array of Polylines ---
    var multiCoords1 = [
        [[47.5468, -0.7910], [48.8068, -0.1318], [49.1242, 1.6699], [49.4966, 3.2958], [51.4266, 2.8564], [51.7542, 2.1093]],
        [[48.0193, -2.8125], [46.3165, -2.8564], [44.9336, -1.0107], [44.5278, 1.5820], [44.8714, 3.7353], [45.8287, 5.1855], [48.1953, 5.1416]],
        [[45.9205, 0.4394], [46.7699, 0.9228], [47.6061, 2.5488], [47.7540, 3.3837]]
    ];
    var plArray = [];
    for(var i=0; i<multiCoords1.length; i++) {
        plArray.push(L.polyline(multiCoords1[i]).addTo(map));
    }
    L.polylineDecorator(multiCoords1, {
        patterns: [
            {offset: 25, repeat: 50, symbol: L.Symbol.arrowHead({pixelSize: 15, pathOptions: {fillOpacity: 1, weight: 0}})}
        ]
    }).addTo(map);
}
