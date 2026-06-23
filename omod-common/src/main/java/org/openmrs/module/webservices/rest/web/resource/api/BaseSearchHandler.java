/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.resource.api;

import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * Template Method base class for {@link SearchHandler}s. It fixes the skeleton of a search -
 * validate the request, then perform the actual lookup - so individual handlers only fill in the
 * variable steps instead of repeating the surrounding boilerplate (DRY).
 * <p>
 * The {@link #search(RequestContext)} method is {@code final}: the order of the steps is enforced
 * here and cannot drift per handler. Subclasses override the hooks {@link #validate(RequestContext)}
 * (optional) and {@link #performSearch(RequestContext)} (required), and still provide their own
 * {@link #getSearchConfig()} from the {@link SearchHandler} interface.
 */
public abstract class BaseSearchHandler implements SearchHandler {

	/**
	 * The fixed search skeleton (Template Method). Do not override - override the hooks instead.
	 *
	 * @see SearchHandler#search(RequestContext)
	 */
	@Override
	public final PageableResult search(RequestContext context) throws ResponseException {
		validate(context);
		return performSearch(context);
	}

	/**
	 * Hook for request validation. The default does nothing; subclasses override it when they need to
	 * fail fast on invalid parameters before any lookup happens.
	 *
	 * @param context the request context
	 * @throws ResponseException if the request is invalid
	 */
	protected void validate(RequestContext context) throws ResponseException {
		// no validation by default
	}

	/**
	 * The variable step: the actual lookup for this handler.
	 *
	 * @param context the request context
	 * @return the search result
	 * @throws ResponseException if the search fails
	 */
	protected abstract PageableResult performSearch(RequestContext context) throws ResponseException;
}
