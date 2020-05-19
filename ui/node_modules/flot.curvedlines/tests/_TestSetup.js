! function() {

	var TestSetup = function(div, lineParameter, replotFunctions) {
		div.append("<div id='normalParameters' class='parameterBox'></div>");
		$("#normalParameters").append("<input class='parameterInput' id='apply' type='checkbox' onchange='TestSetup.applyChanged()'>apply</input>");
		$("#normalParameters").append("<input class='parameterInput' id='monotonicFit' type='checkbox' onchange='TestSetup.monotonicFitChanged()'>monotonicFit</input>");
		$("#normalParameters").append("<input class='parameterInput' id='tension' type='range' min='0' max='1' step='0.1' onchange='TestSetup.tensionChanged()'>tension /in [0,1]</input>");
		$("#normalParameters").append("<input class='parameterInput' id='nrSplinePoints' type='text' onchange='TestSetup.nrSplinePointsChanged()'># spline points</input>");

		div.append("<div id='legacyParameters' class='parameterBox'></div>");
		$("#legacyParameters").append("<input class='parameterInput' id='useLegacy' type='checkbox' onchange='TestSetup.useLegacyChanged()'>use legacy options</input>");
		$("#legacyParameters").append("<input class='parameterInput' id='legacyFit' type='checkbox' onchange='TestSetup.legacyFitChanged()'>fit</input>");
		$("#legacyParameters").append("<input class='parameterInput' id='legacyPointFactor' type='text' onchange='TestSetup.legacyPointFactorChanged()'>point factor</input>");
		$("#legacyParameters").append("<input class='parameterInput' id='legacyFitPointDist' type='text' onchange='TestSetup.legacyFitPointDistChanged()'>fit point dist</input>");

		function replotAll() {
			
			var parameter = {
				apply: $("#apply").prop("checked"),		
				monotonicFit: $("#monotonicFit").prop("checked"),
				tension: $("#tension").val(),
				nrSplinePoints: $("#nrSplinePoints").val(),
				legacyOverride: undefined
			};
			
			if ($("#useLegacy").prop("checked")) {
				var fDist = $("#legacyFitPointDist").val();
				
				parameter.legacyOverride = {
					fit: $("#legacyFit").prop("checked"),		
					curvePointFactor: $("#legacyPointFactor").val(),
					fitPointDist: (fDist == '') ? undefined : fDist,
				};
			}
						
			for (var i = 0; i < replotFunctions.length; i++) {
				replotFunctions[i](parameter);
			}
		}

		function init(parameter) {

			var defaultParam = {
				active: false,
				apply: false,
				monotonicFit: false,
				tension: 0.0,
				nrSplinePoints: 20,
				legacyOverride: undefined
			};
			
			var defaultLegacy = {
				fit: false,
				curvePointFactor: 20,
				fitPointDist: undefined
			};

			if (typeof parameter.legacyOverride != 'undefined' ) {
				defaultParam.legacyOverride = defaultLegacy;
				if (parameter.legacyOverride == true) {
					parameter.legacyOverride = defaultLegacy;
				}
			}

			var combinedParam = jQuery.extend(true, defaultParam, parameter);

			$("#apply").prop("checked", combinedParam.apply);

			var withLegacy = (typeof combinedParam.legacyOverride != 'undefined');
			var fit = combinedParam.legacyOverride.fit;
			var pointFactor = combinedParam.legacyOverride.curvePointFactor;
			var fitDist = combinedParam.legacyOverride.fitPointDist;
			var monotone = combinedParam.monotonicFit;
			var tension = combinedParam.tension;
			var nrPoints = combinedParam.nrSplinePoints;

			$("#useLegacy").prop("checked", withLegacy);
			$("#legacyFit").prop("checked", fit);
			$("#legacyPointFactor").val(pointFactor);
			$("#legacyFitPointDist").val(fitDist);
			$("#monotonicFit").prop("checked", monotone);
			$("#tension").val(tension);
			$("#nrSplinePoints").val(nrPoints);

			replotAll(parameter, replotFunctions);
		}


		TestSetup.applyChanged = function() {
			replotAll();
		};

		TestSetup.useLegacyChanged = function() {
			replotAll();
		};

		TestSetup.legacyFitChanged = function() {
			if ($("#useLegacy").prop("checked")) {
				replotAll();
			}
		};

		TestSetup.legacyPointFactorChanged = function() {
			if ($("#useLegacy").prop("checked")) {
				replotAll();
			}
		};

		TestSetup.legacyFitPointDistChanged = function() {
			if ($("#useLegacy").prop("checked")) {
				replotAll();
			}
		};

		TestSetup.monotonicFitChanged = function() {
			$("#useLegacy").prop("checked", false);
			replotAll();
		};

		TestSetup.tensionChanged = function() {
			$("#useLegacy").prop("checked", false);
			replotAll();
		};

		TestSetup.nrSplinePointsChanged = function() {
			$("#useLegacy").prop("checked", false);
			replotAll();
		};

		init(lineParameter);
	};

	this.TestSetup = TestSetup;
}(); 