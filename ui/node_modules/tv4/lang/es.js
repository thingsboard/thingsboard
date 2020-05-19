(function (global) {
	var lang = {
		INVALID_TYPE: "Tipo inválido: {type} (se esperaba {expected})",
		ENUM_MISMATCH: "No hay enum que corresponda con: {value}",
		ANY_OF_MISSING: "Los datos no corresponden con ningún esquema de \"anyOf\"",
		ONE_OF_MISSING: "Los datos no corresponden con ningún esquema de \"oneOf\"",
		ONE_OF_MULTIPLE: "Los datos son válidos contra más de un esquema de \"oneOf\": índices {index1} y {index2}",
		NOT_PASSED: "Los datos se corresponden con el esquema de \"not\"",
		// Errores numéricos
		NUMBER_MULTIPLE_OF: "El valor {value} no es múltiplo de {multipleOf}",
		NUMBER_MINIMUM: "El {value} es inferior al mínimo {minimum}",
		NUMBER_MINIMUM_EXCLUSIVE: "El valor {value} es igual que el mínimo exclusivo {minimum}",
		NUMBER_MAXIMUM: "El valor {value} es mayor que el máximo {maximum}",
		NUMBER_MAXIMUM_EXCLUSIVE: "El valor {value} es igual que el máximo exclusivo {maximum}",
		NUMBER_NOT_A_NUMBER: "El valor {value} no es un número válido",
		// Errores de cadena
		STRING_LENGTH_SHORT: "La cadena es demasiado corta ({length} chars), mínimo {minimum}",
		STRING_LENGTH_LONG: "La cadena es demasiado larga ({length} chars), máximo {maximum}",
		STRING_PATTERN: "La cadena no se corresponde con el patrón: {pattern}",
		// Errores de objeto
		OBJECT_PROPERTIES_MINIMUM: "No se han definido suficientes propiedades ({propertyCount}), mínimo {minimum}",
		OBJECT_PROPERTIES_MAXIMUM: "Se han definido demasiadas propiedades ({propertyCount}), máximo {maximum}",
		OBJECT_REQUIRED: "Falta la propiedad requerida: {key}",
		OBJECT_ADDITIONAL_PROPERTIES: "No se permiten propiedades adicionales",
		OBJECT_DEPENDENCY_KEY: "Dependencia fallida - debe existir la clave: {missing} (debido a la clave: {key})",
		// Errores de array
		ARRAY_LENGTH_SHORT: "Array demasiado corto ({length}), mínimo {minimum}",
		ARRAY_LENGTH_LONG: "Array demasiado largo ({length}), máximo {maximum}",
		ARRAY_UNIQUE: "Elementos de array no únicos (índices {match1} y {match2})",
		ARRAY_ADDITIONAL_ITEMS: "Elementos adicionales no permitidos",
		// Errores de formato
		FORMAT_CUSTOM: "Fallo en la validación del formato ({message})",
		KEYWORD_CUSTOM: "Fallo en la palabra clave: {key} ({message})",
		// Estructura de esquema
		CIRCULAR_REFERENCE: "Referencias $refs circulares: {urls}",
		// Opciones de validación no estándar
		UNKNOWN_PROPERTY: "Propiedad desconocida (no existe en el esquema)"
	};

	if (typeof define === 'function' && define.amd) {
		// AMD. Register as an anonymous module.
		define(['../tv4'], function(tv4) {
			tv4.addLanguage('es', lang);
			return tv4;
		});
	} else if (typeof module !== 'undefined' && module.exports){
		// CommonJS. Define export.
		var tv4 = require('../tv4');
		tv4.addLanguage('es', lang);
		module.exports = tv4;
	} else {
		// Browser globals
		global.tv4.addLanguage('es', lang);
	}
})(this);
