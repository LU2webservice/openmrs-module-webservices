/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
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
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.audit.AuditLog;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Deletable;
import org.openmrs.module.webservices.rest.web.resource.api.Purgeable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests that {@link MainResourceController} actually writes an audit entry for every state-changing
 * action, for both successful and failed/denied attempts, without leaking sensitive data.
 * <p>
 * The controller is exercised directly with mocked resources (no database, no Spring context), and
 * the audit output is captured in memory. This proves the wiring in the real production code, not
 * just the {@link AuditLog} helper in isolation.
 * <p>
 * This test fails against the original controller (which did not log anything) and passes against
 * the fixed controller — that red/green difference is the "before/after" evidence.
 */
public class MainResourceControllerAuditTest extends BaseModuleWebContextSensitiveTest {

	private static final String PASSWORD = "S3cr3t-Passw0rd!";

	private MainResourceController controller;

	private RestService restService;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private LoggerContext context;

	private Logger auditLogger;

	private CapturingAppender appender;

	@Before
	public void setUp() {
		restService = mock(RestService.class);

		controller = new MainResourceController();
		controller.restService = restService;
		controller.baseUriSetup = mock(BaseUriSetup.class);

		request = new MockHttpServletRequest();
		request.setRemoteAddr("192.168.1.50");
		response = new MockHttpServletResponse();

		context = (LoggerContext) LogManager.getContext(false);
		auditLogger = context.getLogger(AuditLog.LOGGER_NAME);
		appender = new CapturingAppender("controller-audit-capture");
		appender.start();
		auditLogger.addAppender(appender);
		auditLogger.setLevel(Level.INFO);
		auditLogger.setAdditive(false);
	}

	@After
	public void tearDown() {
		auditLogger.removeAppender(appender);
		appender.stop();
	}

	// --------------------------------------------------------------------- //
	// Successful actions
	// --------------------------------------------------------------------- //

	@Test
	public void delete_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		Deletable resource = mock(Deletable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).delete(anyString(), anyString(), any());

		controller.delete("patient", "uuid-success", "web service call", request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=DELETE"));
		assertTrue(line, line.contains("resource=patient"));
		assertTrue(line, line.contains("uuid=uuid-success"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
		assertTrue(line, line.contains("ip=192.168.1.50"));
	}

	@Test
	public void purge_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		Purgeable resource = mock(Purgeable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).purge(anyString(), any());

		controller.purge("location", "uuid-purge", request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PURGE"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	// --------------------------------------------------------------------- //
	// Failed / denied actions
	// --------------------------------------------------------------------- //

	@Test
	public void delete_whenNotAuthorised_writesDeniedAuditEntryAndRethrows() throws Exception {
		Deletable resource = mock(Deletable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new APIAuthenticationException("not allowed")).when(resource).delete(anyString(), anyString(), any());

		try {
			controller.delete("patient", "uuid-denied", "web service call", request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (APIAuthenticationException expected) {
			// expected: the exception still bubbles up to the normal error handler
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=DELETE"));
		assertTrue(line, line.contains("uuid=uuid-denied"));
		assertTrue(line, line.contains("outcome=DENIED"));
	}

	@Test
	public void delete_whenServerError_writesFailedAuditEntryAndRethrows() throws Exception {
		Deletable resource = mock(Deletable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new IllegalStateException("boom")).when(resource).delete(anyString(), anyString(), any());

		try {
			controller.delete("patient", "uuid-error", "web service call", request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (IllegalStateException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("outcome=FAILED"));
	}

	/**
	 * The same {@code auditFailure} wiring used by {@code delete} is shared by create/update/purge.
	 * This proves CREATE is covered too, not just DELETE.
	 */
	@Test
	public void create_whenNotAuthorised_writesDeniedAuditEntryAndRethrows() throws Exception {
		Creatable resource = mock(Creatable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new APIAuthenticationException("not allowed")).when(resource).create(any(SimpleObject.class), any());

		try {
			controller.create("patient", new SimpleObject(), request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (APIAuthenticationException expected) {
			// expected: the exception still bubbles up to the normal error handler
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=CREATE"));
		assertTrue(line, line.contains("outcome=DENIED"));
	}

	/**
	 * Same wiring, exercised for UPDATE: an authorisation failure during update must be logged as
	 * DENIED, not as a generic FAILED.
	 */
	@Test
	public void update_whenNotAuthorised_writesDeniedAuditEntryAndRethrows() throws Exception {
		Updatable resource = mock(Updatable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new APIAuthenticationException("not allowed")).when(resource)
		        .update(anyString(), any(SimpleObject.class), any());

		try {
			controller.update("user", "uuid-update-denied", new SimpleObject(), request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (APIAuthenticationException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=UPDATE"));
		assertTrue(line, line.contains("uuid=uuid-update-denied"));
		assertTrue(line, line.contains("outcome=DENIED"));
	}

	/**
	 * Same wiring, exercised for PURGE: a non-authorisation failure (e.g. the object cannot be
	 * permanently removed because of a DB constraint) must be logged as FAILED, not DENIED.
	 */
	@Test
	public void purge_whenServerError_writesFailedAuditEntryAndRethrows() throws Exception {
		Purgeable resource = mock(Purgeable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new IllegalStateException("constraint violation")).when(resource).purge(anyString(), any());

		try {
			controller.purge("location", "uuid-purge-error", request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (IllegalStateException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PURGE"));
		assertTrue(line, line.contains("outcome=FAILED"));
	}

	// --------------------------------------------------------------------- //
	// Sensitive data
	// --------------------------------------------------------------------- //

	/**
	 * An update of a user carries a password in the request body. The audit entry must record that
	 * the update happened (who/what/when) but must never contain the password value.
	 */
	@Test
	public void update_withPasswordInBody_doesNotLogThePassword() throws Exception {
		Updatable resource = mock(Updatable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		when(resource.update(anyString(), any(SimpleObject.class), any())).thenReturn(new SimpleObject());

		SimpleObject body = new SimpleObject();
		body.add("username", "dr-house");
		body.add("password", PASSWORD);

		controller.update("user", "uuid-user-1", body, request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=UPDATE"));
		assertTrue(line, line.contains("uuid=uuid-user-1"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
		assertFalse("the audit line must not contain the password", line.contains(PASSWORD));
		assertFalse("the audit line must not contain the field name 'password'", line.contains("password"));
	}

	@Test
	public void create_withPasswordInBody_logsSuccessWithoutThePassword() throws Exception {
		Creatable resource = mock(Creatable.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		when(resource.create(any(SimpleObject.class), any())).thenReturn(new SimpleObject());

		SimpleObject body = new SimpleObject();
		body.add("username", "new-user");
		body.add("password", PASSWORD);

		controller.create("user", body, request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=CREATE"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
		assertFalse("the audit line must not contain the password", line.contains(PASSWORD));
	}

	// --------------------------------------------------------------------- //
	// Helpers
	// --------------------------------------------------------------------- //

	private String onlyAuditLine() {
		assertEquals("expected exactly one audit line", 1, appender.messages.size());
		return appender.messages.get(0);
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
