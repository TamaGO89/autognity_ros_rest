package rest.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import rest.pojo.DummyContainer;
import rest.pojo.RestEntity;
import rest.pojo.RestObject;
import rest.provider.UserPrincipal;
import rest.util.DbMngr;
import rest.util.LogUtils;
import rest.util.ResUtils;

/* REST RESOURCE * Abstract class for the resources of the web-app to extend */
abstract public class RestResource {

	// Instance variables
	@Context SecurityContext security_context;
	@Context ContainerRequestContext request;
	
	// Getters
	protected UserPrincipal getUserPrincipal() { return (UserPrincipal) this.security_context.getUserPrincipal(); }
	protected DbMngr getDbManager() { return ((UserPrincipal) security_context.getUserPrincipal()).getDbMngr(); }
	protected long getUserID() { return ((UserPrincipal) security_context.getUserPrincipal()).getID(); }
	protected String getUserIDField() { return ((UserPrincipal) security_context.getUserPrincipal()).getID_field(); }
	protected DummyContainer getUserContainer() { return ((UserPrincipal) security_context.getUserPrincipal()).getContainer(); }
	protected String getUserToken() { return ((UserPrincipal) security_context.getUserPrincipal()).getToken(); }
	// Response builders, the getToken method throws an exception, the builder avoid to set the cookie
	protected ResponseBuilder getResponse(Status status_code, RestObject rest_obj) {
		ResponseBuilder response = ResUtils.response(status_code, rest_obj);
		try { response = response.cookie(new NewCookie(LogUtils.TOKEN, this.getUserToken(), ResUtils.BASE_URL,
													   null, null, NewCookie.DEFAULT_MAX_AGE, false)); }
		catch (NullPointerException ignore) { } return response; }
	protected ResponseBuilder getResponse(Status status_code, String rest_str) {
		ResponseBuilder response = ResUtils.response(status_code, rest_str);
		try { response = response.cookie(new NewCookie(LogUtils.TOKEN, this.getUserToken(), ResUtils.BASE_URL,
													   null, null, NewCookie.DEFAULT_MAX_AGE, false)); }
		catch (NullPointerException ignore) { } return response; }
	// Responses
	protected Response getNotFound() { return this.getResponse(Status.NOT_FOUND, new RestEntity(ResUtils.NOT_FOUND)).build(); }
	protected Response getNotAllowed() {
		return this.getResponse(Status.METHOD_NOT_ALLOWED, new RestEntity(ResUtils.NOT_ALLOWED)).build();}
	protected Response getOk(RestObject rest_obj) { return this.getResponse(Status.OK, rest_obj).build(); }
	protected Response getOk(String response) { return this.getResponse(Status.OK, response).build(); }
	protected Response getOk(List<String> responses) { return this.getOk("{\"list\":[" + String.join(",", responses) + "]}"); }
	protected Response getOk(Map<String,String> map) {
		List<String> response = new ArrayList<String>(map.size());
		for (String key : map.keySet()) response.add(String.format("\"%s\":%s", key, map.get(key)));
		System.out.println("{" + String.join(",", response) + "}");
		return this.getOk("{" + String.join(",", response) + "}"); }
	protected Response getCreated(RestObject rest_obj) { return this.getResponse(Status.CREATED, rest_obj).build(); }
	protected Response getCreated(String response) { return this.getResponse(Status.CREATED, response).build(); }
	protected void putException(Exception e) { putException(e, ResUtils.STACK_SIZE); }
	@SuppressWarnings("unchecked") protected void putException(Exception e, int size) {
		((List<String>) this.request.getProperty(LogUtils.LOGGER)).add(ResUtils.stackTrace(e, size));
	}
}
