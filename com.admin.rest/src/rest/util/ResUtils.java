package rest.util;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import rest.pojo.RestEntity;
import rest.pojo.RestObject;

/*==============================================================================================================================
 * DATABASE MANAGER * Manage the connection and the queries to any SQL SCHEME
 *============================================================================================================================*/
public class ResUtils {
	
	// Static variables
	private static Map<String,Object> exception_map = new HashMap<String,Object>();
	public static String WRONG_TYPE, USER_PASS, MISSING_ID, ROLE_DENIED, BAD_REQ, NOT_AUTH, FORBID,
						 NOT_FOUND, NOT_ALLOWED, NOT_ACCEPTED, NOT_SUPPORTED, SERVER_ERROR, BASE_URL;
	public static int STACK_SIZE;

	/*--------------------------------------------------------------------------------------------------------------------------
	 * GET RESPONSE * Calls to retrieve the correct response, based on status code and String or RestObject
	 *------------------------------------------------------------------------------------------------------------------------*/
	public static ResponseBuilder response(Status status_code, String content) { return getResponse(status_code, content); }
	public static ResponseBuilder response(Status status_code, RestObject object) {
		return getResponse(status_code, object.toJson()); }
	public static String entity(int status_code) { return new RestEntity(status_code).toJson(); }
	/*--------------------------------------------------------------------------------------------------------------------------
	 * GET EXCEPTIONS * Calls to retrieve the correct exception, based on status code or specifying the response message
	 *------------------------------------------------------------------------------------------------------------------------*/
	//GeneralException (###) redirect to the appropriate exception
	public static WebApplicationException exception(Exception exception, Status status_code) {
		switch (status_code) {
			case BAD_REQUEST: return badRequest(exception, BAD_REQ); 
			case UNAUTHORIZED: return notAuthorized(exception, NOT_AUTH);
			case FORBIDDEN: return forbidden(exception, FORBID);
			case NOT_FOUND: return notFound(exception, NOT_FOUND);
			case METHOD_NOT_ALLOWED: return notAllowed(exception, NOT_ALLOWED);
			case NOT_ACCEPTABLE: return notAcceptable(exception, NOT_ACCEPTED);
			case UNSUPPORTED_MEDIA_TYPE: return notSupported(exception, NOT_SUPPORTED);
			case INTERNAL_SERVER_ERROR: return internalServerError(exception, SERVER_ERROR);
			default: return webApplication(exception, status_code);
		}
	}
	//BadRequestException (400) Malformed message
	public static BadRequestException badRequest(Exception exception, String response) {
		return new BadRequestException(getMessage(exception), getResponse(Status.BAD_REQUEST,
																		  new RestEntity(response).toJson()).build());
	}
	//NotAuthorizedException (401) Authentication failure
	public static NotAuthorizedException notAuthorized(Exception exception, String response) {
		return new NotAuthorizedException(getMessage(exception), getResponse(Status.UNAUTHORIZED,
																			 new RestEntity(response).toJson()).build());
	}
	//ForbiddenException (403) Not permitted to access
	public static ForbiddenException forbidden(Exception exception, String response) {
		return new ForbiddenException(getMessage(exception), getResponse(Status.FORBIDDEN,
																		 new RestEntity(response).toJson()).build());
	}
	//NotFoundException (404) Couldn’t find resource
	public static NotFoundException notFound(Exception exception, String response) {
		return new NotFoundException(getMessage(exception), getResponse(Status.NOT_FOUND,
																		new RestEntity(response).toJson()).build());
	}
	//NotAllowedException (405) HTTP method not supported
	public static NotAllowedException notAllowed(Exception exception, String response) {
		return new NotAllowedException(getMessage(exception), getResponse(Status.METHOD_NOT_ALLOWED,
																		  new RestEntity(response).toJson()).build());
	}
	//NotAcceptableException (406) Client media type requested not supported
	public static NotAcceptableException notAcceptable(Exception exception, String response) {
		return new NotAcceptableException(getMessage(exception), getResponse(Status.NOT_ACCEPTABLE,
																			 new RestEntity(response).toJson()).build());
	}
	//NotSupportedException (415) Client posted media type not supported
	public static NotSupportedException notSupported(Exception exception, String response) {
		return new NotSupportedException(getMessage(exception), getResponse(Status.UNSUPPORTED_MEDIA_TYPE,
																			new RestEntity(response).toJson()).build());
	}
	//InternalServerErrorException (500) General server error
	public static InternalServerErrorException internalServerError(Exception exception, String response) {
		return new InternalServerErrorException(getMessage(exception),
												getResponse(Status.INTERNAL_SERVER_ERROR,
															new RestEntity(response).toJson()).build());
	}
	// webApplicationException (###) Other types of exceptions
	public static WebApplicationException webApplication(Exception exception, Status code) {
		return new WebApplicationException(getMessage(exception), getResponse(code).build());
	}
	// webApplicationException (###) Other types of exceptions
	public static WebApplicationException webApplication(Exception exception, String response, Status code) {
		return new WebApplicationException(getMessage(exception), getResponse(code, new RestEntity(response).toJson()).build());
	}
	public static String stackTrace(Exception exception) { return ResUtils.stackTrace(exception, 5); }
	public static String stackTrace(Exception exception, int size) {
		String[] stack = new String[size];
		for (int i = 0; i < Math.min(exception.getStackTrace().length, size); i++)
			stack[i] = exception.getStackTrace()[i].toString();
		return exception.getMessage() + "\n" + String.join("\n", stack);
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * METHOD TO MANAGE THE UTILITIES * Setup the exception map and relative error messages, manage the exception
	 *------------------------------------------------------------------------------------------------------------------------*/
	// return the message of the exception and print to console the stack trace
	private static String getMessage(Exception exception) {
		//exception.printStackTrace();
		return exception.getMessage();
	}
	private static ResponseBuilder getResponse(Status code, String response) { return Response.status(code).entity(response); }
	private static ResponseBuilder getResponse(Status code) {
		return Response.status(code).entity(new RestEntity(code.getStatusCode()).toJson()); }
	// Manage the map, set it up and get strings from it
	private static String getStr(String key) { return (String) exception_map.get(key); }
	private static int getInt(String key) { return (int) exception_map.get(key); }
	public static void setResourceMap(Map<String,Object> map) {
		exception_map = map;
		// Basic configuration
		BASE_URL = getStr("base_url");
		// Customized error messages for various scenario
		WRONG_TYPE = getStr("unknown_type");
		USER_PASS = getStr("user_pass");
		MISSING_ID = getStr("missingid");
		ROLE_DENIED = getStr("roledenied");
		// Standard error messages for the standard bad status codes
		BAD_REQ = getStr("bad_request");
		NOT_AUTH = getStr("not_authorized");
		FORBID = getStr("forbidden");
		NOT_FOUND = getStr("not_found");
		NOT_ALLOWED = getStr("not_allowed");
		NOT_ACCEPTED = getStr("not_acceptable");
		NOT_SUPPORTED = getStr("not_supported");
		SERVER_ERROR = getStr("server_error");
		STACK_SIZE = getInt("stack_size");
	}
}
