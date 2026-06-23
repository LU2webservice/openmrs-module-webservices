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
import java.util.Map;

import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.response.ConversionException;

/**
 * Converts a {@link Map} source (typically a SimpleObject submitted as JSON) into the target type by
 * delegating to {@link ConversionUtil#convertMap(Map, Class)}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MapConverter implements TypeConverter {

	@Override
	public boolean canConvert(Object source, Class<?> toClass, Type toType) {
		return source instanceof Map;
	}

	@Override
	public Object convert(Object source, Class<?> toClass, Type toType) throws ConversionException {
		return ConversionUtil.convertMap((Map<String, ?>) source, toClass);
	}
}
