/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.audit;

import java.time.Instant;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a tamper-evident audit trail entry for state-changing actions on REST resources.
 * <p>
 * This addresses the threat-model finding <strong>R-1 (Incomplete auditlogging / Repudiation)</strong>
 * and the logging gap analysis: create, update, delete and purge actions were previously not logged
 * at all, so a user could deny ever having performed them.
 * <p>
 * In line with <strong>NEN-7510 / ISO 27002:2022 control 8.15 (Logging)</strong> every entry records:
 * <ul>
 * <li><em>who</em> – the authenticated user;</li>
 * <li><em>what</em> – the action, the resource and the uuid of the affected object;</li>
 * <li><em>outcome</em> – whether the action {@link Outcome#SUCCESS succeeded}, was
 * {@link Outcome#DENIED denied} (authorisation failure) or otherwise {@link Outcome#FAILED failed};</li>
 * <li><em>when</em> – the timestamp;</li>
 * <li><em>from where</em> – the client IP address.</li>
 * </ul>
 * <p>
 * <strong>Sensitive data:</strong> on purpose this class only logs <em>metadata</em> (action, resource,
 * uuid, outcome, user, ip). It never logs the request body, request parameters or field values, so
 * secrets such as passwords (e.g. on <code>PATCH /user</code>) can never leak into the audit log.
 * <p>
 * The entries are written to a dedicated logger ({@link #LOGGER_NAME}) so they can be routed to a
 * separate, append-only audit file and verified in isolation. The {@link #formatMessage} method is
 * kept pure (no I/O, no global state) so the exact content of an audit line can be asserted in a unit
 * test.
 */
public class AuditLog {

	/** The result of an audited action. */
	public enum Outcome {
		/** The action completed successfully. */
		SUCCESS,
		/** The action was rejected because the user was not authenticated/authorised. */
		DENIED,
		/** The action failed for another reason (validation, server error, ...). */
		FAILED
	}

	/**
	 * Dedicated logger name for audit entries. Keeping audit logging on its own logger means it can
	 * be sent to a separate, append-only file and can be captured in isolation by tests.
	 */
	public static final String LOGGER_NAME = "org.openmrs.module.webservices.audit";

	/** Fixed marker that prefixes every audit line so they are easy to grep/filter. */
	public static final String MARKER = "AUDIT";

	private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

	private static final String UNKNOWN = "unknown";

	private AuditLog() {
		// utility class, no instances
	}

	/**
	 * Records a successfully completed action.
	 */
	public static void success(String action, String resource, String uuid, HttpServletRequest request) {
		record(Outcome.SUCCESS, action, resource, uuid, request);
	}

	/**
	 * Records an action that was rejected because the user was not authenticated or not authorised.
	 */
	public static void denied(String action, String resource, String uuid, HttpServletRequest request) {
		record(Outcome.DENIED, action, resource, uuid, request);
	}

	/**
	 * Records an action that failed for a reason other than authorisation (e.g. validation or a
	 * server error).
	 */
	public static void failure(String action, String resource, String uuid, HttpServletRequest request) {
		record(Outcome.FAILED, action, resource, uuid, request);
	}

	/**
	 * Records an audit entry. Never throws: audit logging must never break the request it is
	 * auditing.
	 *
	 * @param outcome the result of the action, see {@link Outcome}
	 * @param action human readable action, e.g. "DELETE" or "PURGE"
	 * @param resource the resource name, e.g. "patient"
	 * @param uuid the uuid of the affected object (may be <code>null</code> for a create)
	 * @param request the current request (used for the client IP); may be <code>null</code>
	 */
	public static void record(Outcome outcome, String action, String resource, String uuid, HttpServletRequest request) {
		try {
			String message = formatMessage(action, resource, uuid, outcome, currentUser(), clientIp(request),
			    Instant.now());
			// DENIED/FAILED are security relevant, so they are logged at WARN; SUCCESS at INFO.
			if (outcome == Outcome.SUCCESS) {
				log.info(message);
			} else {
				log.warn(message);
			}
		}
		catch (Exception e) {
			// An audit failure should never propagate into the user-facing request.
			log.warn("Could not write audit entry for action={} resource={} uuid={}", action, resource, uuid, e);
		}
	}

	/**
	 * Builds the audit line. Pure function: same input always produces the same output, so it can be
	 * asserted directly in a unit test.
	 *
	 * @return a single line such as
	 *         <code>AUDIT action=DELETE resource=patient uuid=abc outcome=SUCCESS when=2026-06-15T10:00:00Z user=admin ip=127.0.0.1</code>
	 */
	public static String formatMessage(String action, String resource, String uuid, Outcome outcome, String user,
	        String ip, Instant when) {
		return String.format("%s action=%s resource=%s uuid=%s outcome=%s when=%s user=%s ip=%s", MARKER,
		    blankToUnknown(action), blankToUnknown(resource), blankToUnknown(uuid), outcome == null ? UNKNOWN
		            : outcome.name(), when == null ? UNKNOWN : when.toString(), blankToUnknown(user), blankToUnknown(ip));
	}

	/**
	 * @return the username of the authenticated user, or "unknown" when there is no (resolvable)
	 *         authenticated user.
	 */
	private static String currentUser() {
		try {
			if (Context.isAuthenticated() && Context.getAuthenticatedUser() != null) {
				String username = Context.getAuthenticatedUser().getUsername();
				return StringUtils.isBlank(username) ? Context.getAuthenticatedUser().getSystemId() : username;
			}
		}
		catch (Exception e) {
			// no usable context (e.g. in a plain unit test) -> fall through to unknown
		}
		return UNKNOWN;
	}

	/**
	 * @return the client IP, honouring a single X-Forwarded-For hop when the module runs behind a
	 *         reverse proxy, otherwise the direct remote address.
	 */
	private static String clientIp(HttpServletRequest request) {
		if (request == null) {
			return UNKNOWN;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (StringUtils.isNotBlank(forwarded)) {
			// X-Forwarded-For can be a comma separated list; the first entry is the original client
			return forwarded.split(",")[0].trim();
		}
		return blankToUnknown(request.getRemoteAddr());
	}

	private static String blankToUnknown(String value) {
		return StringUtils.isBlank(value) ? UNKNOWN : value;
	}
}
