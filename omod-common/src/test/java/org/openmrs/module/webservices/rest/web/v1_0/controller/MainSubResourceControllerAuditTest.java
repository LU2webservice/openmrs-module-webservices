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
import org.openmrs.module.webservices.rest.web.resource.api.SubResource;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests that {@link MainSubResourceController} writes an audit entry for every state-changing action
 * on a <em>sub-resource</em> endpoint (e.g. {@code POST /patient/{uuid}/identifier}), for both
 * successful and failed/denied attempts, without leaking sensitive data.
 * <p>
 * This is the sub-resource counterpart of {@link MainResourceControllerAuditTest}. Sub-resources are
 * a separate set of REST endpoints handled by a different controller, so they need their own audit
 * coverage; before this test the create/update/put paths of the sub-resource controller were not
 * logged at all.
 * <p>
 * The controller is exercised directly with mocked resources (no database), and the audit output is
 * captured in memory so the exact audit line can be asserted.
 */
public class MainSubResourceControllerAuditTest extends BaseModuleWebContextSensitiveTest {

	private static final String PASSWORD = "S3cr3t-Passw0rd!";

	private MainSubResourceController controller;

	private RestService restService;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private LoggerContext context;

	private Logger auditLogger;

	private CapturingAppender appender;

	@Before
	public void setUp() {
		restService = mock(RestService.class);

		controller = new MainSubResourceController();
		controller.restService = restService;
		controller.baseUriSetup = mock(BaseUriSetup.class);

		request = new MockHttpServletRequest();
		request.setRemoteAddr("192.168.1.50");
		response = new MockHttpServletResponse();

		context = (LoggerContext) LogManager.getContext(false);
		auditLogger = context.getLogger(AuditLog.LOGGER_NAME);
		appender = new CapturingAppender("subresource-audit-capture");
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
	// Successful actions (one per state-changing endpoint)
	// --------------------------------------------------------------------- //

	@Test
	public void create_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		when(resource.create(anyString(), any(SimpleObject.class), any())).thenReturn(new SimpleObject());

		controller.create("patient", "parent-uuid", "identifier", new SimpleObject(), request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=CREATE"));
		assertTrue(line, line.contains("resource=patient/identifier"));
		assertTrue(line, line.contains("uuid=parent-uuid"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
		assertTrue(line, line.contains("ip=192.168.1.50"));
	}

	@Test
	public void update_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		when(resource.update(anyString(), anyString(), any(SimpleObject.class), any())).thenReturn(new SimpleObject());

		controller.update("patient", "parent-uuid", "identifier", "child-uuid", new SimpleObject(), request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=UPDATE"));
		assertTrue(line, line.contains("resource=patient/identifier"));
		assertTrue(line, line.contains("uuid=child-uuid"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	@Test
	public void delete_withUuid_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).delete(anyString(), anyString(), anyString(), any());

		controller.delete("patient", "parent-uuid", "identifier", "child-uuid", "web service call", request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=DELETE"));
		assertTrue(line, line.contains("resource=patient/identifier"));
		assertTrue(line, line.contains("uuid=child-uuid"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	@Test
	public void purge_withUuid_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).purge(anyString(), anyString(), any());

		controller.purge("patient", "parent-uuid", "identifier", "child-uuid", request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PURGE"));
		assertTrue(line, line.contains("uuid=child-uuid"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	@Test
	public void delete_withoutUuid_whenSuccessful_logsParentAsAffectedObject() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).delete(anyString(), any(), anyString(), any());

		controller.delete("patient", "parent-uuid", "identifier", "web service call", request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=DELETE"));
		// when no child uuid is given, the parent uuid is the identifiable affected object
		assertTrue(line, line.contains("uuid=parent-uuid"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	@Test
	public void purge_withoutUuid_whenSuccessful_logsParentAsAffectedObject() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).purge(anyString(), any(), any());

		controller.purge("patient", "parent-uuid", "identifier", request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PURGE"));
		assertTrue(line, line.contains("uuid=parent-uuid"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	@Test
	public void put_whenSuccessful_writesSuccessAuditEntry() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doNothing().when(resource).put(anyString(), any(SimpleObject.class), any());

		controller.put("patient", "parent-uuid", "identifier", new SimpleObject(), request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PUT"));
		assertTrue(line, line.contains("resource=patient/identifier"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
	}

	// --------------------------------------------------------------------- //
	// Denied / failed actions
	// --------------------------------------------------------------------- //

	@Test
	public void create_whenNotAuthorised_writesDeniedAuditEntryAndRethrows() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new APIAuthenticationException("not allowed")).when(resource).create(anyString(),
		    any(SimpleObject.class), any());

		try {
			controller.create("patient", "parent-uuid", "identifier", new SimpleObject(), request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (APIAuthenticationException expected) {
			// expected: the exception still bubbles up to the normal error handler
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=CREATE"));
		assertTrue(line, line.contains("outcome=DENIED"));
	}

	@Test
	public void update_whenNotAuthorised_writesDeniedAuditEntryAndRethrows() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new APIAuthenticationException("not allowed")).when(resource).update(anyString(), anyString(),
		    any(SimpleObject.class), any());

		try {
			controller.update("patient", "parent-uuid", "identifier", "child-uuid", new SimpleObject(), request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (APIAuthenticationException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=UPDATE"));
		assertTrue(line, line.contains("uuid=child-uuid"));
		assertTrue(line, line.contains("outcome=DENIED"));
	}

	@Test
	public void delete_whenNotAuthorised_writesDeniedAuditEntryAndRethrows() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new APIAuthenticationException("not allowed")).when(resource).delete(anyString(), anyString(),
		    anyString(), any());

		try {
			controller.delete("patient", "parent-uuid", "identifier", "child-uuid", "web service call", request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (APIAuthenticationException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=DELETE"));
		assertTrue(line, line.contains("outcome=DENIED"));
	}

	@Test
	public void purge_whenServerError_writesFailedAuditEntryAndRethrows() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new IllegalStateException("constraint violation")).when(resource).purge(anyString(), anyString(), any());

		try {
			controller.purge("patient", "parent-uuid", "identifier", "child-uuid", request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (IllegalStateException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PURGE"));
		assertTrue(line, line.contains("outcome=FAILED"));
	}

	@Test
	public void put_whenServerError_writesFailedAuditEntryAndRethrows() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		doThrow(new IllegalStateException("boom")).when(resource).put(anyString(), any(SimpleObject.class), any());

		try {
			controller.put("patient", "parent-uuid", "identifier", new SimpleObject(), request, response);
			fail("the controller must rethrow the original exception");
		}
		catch (IllegalStateException expected) {
			// expected
		}

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=PUT"));
		assertTrue(line, line.contains("outcome=FAILED"));
	}

	// --------------------------------------------------------------------- //
	// Sensitive data
	// --------------------------------------------------------------------- //

	@Test
	public void create_withPasswordInBody_doesNotLogThePassword() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		when(resource.create(anyString(), any(SimpleObject.class), any())).thenReturn(new SimpleObject());

		SimpleObject body = new SimpleObject();
		body.add("username", "new-user");
		body.add("password", PASSWORD);

		controller.create("user", "parent-uuid", "credential", body, request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=CREATE"));
		assertTrue(line, line.contains("outcome=SUCCESS"));
		assertFalse("the audit line must not contain the password", line.contains(PASSWORD));
		assertFalse("the audit line must not contain the field name 'password'", line.contains("password"));
	}

	@Test
	public void update_withPasswordInBody_doesNotLogThePassword() throws Exception {
		SubResource resource = mock(SubResource.class);
		when(restService.getResourceByName(anyString())).thenReturn(resource);
		when(resource.update(anyString(), anyString(), any(SimpleObject.class), any())).thenReturn(new SimpleObject());

		SimpleObject body = new SimpleObject();
		body.add("username", "dr-house");
		body.add("password", PASSWORD);

		controller.update("user", "parent-uuid", "credential", "child-uuid", body, request, response);

		String line = onlyAuditLine();
		assertTrue(line, line.contains("action=UPDATE"));
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
