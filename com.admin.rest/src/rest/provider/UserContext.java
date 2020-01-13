package rest.provider;

import javax.ws.rs.core.SecurityContext;

import rest.util.DbMngr;

/* USER CONTEXT * Security Context for users and robots, contains references to Database Manager and other resources */
public class UserContext implements SecurityContext{
	
	// Instance variables
	private final UserPrincipal user_prin;
	private final String auth_scheme;

	// Constructors
	public UserContext (UserPrincipal user_prin) {					   this.user_prin = user_prin;
																	   this.auth_scheme = "NONE"; }
	public UserContext (UserPrincipal user_prin, String auth_scheme) { this.user_prin = user_prin;
																	   this.auth_scheme = auth_scheme; }

	// Getters
	public String getUserToken() throws NullPointerException {
		return this.user_prin.getToken(); }
	@Override public UserPrincipal getUserPrincipal() {	return this.user_prin; }
	public DbMngr getUserDbMngr() {						return this.user_prin.getDbMngr(); }
	@Override public String getAuthenticationScheme() {	return this.auth_scheme; }

	// Checks for Roles, Stations, etc.
	@Override public boolean isUserInRole(String role) {return this.user_prin.isRole(role); }
	public boolean isUserInStation(String station) {	return this.user_prin.isStation(station); }
	@Override public boolean isSecure() {				return this.auth_scheme != "NONE"; }
	public boolean isUserRobot() {						return this.user_prin.isRobot(); }
}
