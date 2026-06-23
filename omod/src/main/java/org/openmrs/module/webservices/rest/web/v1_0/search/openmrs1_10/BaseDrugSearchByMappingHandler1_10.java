/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.search.openmrs1_10;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.api.BaseSearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Shared base for the two "find drug(s) by mapping" search handlers
 * ({@link DrugSearchByMappingHandler1_10} and {@link DrugsSearchByMappingHandler1_10}). It pulls up
 * the request-parameter parsing and the source/map-type resolution that the two handlers used to
 * duplicate (~41 identical lines), leaving each handler responsible only for the actual lookup
 * (Template Method + Extract Superclass, DRY).
 */
public abstract class BaseDrugSearchByMappingHandler1_10 extends BaseSearchHandler {

	public static final String REQUEST_PARAM_CODE = "code";

	public static final String REQUEST_PARAM_SOURCE = "source";

	public static final String REQUEST_PARAM_MAP_TYPES = "preferredMapTypes";

	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;

	/**
	 * Reads {@code source}/{@code code}/{@code preferredMapTypes}, resolves the concept source and map
	 * types (failing fast with {@link ObjectNotFoundException} on unknown uuids) and then delegates the
	 * version-specific lookup to {@link #findDrugs(String, ConceptSource, List, RequestContext)}.
	 *
	 * @see BaseSearchHandler#performSearch(RequestContext)
	 */
	@Override
	protected PageableResult performSearch(RequestContext context) throws ResponseException {
		String code = context.getParameter(REQUEST_PARAM_CODE);
		String sourceUuid = context.getParameter(REQUEST_PARAM_SOURCE);
		String mapTypesUuids = context.getParameter(REQUEST_PARAM_MAP_TYPES);

		ConceptSource source = null;
		if (StringUtils.isNotBlank(sourceUuid)) {
			source = conceptService.getConceptSourceByUuid(sourceUuid);
			if (source == null) {
				throw new ObjectNotFoundException();
			}
		}

		List<ConceptMapType> mapTypesInOrderOfPreference = null;
		if (StringUtils.isNotBlank(mapTypesUuids)) {
			String[] uuids = StringUtils.split(mapTypesUuids, ",");
			for (String uuid : uuids) {
				ConceptMapType mapType = conceptService.getConceptMapTypeByUuid(uuid.trim());
				if (mapType == null) {
					throw new ObjectNotFoundException();
				}
				if (mapTypesInOrderOfPreference == null) {
					mapTypesInOrderOfPreference = new ArrayList<ConceptMapType>();
				}
				mapTypesInOrderOfPreference.add(mapType);
			}
		}

		return findDrugs(code, source, mapTypesInOrderOfPreference, context);
	}

	/**
	 * The variable step: perform the actual drug lookup with the already-resolved arguments.
	 *
	 * @param code the (optional) mapping code
	 * @param source the resolved concept source (may be {@code null})
	 * @param mapTypesInOrderOfPreference the resolved map types (may be {@code null})
	 * @param context the request context (for paging / includeAll)
	 * @return the search result
	 */
	protected abstract PageableResult findDrugs(String code, ConceptSource source,
	        List<ConceptMapType> mapTypesInOrderOfPreference, RequestContext context);
}
