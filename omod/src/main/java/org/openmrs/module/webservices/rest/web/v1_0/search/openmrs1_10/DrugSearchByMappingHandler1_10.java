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
import java.util.Collections;
import java.util.List;

import org.openmrs.ConceptMapType;
import org.openmrs.ConceptSource;
import org.openmrs.Drug;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.springframework.stereotype.Component;

/**
 * Allows finding a drug by mapping. The shared request parsing lives in
 * {@link BaseDrugSearchByMappingHandler1_10}; this handler only adds the single-drug lookup.
 */
@Component
public class DrugSearchByMappingHandler1_10 extends BaseDrugSearchByMappingHandler1_10 {

	SearchQuery searchQuery = new SearchQuery.Builder(
	        "Allows you to find a drug by source, code and preferred map types(comma delimited). "
	                + "Gets the best matching drug, i.e. matching the earliest ConceptMapType passed if there are "
	                + "multiple matches for the highest-priority ConceptMapType")
	        .withRequiredParameters(REQUEST_PARAM_SOURCE)
	        .withOptionalParameters(REQUEST_PARAM_CODE, REQUEST_PARAM_MAP_TYPES).build();

	private final SearchConfig searchConfig = new SearchConfig("getDrugByMapping", RestConstants.VERSION_1 + "/drug",
	        Collections.singletonList("1.10.* - 9.*"), searchQuery);

	/**
	 * @see SearchHandler#getSearchConfig()
	 */
	@Override
	public SearchConfig getSearchConfig() {
		return searchConfig;
	}

	/**
	 * @see BaseDrugSearchByMappingHandler1_10#findDrugs(String, ConceptSource, List, RequestContext)
	 */
	@Override
	protected PageableResult findDrugs(String code, ConceptSource source,
	        List<ConceptMapType> mapTypesInOrderOfPreference, RequestContext context) {
		Drug drug = conceptService.getDrugByMapping(code, source, mapTypesInOrderOfPreference);
		if (drug == null) {
			return new EmptySearchResult();
		}

		List<Drug> drugs = new ArrayList<Drug>();
		drugs.add(drug);
		return new NeedsPaging<Drug>(drugs, context);
	}
}
