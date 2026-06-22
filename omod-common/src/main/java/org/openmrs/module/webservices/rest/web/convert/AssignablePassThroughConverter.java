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
 * When the source value is already (an instance of) the target type, it is returned unchanged.
 */
public class AssignablePassThroughConverter implements TypeConverter {

	@Override
	public boolean canConvert(Object source, Class<?> toClass, Type toType) {
		return toClass.isAssignableFrom(source.getClass());
	}

	@Override
	public Object convert(Object source, Class<?> toClass, Type toType) throws ConversionException {
		return source;
	}
}
