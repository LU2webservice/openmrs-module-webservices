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
 * Numbers with a decimal are always assumed to be {@link Double}, so this strategy narrows a
 * {@code Double} source to a {@link Float} target when that is what was asked for.
 */
public class DoubleToFloatConverter implements TypeConverter {

	@Override
	public boolean canConvert(Object source, Class<?> toClass, Type toType) {
		return toClass.isAssignableFrom(Float.class) && source instanceof Double;
	}

	@Override
	public Object convert(Object source, Class<?> toClass, Type toType) throws ConversionException {
		return new Float((Double) source);
	}
}
