/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Transport-security hardening for the /ws/rest endpoints (SR-1 / S-1).
 * <p>
 * TLS itself is terminated by the reverse proxy / servlet container, not by this module. This
 * filter adds the module-level defence-in-depth that <em>is</em> possible in code:
 * <ul>
 * <li>it sends an <strong>HSTS</strong> ({@code Strict-Transport-Security}) header on secure
 * requests, so a compliant browser will refuse to talk to the API over plain HTTP afterwards
 * (harmless over HTTP, where it is simply ignored);</li>
 * <li>it can <strong>enforce HTTPS</strong> when the global property
 * {@value #REQUIRE_HTTPS_GLOBAL_PROPERTY} is set to {@code true}: a plain-HTTP call is then
 * rejected with HTTP 403 instead of leaking Basic-Auth credentials over the wire.</li>
 * </ul>
 * The enforcement is <strong>off by default</strong> so local HTTP development and the Docker demo
 * keep working; production deployments are expected to enable it. A request is considered secure
 * when {@link HttpServletRequest#isSecure()} is true or the reverse proxy sets
 * {@code X-Forwarded-Proto: https}.
 */
public class TransportSecurityFilter implements Filter {

	protected final Log log = LogFactory.getLog(getClass());

	/** Global property (default {@code false}) that opts in to strict HTTPS enforcement. */
	public static final String REQUIRE_HTTPS_GLOBAL_PROPERTY = "webservices.rest.requireHttps";

	private static final String HSTS_HEADER = "Strict-Transport-Security";

	/** One year, and include sub domains. Only honoured by browsers over HTTPS. */
	private static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";

	private static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.debug("Initializing REST WS Transport-Security filter");
	}

	@Override
	public void destroy() {
		log.debug("Destroying REST WS Transport-Security filter");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
	        ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (isSecure(httpRequest)) {
			// Tell compliant browsers to stick to HTTPS from now on.
			httpResponse.setHeader(HSTS_HEADER, HSTS_VALUE);
		} else if (isHttpsRequired()) {
			// Plain HTTP while HTTPS is mandatory: refuse before any credential is processed.
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required for REST web service calls");
			return;
		}

		chain.doFilter(request, response);
	}

	/**
	 * @return true when the request reached us over TLS, either directly or via a reverse proxy
	 *         that terminated TLS and set {@code X-Forwarded-Proto: https}.
	 */
	private boolean isSecure(HttpServletRequest request) {
		if (request.isSecure()) {
			return true;
		}
		return "https".equalsIgnoreCase(request.getHeader(FORWARDED_PROTO_HEADER));
	}

	/**
	 * @return true when the administrator has opted in to strict HTTPS enforcement. Reads the
	 *         global property defensively: any problem resolving it falls back to {@code false} so
	 *         the filter can never lock out the API by accident.
	 */
	private boolean isHttpsRequired() {
		try {
			return Boolean.parseBoolean(Context.getAdministrationService().getGlobalProperty(
			    REQUIRE_HTTPS_GLOBAL_PROPERTY, "false"));
		}
		catch (Exception e) {
			log.debug("Could not read " + REQUIRE_HTTPS_GLOBAL_PROPERTY + ", defaulting to not enforced", e);
			return false;
		}
	}
}
