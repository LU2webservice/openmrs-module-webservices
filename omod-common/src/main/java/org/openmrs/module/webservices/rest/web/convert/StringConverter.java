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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.util.LocaleUtility;

/**
 * Converts a {@link String} source to whatever target type was requested: via a registered REST
 * {@link Converter} (by unique id), to a {@link Date} (several ISO formats), {@link Locale},
 * {@code enum}, {@link Class}, or any type that exposes a static {@code valueOf(String)} method.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class StringConverter implements TypeConverter {

	private static final Log log = LogFactory.getLog(StringConverter.class);

	@Override
	public boolean canConvert(Object source, Class<?> toClass, Type toType) {
		return source instanceof String;
	}

	@Override
	public Object convert(Object source, Class<?> toClass, Type toType) throws ConversionException {
		String string = (String) source;

		Converter<?> converter = ConversionUtil.getConverter(toClass);
		if (converter != null) {
			return converter.getByUniqueId(string);
		}

		if (toClass.isAssignableFrom(Date.class)) {
			IllegalArgumentException pex = null;
			String[] supportedFormats = { ConversionUtil.DATE_FORMAT, "yyyy-MM-dd'T'HH:mm:ss.SSS",
			        "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss",
			        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };
			for (int i = 0; i < supportedFormats.length; i++) {
				try {
					return DateTime.parse(string, DateTimeFormat.forPattern(supportedFormats[i])).toDate();
				}
				catch (IllegalArgumentException ex) {
					pex = ex;
				}
			}
			throw new ConversionException(
			        "Error converting date - correct format (ISO8601 Long): yyyy-MM-dd'T'HH:mm:ss.SSSZ", pex);
		} else if (toClass.isAssignableFrom(Locale.class)) {
			return LocaleUtility.fromSpecification(string);
		} else if (toClass.isEnum()) {
			return Enum.valueOf((Class<? extends Enum>) toClass, string.toUpperCase());
		} else if (toClass.isAssignableFrom(Class.class)) {
			try {
				return Context.loadClass(string);
			}
			catch (ClassNotFoundException e) {
				throw new ConversionException("Could not convert from " + source.getClass() + " to " + toType, e);
			}
		}

		// look for a static valueOf(String) method (e.g. Double, Integer, Boolean)
		try {
			Method method = toClass.getMethod("valueOf", String.class);
			if (Modifier.isStatic(method.getModifiers()) && toClass.isAssignableFrom(method.getReturnType())) {
				return method.invoke(null, string);
			}
		}
		catch (Exception ex) {
			// no usable valueOf(String) for this type - fall through to the fail-fast exception below
			// instead of silently swallowing the cause
			log.debug("No usable valueOf(String) on " + toClass + " for value '" + string + "'", ex);
		}

		throw new ConversionException("Don't know how to convert from " + source.getClass() + " to " + toType, null);
	}
}
