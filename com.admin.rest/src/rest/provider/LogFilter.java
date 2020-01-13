package rest.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.AccountException;
import javax.security.auth.login.CredentialException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.internal.ReaderWriter;

import rest.util.LogUtils;
import rest.util.ResUtils;

/*==============================================================================================================================
 * AUTHENTICATION FILTER * Verify the credentials of the user connecting to the service (BASIC, TOKEN, SIGNIN)
 *============================================================================================================================*/
@Provider 
public class LogFilter implements ContainerRequestFilter, ContainerResponseFilter {

	// Instance variables
    private List<String> roles = new ArrayList<String>(),
    					 stations = new ArrayList<String>();
    private boolean is_deny_all = false, is_permit_all = false, secret_request = false, secret_response = false;
    private int log_level = 10;

	/*--------------------------------------------------------------------------------------------------------------------------
	 * CONTAINER FILTER METHODS * Methods derived by the implemented interfaces, FILTERs for requests and responses
	 * Temporary log levels guide:
	 * 0 - Nothing, ignored
	 * 1 - Address, port, user informations and authorization method
	 * 2 - Request path, method and query
	 * 3 - Request and Response entities
	 *------------------------------------------------------------------------------------------------------------------------*/	
    // REQUESTS, Input stream for the RESTful service
	@Override @SuppressWarnings("unchecked") public void filter(ContainerRequestContext req_ctx) throws IOException {
		List<String> logger = ((List<String>) req_ctx.getProperty(LogUtils.LOGGER));
		if (req_ctx.getHeaders().containsKey(LogUtils.AUTH))
			logger.add(req_ctx.getHeaders().get(LogUtils.AUTH).get(0).split("\\s")[0] + ":" + LogUtils.LOG_SECRET);
		if (req_ctx.getCookies().containsKey(LogUtils.TOKEN)) logger.add(LogUtils.TOKEN + ":" + LogUtils.LOG_SECRET);
		if (this.log_level > 1) {
			logger.add(String.format("%s:%s:%s", req_ctx.getMethod(), req_ctx.getUriInfo().getPath(),
									 req_ctx.getUriInfo().getQueryParameters()));
			if (req_ctx.hasEntity() && this.log_level > 2)
				if (this.secret_request) logger.add("Request:"+LogUtils.LOG_SECRET);
				else {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ReaderWriter.writeTo(req_ctx.getEntityStream(), out);
					logger.add("Request:"+out.toString().replaceAll("\\s+", " "));
					req_ctx.setEntityStream(new ByteArrayInputStream(out.toByteArray()));
				}
		}
		try {
			// If DenyAll, no one is allowed, if PermitAll, go on
			if (is_deny_all) throw new AccountException("No one allowed!");
			if (is_permit_all) {
				req_ctx.setSecurityContext(new UserContext(LogUtils.getGuestUser()));
				return; }
			// Check for cookies and for an authorization header for basic authentication
			else if (req_ctx.getCookies().containsKey(LogUtils.TOKEN) && !req_ctx.getHeaders().containsKey(LogUtils.AUTH))
					 req_ctx.setSecurityContext(new UserContext(
							 LogUtils.checkCookie(req_ctx.getCookies().get(LogUtils.TOKEN).getValue()), LogUtils.TOKEN));
			else if (!req_ctx.getCookies().containsKey(LogUtils.TOKEN) &&
					 req_ctx.getHeaders().get(LogUtils.AUTH).get(0).split("\\s")[0].equalsIgnoreCase(LogUtils.BASIC))
				req_ctx.setSecurityContext(new UserContext(
						LogUtils.checkUserPass(req_ctx.getHeaders().get(LogUtils.AUTH).get(0).split("\\s")[1]),
											   LogUtils.BASIC));
			else if (!req_ctx.getCookies().containsKey(LogUtils.TOKEN) && 
					 req_ctx.getHeaders().get(LogUtils.AUTH).get(0).split("\\s")[0].equalsIgnoreCase(LogUtils.SIGNIN))
				req_ctx.setSecurityContext(new UserContext(
						LogUtils.checkVerification(req_ctx.getHeaders().get(LogUtils.AUTH).get(0).split("\\s")[1]),
												   LogUtils.SIGNIN));
			else throw new IllegalArgumentException(
					String.format("Wrong authentication scheme: %s | %s",
								  req_ctx.getCookies().containsKey(LogUtils.TOKEN) ? LogUtils.TOKEN : "",
								  req_ctx.getHeaders().containsKey(LogUtils.AUTH)
										  ? req_ctx.getHeaders().get(LogUtils.AUTH).get(0).split("\\s")[0] : ""));
			LogUtils.checkPermissions((UserPrincipal) req_ctx.getSecurityContext().getUserPrincipal(), roles, stations);
		// Any type of exception that might emerge will be dealt as a authentication error
		} catch (AccountException e) {
			if (this.log_level > 0) logger.add(ResUtils.stackTrace(e));
			throw ResUtils.forbidden(e, ResUtils.ROLE_DENIED);
		} catch (CredentialException | NullPointerException e) {
			if (this.log_level > 0) logger.add(ResUtils.stackTrace(e));
			throw ResUtils.notAuthorized(e, ResUtils.USER_PASS);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			if (this.log_level > 0) logger.add(ResUtils.stackTrace(e));
			throw ResUtils.notAuthorized(e, ResUtils.WRONG_TYPE);
		}
	}

	/* TODO For every transaction check the Request time and Response time... try to make every request about the same duration
	 * I can do the same with the length of each response, trying to make them all the same length with additional padding
	 * This two are probably really trivial additions... 
	 * Check the maximum length of a reply and the maximum elapsed time to compute an answer
	 * Add padding to the response to match the estimated maximum length and wait to deliver the response the maximum delay
	 * This is another thing that can be avoided, the difference in size and duration is useful only for specialized attacks*/
	@Override public void filter(ContainerRequestContext req_ctx, ContainerResponseContext res_ctx) throws IOException {
		if (!res_ctx.hasEntity()) res_ctx.setEntity(ResUtils.entity(res_ctx.getStatus()));
		if (this.log_level > 0) {
			@SuppressWarnings("unchecked") List<String> logger = (List<String>) req_ctx.getProperty(LogUtils.LOGGER);
			if (this.log_level > 2)
				if (res_ctx.getStatusInfo().getFamily() == Family.SUCCESSFUL && this.secret_response)
					logger.add(String.format("Response:%d:%s", res_ctx.getStatus(), LogUtils.LOG_SECRET));
				else logger.add(String.format("Response:%d:%s", res_ctx.getStatus(), res_ctx.getEntity()));
			res_ctx.getHeaders().addFirst(LogUtils.LOG_ID, LogUtils.getID(
					(UserPrincipal) req_ctx.getSecurityContext().getUserPrincipal(), logger));
		}
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * PUBLIC CONFIGURATION METHODS * Used by the RegistrationFeature to set the roles, etc...
	 *------------------------------------------------------------------------------------------------------------------------*/
	// If DenyAll or PermitAll are annotations of the requested resource
	public boolean setDenyAll() { return this.is_deny_all = true; }
	public boolean setPermitAll() { return this.is_permit_all = true; }
	public boolean setSecretRequest() { return this.secret_request = true; }
	public boolean setSecretResponse() { return this.secret_response = true; }
	public void setLogLevel(int level) { this.log_level = level; }
	// Give this filter the roles associated with the requested resource
	public void setRolesAllowed(List<String> roles_allowed) { this.roles = roles_allowed; }
	public void setStationsAllowed(List<String> stations_allowed) { this.stations = stations_allowed; }
}
