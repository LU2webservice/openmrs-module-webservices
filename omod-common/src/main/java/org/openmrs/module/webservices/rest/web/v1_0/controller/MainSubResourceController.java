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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.audit.AuditLog;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.SubResource;
import org.openmrs.module.webservices.rest.web.resource.api.SubResourceSearchHandler;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Enumeration;

/**
 * Base controller that handles exceptions (via {@link BaseRestController}) and also standard CRUD
 * operations based on a {@link SubResource}.
 * 
 * @param <R>
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1)
public class MainSubResourceController extends BaseRestController {
	
	@Autowired
	RestService restService;
	
	@Autowired
	BaseUriSetup baseUriSetup;
	
	/**
	 * @param parentUuid
	 * @param uuid
	 * @param request
	 * @return
	 * @throws ResponseException
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}/{uuid}", method = RequestMethod.GET)
	@ResponseBody
	public Object retrieve(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, @PathVariable("uuid") String uuid, HttpServletRequest request,
	        HttpServletResponse response) throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		return res.retrieve(parentUuid, uuid, context);
	}
	
	/**
	 * @param parentUuid
	 * @param request
	 * @param response
	 * @return
	 * @throws ResponseException
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}", method = RequestMethod.GET)
	@ResponseBody
	public SimpleObject get(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, HttpServletRequest request, HttpServletResponse response)
	        throws ResponseException {
		baseUriSetup.setup(request);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		RequestContext context = RestUtil.getRequestContext(request, response);
		Converter conv = res instanceof Converter ? (Converter) res : null;
		
		@SuppressWarnings("unchecked")
		SubResourceSearchHandler searchHandler = (SubResourceSearchHandler) restService.getSearchHandler(
		    buildResourceName(resource) + "/" + subResource, request.getParameterMap());
		if (searchHandler != null) {
			return searchHandler.search(parentUuid, context).toSimpleObject(conv);
		}
		
		Enumeration parameters = request.getParameterNames();
		while (parameters.hasMoreElements()) {
			if (!RestConstants.SPECIAL_REQUEST_PARAMETERS.contains(parameters.nextElement())) {
				if (res instanceof Searchable) {
					return ((Searchable) res).search(context);
				} else {
					throw new ResourceDoesNotSupportOperationException(res.getClass().getSimpleName() + " is not searchable");
				}
			}
		}
		
		return res.getAll(parentUuid, context);
	}
	
	/**
	 * @param parentUuid
	 * @param post
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}", method = RequestMethod.POST)
	@ResponseBody
	public Object create(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, @RequestBody SimpleObject post, HttpServletRequest request,
	        HttpServletResponse response) throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		Object created;
		try {
			created = res.create(parentUuid, post, context);
		}
		catch (RuntimeException e) {
			// only metadata is logged, never the posted body, so secrets cannot leak
			auditFailure("CREATE", resource + "/" + subResource, parentUuid, request, e);
			throw e;
		}
		AuditLog.success("CREATE", resource + "/" + subResource, parentUuid, request);
		return RestUtil.created(response, created);
	}
	
	/**
	 * @param parentUuid
	 * @param uuid
	 * @param post
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}/{uuid}", method = RequestMethod.POST)
	@ResponseBody
	public Object update(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, @PathVariable("uuid") String uuid,
	        @RequestBody SimpleObject post, HttpServletRequest request, HttpServletResponse response)
	        throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		Object updated;
		try {
			updated = res.update(parentUuid, uuid, post, context);
		}
		catch (RuntimeException e) {
			// only the uuid is logged, never the posted body, so secrets (e.g. passwords) cannot leak
			auditFailure("UPDATE", resource + "/" + subResource, uuid, request, e);
			throw e;
		}
		AuditLog.success("UPDATE", resource + "/" + subResource, uuid, request);
		return RestUtil.updated(response, updated);
	}
	
	/**
	 * @param parentUuid
	 * @param uuid
	 * @param reason
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}/{uuid}", method = RequestMethod.DELETE, params = "!purge")
	@ResponseBody
	public Object delete(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, @PathVariable("uuid") String uuid,
	        @RequestParam(value = "reason", defaultValue = "web service call") String reason, HttpServletRequest request,
	        HttpServletResponse response) throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		try {
			res.delete(parentUuid, uuid, reason, context);
		}
		catch (RuntimeException e) {
			auditFailure("DELETE", resource + "/" + subResource, uuid, request, e);
			throw e;
		}
		// R-1: record who deleted what, when and from where so the action cannot be denied
		AuditLog.success("DELETE", resource + "/" + subResource, uuid, request);
		return RestUtil.noContent(response);
	}
	
	/**
	 * @param parentUuid
	 * @param uuid
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}/{uuid}", method = RequestMethod.DELETE, params = "purge")
	@ResponseBody
	public Object purge(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, @PathVariable("uuid") String uuid, HttpServletRequest request,
	        HttpServletResponse response) throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		try {
			res.purge(parentUuid, uuid, context);
		}
		catch (RuntimeException e) {
			auditFailure("PURGE", resource + "/" + subResource, uuid, request, e);
			throw e;
		}
		// R-1: PURGE permanently removes data; record it so the action cannot be denied
		AuditLog.success("PURGE", resource + "/" + subResource, uuid, request);
		return RestUtil.noContent(response);
	}
	
	/**
	 * @param parentUuid
	 * @param reason
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}", method = RequestMethod.DELETE, params = "!purge")
	@ResponseBody
	public Object delete(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource,
	        @RequestParam(value = "reason", defaultValue = "web service call") String reason, HttpServletRequest request,
	        HttpServletResponse response) throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		try {
			res.delete(parentUuid, null, reason, context);
		}
		catch (RuntimeException e) {
			auditFailure("DELETE", resource + "/" + subResource, parentUuid, request, e);
			throw e;
		}
		// R-1: record who deleted what, when and from where so the action cannot be denied
		AuditLog.success("DELETE", resource + "/" + subResource, parentUuid, request);
		return RestUtil.noContent(response);
	}
	
	/**
	 * @param parentUuid
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}", method = RequestMethod.DELETE, params = "purge")
	@ResponseBody
	public Object purge(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, HttpServletRequest request, HttpServletResponse response)
	        throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		try {
			res.purge(parentUuid, null, context);
		}
		catch (RuntimeException e) {
			auditFailure("PURGE", resource + "/" + subResource, parentUuid, request, e);
			throw e;
		}
		// R-1: PURGE permanently removes data; record it so the action cannot be denied
		AuditLog.success("PURGE", resource + "/" + subResource, parentUuid, request);
		return RestUtil.noContent(response);
	}
	
	/**
	 * @param parentUuid
	 * @param post
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{resource}/{parentUuid}/{subResource}", method = RequestMethod.PUT)
	@ResponseBody
	public Object put(@PathVariable("resource") String resource, @PathVariable("parentUuid") String parentUuid,
	        @PathVariable("subResource") String subResource, @RequestBody SimpleObject post, HttpServletRequest request,
	        HttpServletResponse response) throws ResponseException {
		baseUriSetup.setup(request);
		RequestContext context = RestUtil.getRequestContext(request, response);
		SubResource res = (SubResource) restService.getResourceByName(buildResourceName(resource) + "/" + subResource);
		try {
			res.put(parentUuid, post, context);
		}
		catch (RuntimeException e) {
			// only metadata is logged, never the posted body, so secrets cannot leak
			auditFailure("PUT", resource + "/" + subResource, parentUuid, request, e);
			throw e;
		}
		AuditLog.success("PUT", resource + "/" + subResource, parentUuid, request);
		return RestUtil.noContent(response);
	}

	/**
	 * Writes an audit entry for a failed action, distinguishing an authorisation failure (DENIED)
	 * from any other failure (FAILED). Mirrors the behaviour of {@link MainResourceController} so
	 * sub-resource endpoints are audited the same way as top-level resource endpoints.
	 */
	private void auditFailure(String action, String resource, String uuid, HttpServletRequest request, RuntimeException e) {
		if (e instanceof APIAuthenticationException || RestUtil.hasCause(e, APIAuthenticationException.class)) {
			AuditLog.denied(action, resource, uuid, request);
		} else {
			AuditLog.failure(action, resource, uuid, request);
		}
	}
}
