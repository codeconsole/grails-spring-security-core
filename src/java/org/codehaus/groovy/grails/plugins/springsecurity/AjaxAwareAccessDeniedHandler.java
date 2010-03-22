/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.springsecurity;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.PortResolver;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.util.Assert;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class AjaxAwareAccessDeniedHandler implements AccessDeniedHandler, InitializingBean {

	private String errorPage;
	private String ajaxErrorPage;
	private String ajaxHeader = AjaxAwareAuthenticationEntryPoint.AJAX_HEADER;
	private PortResolver portResolver;
	private AuthenticationTrustResolver authenticationTrustResolver;

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.web.access.AccessDeniedHandler#handle(
	 * 	javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * 	org.springframework.security.access.AccessDeniedException)
	 */
	public void handle(final HttpServletRequest request, final HttpServletResponse response,
			final AccessDeniedException e) throws IOException, ServletException {

		if (e != null && isLoggedIn() && authenticationTrustResolver.isRememberMe(getAuthentication())) {
			// user has a cookie but is getting bounced because of IS_AUTHENTICATED_FULLY,
			// so Acegi won't save the original request
			request.getSession().setAttribute(
					DefaultSavedRequest.SPRING_SECURITY_SAVED_REQUEST_KEY,
					new DefaultSavedRequest(request, portResolver));
		}

		if (response.isCommitted()) {
			return;
		}

		boolean ajaxError = ajaxErrorPage != null && request.getHeader(ajaxHeader) != null;
		if (errorPage == null && !ajaxError) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
			return;
		}

		boolean includePort = true;
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = portResolver.getServerPort(request);
		String contextPath = request.getContextPath();
		boolean inHttp = "http".equals(scheme.toLowerCase());
		boolean inHttps = "https".equals(scheme.toLowerCase());

		if (inHttp && (serverPort == 80)) {
			includePort = false;
		}
		else if (inHttps && (serverPort == 443)) {
			includePort = false;
		}

		String redirectUrl = scheme + "://" + serverName + ((includePort) ? (":" + serverPort) : "") + contextPath;
		if (ajaxError) {
			redirectUrl += ajaxErrorPage;
		}
		else if (errorPage != null) {
			redirectUrl += errorPage;
		}
		response.sendRedirect(response.encodeRedirectURL(redirectUrl));
	}

	private Authentication getAuthentication() {
		return SecurityContextHolder.getContext() == null ? null :
		       SecurityContextHolder.getContext().getAuthentication();
	}

	private boolean isLoggedIn() {
		Authentication authentication = getAuthentication();
		if (authentication == null) {
			return false;
		}
		return !authenticationTrustResolver.isAnonymous(authentication);
	}

	/**
	 * Dependency injection for the error page, e.g. '/login/denied'.
	 * @param page  the page
	 */
	public void setErrorPage(final String page) {
		Assert.isTrue(page == null || page.startsWith("/"), "ErrorPage must begin with '/'");
		errorPage = page;
	}

	/**
	 * Dependency injection for the Ajax error page, e.g. '/login/deniedAjax'.
	 * @param page  the page
	 */
	public void setAjaxErrorPage(final String page) {
		Assert.isTrue(page == null || page.startsWith("/"), "Ajax ErrorPage must begin with '/'");
		ajaxErrorPage = page;
	}

	/**
	 * Dependency injection for the Ajax header name; defaults to 'X-Requested-With'.
	 * @param header  the header name
	 */
	public void setAjaxHeader(final String header) {
		ajaxHeader = header;
	}

	/**
	 * Dependency injection for the port resolver.
	 * @param resolver  the resolver
	 */
	public void setPortResolver(final PortResolver resolver) {
		portResolver = resolver;
	}

	/**
	 * Dependency injection for the {@link AuthenticationTrustResolver}.
	 * @param resolver  the resolver
	 */
	public void setAuthenticationTrustResolver(final AuthenticationTrustResolver resolver) {
		authenticationTrustResolver = resolver;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		Assert.notNull(ajaxHeader, "ajaxHeader is required");
		Assert.notNull(portResolver, "portResolver is required");
		Assert.notNull(authenticationTrustResolver, "authenticationTrustResolver is required");
	}
}
