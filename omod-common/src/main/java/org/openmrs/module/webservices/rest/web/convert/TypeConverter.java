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

import org.openmrs.module.webservices.rest.web.response.ConversionException;

/**
 * A single, self-contained conversion algorithm (Strategy pattern). Each implementation knows how to
 * convert one kind of source value to one kind of target type. The strategies are looked up by the
 * {@link TypeConverterRegistry} and used by
 * {@link org.openmrs.module.webservices.rest.web.ConversionUtil#convert(Object, Type)} instead of one
 * large {@code if/else} chain. Adding a new conversion means adding a new {@code TypeConverter} and
 * registering it - existing code stays untouched (Open/Closed principle).
 */
public interface TypeConverter {

	/**
	 * @param source the non-null value to convert
	 * @param toClass the raw target class (already resolved from {@code toType})
	 * @param toType the target type, which may carry generic information
	 * @return {@code true} if this strategy is responsible for converting the given source to the
	 *         given target type
	 */
	boolean canConvert(Object source, Class<?> toClass, Type toType);

	/**
	 * Performs the conversion. Only called when {@link #canConvert(Object, Class, Type)} returned
	 * {@code true} for the same arguments.
	 *
	 * @param source the non-null value to convert
	 * @param toClass the raw target class
	 * @param toType the target type, which may carry generic information
	 * @return the converted value
	 * @throws ConversionException if the value cannot be converted
	 */
	Object convert(Object source, Class<?> toClass, Type toType) throws ConversionException;
}
