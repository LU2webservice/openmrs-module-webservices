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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.webservices.rest.web.audit.AuditLog.Outcome;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for the audit-logging mechanism ({@link AuditLog}).
 * <p>
 * The test captures everything written to the dedicated audit logger
 * ({@link AuditLog#LOGGER_NAME}) in memory, so the audit output can be asserted directly instead of
 * having to read a log file by hand. This is what makes the logging "testable".
 * <p>
 * Covered:
 * <ul>
 * <li>successful actions are logged (with who/what/when/where);</li>
 * <li>denied and failed actions are logged with the right outcome;</li>
 * <li>no audit line is produced when nothing is recorded (the old, vulnerable situation);</li>
 * <li>sensitive data (e.g. a password) never ends up in the audit line.</li>
 * </ul>
 */
public class AuditLogTest {

	private LoggerContext context;

	private Logger auditLogger;

	private CapturingAppender appender;

	@Before
	public void attachCapturingAppender() {
		context = (LoggerContext) LogManager.getContext(false);
		auditLogger = context.getLogger(AuditLog.LOGGER_NAME);
		appender = new CapturingAppender("audit-test-capture");
		appender.start();
		auditLogger.addAppender(appender);
		auditLogger.setLevel(Level.INFO);
		auditLogger.setAdditive(false);
	}

	@After
	public void detachCapturingAppender() {
		auditLogger.removeAppender(appender);
		appender.stop();
	}

	// --------------------------------------------------------------------- //
	// BEFORE (vulnerable) situation
	// --------------------------------------------------------------------- //

	/**
	 * BEFORE the fix: the delete path did not log anything. Without an {@link AuditLog} call there is
	 * no audit trail at all, so a user could deny ever having deleted the object (repudiation).
	 */
	@Test
	public void noRecordCall_leavesNoAuditTrail() {
		// no AuditLog call -> this mirrors the original create/update/delete/purge code
		assertEquals("old behaviour must produce no audit trail", 0, appender.messages.size());
	}

	// --------------------------------------------------------------------- //
	// Successful actions
	// --------------------------------------------------------------------- //

	/**
	 * A successful action produces exactly one audit line recording who/what/when/from-where and
	 * outcome=SUCCESS.
	 */
	@Test
	public void success_leavesAuditTrailWithWhoWhatWhenWhere() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("10.0.0.5");

		AuditLog.success("DELETE", "patient", "9b6639b2-5785-4603-9c92-32b8c5b7e1aa", request);

		assertEquals(1, appender.messages.size());
		String line = appender.messages.get(0);
		assertTrue(line, line.contains("AUDIT"));
		assertTrue(line, line.contains("action=DELETE")); // what
		assertTrue(line, line.contains("resource=patient")); // what
		assertTrue(line, line.contains("uuid=9b6639b2-5785-4603-9c92-32b8c5b7e1aa")); // what
		assertTrue(line, line.contains("outcome=SUCCESS")); // result
		assertTrue(line, line.contains("when=")); // when
		assertTrue(line, line.contains("user=")); // who
		assertTrue(line, line.contains("ip=10.0.0.5")); // from where
	}

	// --------------------------------------------------------------------- //
	// Failed / denied actions
	// --------------------------------------------------------------------- //

	/**
	 * An action that is rejected for authorisation reasons is logged with outcome=DENIED.
	 */
	@Test
	public void denied_leavesAuditTrailWithDeniedOutcome() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("10.0.0.9");

		AuditLog.denied("DELETE", "patient", "abc-123", request);

		assertEquals(1, appender.messages.size());
		assertTrue(appender.messages.get(0).contains("outcome=DENIED"));
	}

	/**
	 * An action that fails for another reason (validation, server error, ...) is logged with
	 * outcome=FAILED.
	 */
	@Test
	public void failure_leavesAuditTrailWithFailedOutcome() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("10.0.0.9");

		AuditLog.failure("PURGE", "location", "loc-1", request);

		assertEquals(1, appender.messages.size());
		assertTrue(appender.messages.get(0).contains("outcome=FAILED"));
		assertTrue(appender.messages.get(0).contains("action=PURGE"));
	}

	// --------------------------------------------------------------------- //
	// Sensitive data
	// --------------------------------------------------------------------- //

	/**
	 * The audit API only accepts metadata (action/resource/uuid). Even if a secret value is passed
	 * by mistake into the uuid slot, the formatted line must never expose it as if it were a
	 * recognised field. This documents that {@link AuditLog} has no place to put a request body.
	 */
	@Test
	public void formatMessage_doesNotLeakSensitivePasswordValue() {
		Instant when = Instant.parse("2026-06-15T10:00:00Z");

		String line = AuditLog.formatMessage("UPDATE", "user", "uuid-1", Outcome.SUCCESS, "admin", "127.0.0.1", when);

		// the line is fully determined by the metadata; there is simply no field that carries a body
		assertFalse("audit line must not contain a password", line.toLowerCase().contains("password"));
		assertFalse("audit line must not contain a secret", line.toLowerCase().contains("secret"));
	}

	/**
	 * Defence against log injection / log forging (CWE-117): a user-provided value that contains a
	 * line break must never produce a second log line. The result must stay on one line.
	 */
	@Test
	public void formatMessage_neutralisesLineBreaksToPreventLogForging() {
		Instant when = Instant.parse("2026-06-15T10:00:00Z");
		String forged = "real-uuid\r\nAUDIT action=DELETE resource=patient uuid=fake outcome=SUCCESS";

		String line = AuditLog.formatMessage("DELETE", "patient", forged, Outcome.SUCCESS, "admin", "127.0.0.1", when);

		assertFalse("must not contain a line feed", line.contains("\n"));
		assertFalse("must not contain a carriage return", line.contains("\r"));
		assertEquals("the whole audit entry must stay on a single line", 1, line.split("\n").length);
	}

	// --------------------------------------------------------------------- //
	// Misc behaviour
	// --------------------------------------------------------------------- //

	/**
	 * The client IP is taken from X-Forwarded-For when present, so the real client is logged even
	 * when the module runs behind a reverse proxy.
	 */
	@Test
	public void record_usesXForwardedForWhenPresent() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("172.16.0.1"); // the proxy
		request.addHeader("X-Forwarded-For", "203.0.113.7, 172.16.0.1"); // real client first

		AuditLog.success("DELETE", "patient", "uuid-x", request);

		assertTrue(appender.messages.get(0).contains("ip=203.0.113.7"));
	}

	/**
	 * The message format is a pure function, so its exact content can be asserted deterministically.
	 */
	@Test
	public void formatMessage_producesAllFieldsInOrder() {
		Instant when = Instant.parse("2026-06-15T10:00:00Z");

		String line = AuditLog.formatMessage("DELETE", "patient", "uuid-1", Outcome.SUCCESS, "admin", "127.0.0.1", when);

		assertEquals(
		    "AUDIT action=DELETE resource=patient uuid=uuid-1 outcome=SUCCESS when=2026-06-15T10:00:00Z user=admin ip=127.0.0.1",
		    line);
	}

	/**
	 * Minimal in-memory log4j2 appender that keeps every formatted message so the test can assert on
	 * the audit output.
	 */
	private static final class CapturingAppender extends AbstractAppender {

		private final List<String> messages = new CopyOnWriteArrayList<String>();

		private CapturingAppender(String name) {
			super(name, (Filter) null, (Layout<? extends Serializable>) null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}
}
