/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.convert;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the ordered list of {@link TypeConverter} strategies and returns the first one that can
 * handle a given conversion (Registry / lookup of the Strategy pattern). The registry replaces the
 * long {@code if/else} chain that used to live inside
 * {@link org.openmrs.module.webservices.rest.web.ConversionUtil#convert(Object, Type)}.
 * <p>
 * Order matters: the strategies are tried top to bottom, which preserves the precedence of the
 * original conditional (collections first, an already-assignable value next, then the type-specific
 * conversions).
 * <p>
 * New conversions can be plugged in without touching {@code ConversionUtil} via
 * {@link #register(TypeConverter)} (Open/Closed principle).
 */
public class TypeConverterRegistry {

	private static final List<TypeConverter> converters = new CopyOnWriteArrayList<TypeConverter>();

	static {
		// The order mirrors the precedence of the former convert() if/else chain.
		converters.add(new CollectionConverter());
		converters.add(new AssignablePassThroughConverter());
		converters.add(new DoubleToFloatConverter());
		converters.add(new StringConverter());
		converters.add(new MapConverter());
		converters.add(new NumberToDoubleConverter());
		converters.add(new NumberToIntegerConverter());
		converters.add(new BooleanToStringConverter());
	}

	private TypeConverterRegistry() {
	}

	/**
	 * Registers an additional strategy. It is appended after the built-in strategies, so it only
	 * kicks in for conversions none of the built-ins already claim.
	 *
	 * @param converter the strategy to add
	 */
	public static void register(TypeConverter converter) {
		converters.add(converter);
	}

	/**
	 * @param source the non-null value to convert
	 * @param toClass the raw target class
	 * @param toType the target type
	 * @return the first strategy able to convert the value, or {@code null} if none applies
	 */
	public static TypeConverter find(Object source, Class<?> toClass, Type toType) {
		for (TypeConverter converter : converters) {
			if (converter.canConvert(source, toClass, toType)) {
				return converter;
			}
		}
		return null;
	}
}
