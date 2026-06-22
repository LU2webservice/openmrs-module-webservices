/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_10;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

// the framework requires we specify a supportedClass, even though this shouldn't have one
@Resource(name = RestConstants.VERSION_1 + "/orderentryconfig", supportedClass = OrderService.class, supportedOpenmrsVersions = {
        "1.10.* - 9.*" })
public class OrderEntryConfigResource1_10 implements Listable {

	private static final Log log = LogFactory.getLog(OrderEntryConfigResource1_10.class);

	@Override
	public SimpleObject getAll(RequestContext context) throws ResponseException {
		OrderService orderService = Context.getOrderService();

		SimpleObject ret = new SimpleObject();
		// each entry is optional - if one of these is not configured we skip it (and log why) rather
		// than letting the whole call fail
		putIfConfigured(ret, "drugRoutes", orderService::getDrugRoutes, context);
		putIfConfigured(ret, "drugDosingUnits", orderService::getDrugDosingUnits, context);
		putIfConfigured(ret, "drugDispensingUnits", orderService::getDrugDispensingUnits, context);
		putIfConfigured(ret, "durationUnits", orderService::getDurationUnits, context);
		putIfConfigured(ret, "testSpecimenSources", orderService::getTestSpecimenSources, context);
		putIfConfigured(ret, "orderFrequencies", () -> orderService.getOrderFrequencies(false), context);
		return ret;
	}

	/**
	 * Adds {@code key} to {@code ret} with the converted value from {@code valueSupplier}. If the value
	 * cannot be obtained or converted (e.g. the concept is not configured), the entry is skipped and the
	 * cause is logged instead of being silently swallowed.
	 */
	private void putIfConfigured(SimpleObject ret, String key, java.util.function.Supplier<Object> valueSupplier,
	        RequestContext context) {
		try {
			ret.put(key, ConversionUtil.convertToRepresentation(valueSupplier.get(), context.getRepresentation()));
		}
		catch (Exception ex) {
			log.debug("Order-entry config '" + key + "' is not configured, skipping", ex);
		}
	}
	
	@Override
	public String getUri(Object instance) {
		return RestConstants.URI_PREFIX + "/orderentryconfig";
	}
}
