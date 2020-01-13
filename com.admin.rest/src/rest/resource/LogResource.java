package rest.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.security.auth.login.CredentialException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import rest.pojo.DummyContainer;
import rest.pojo.RestNode;
import rest.pojo.Station;
import rest.provider.Key;
import rest.provider.Log;
import rest.provider.SecretRequest;
import rest.provider.StationsAllowed;
import rest.provider.UserPrincipal;
import rest.util.FileMngr;
import rest.util.SessionUtils;
import javax.mail.MessagingException;

@Path("/log")
public class LogResource extends RestResource {

	// Register a new user with minimum privileges, resource free for all
	@POST @Path("/signin") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON) @PermitAll
	synchronized public Response newUser(DummyContainer user) {
		try { return this.getCreated(SessionUtils.registerUser(this.getDbManager(), this.getUserContainer(), user)); }
		catch ( CredentialException e) { this.putException(e); return this.getNotAllowed(); }
	}

	// For accounts before verification code
	@GET @Key("temporary") @Path("/temporary") @Produces(MediaType.APPLICATION_JSON)
	@StationsAllowed("guests") @RolesAllowed("guest") public Response temporaryUser() {
		return this.getOk(this.getUserContainer()); }

	// Verify the user, set username and password
	@POST @Key("activate") @Path("/activate") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	@StationsAllowed("guests") @RolesAllowed("guest") @SecretRequest
	synchronized public Response activateUser(UserPrincipal user) {
		try { return this.getCreated(SessionUtils.verifyUser(this.getDbManager(), this.getUserContainer(),
															 user.getUsername(), user.getPassword())); }
		catch (CredentialException | NullPointerException | MessagingException e) {
			this.putException(e); return this.getNotAllowed(); }
	}

	// Register a new robot, the roles of the robot must be controlled by the user, the stations in the user subtree
	@POST @Key("set_user") @Path("/set_user") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	@SecretRequest @RolesAllowed("admin") synchronized public Response setUser(DummyContainer user) {
		try { return this.getCreated(SessionUtils.registerUser(this.getDbManager(), this.getUserContainer(), user)); }
		catch (CredentialException e) { this.putException(e); return this.getNotAllowed(); }
	}

	// Register a new robot, the roles of the robot must be controlled by the user, the stations in the user subtree
	@POST @Key("set_robot") @Path("/set_robot") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	@SecretRequest @RolesAllowed("admin") synchronized public Response setRobot(DummyContainer robot) {
		try { return this.getCreated(SessionUtils.registerRobot(this.getDbManager(), this.getUserContainer(), robot)); }
		catch (CredentialException e) { this.putException(e); return this.getNotAllowed(); }
	}

	// Build a map of resources compatible with the roles and stations of the user
	@GET @Path("/paths") @Produces(MediaType.APPLICATION_JSON) @Log(2) public Response getPaths() {
		return this.getOk(SessionUtils.getPaths(this.getUserContainer()));
	}

	// Return an in-depth description of the user
	@GET @Key("client") @Path("/client") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getClient() {
		try { return this.getOk(SessionUtils.getDummy(this.getDbManager(), this.getUserContainer())); }
		catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	// Return a list of users managed by the one asking for them
	@GET @Key("clients") @Path("/users") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getClients() {
		try {
			List<String> results = new ArrayList<String>();
			for (DummyContainer client : SessionUtils.getUserList(this.getDbManager(), this.getUserID(), this.getUserIDField()))
				results.add(client.toJson());
			return this.getOk(results);
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	// Return a list of robots managed by the one asking for them
	@GET @Key("robots") @Path("/robots") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getRobots() {
		try {
			List<String> results = new ArrayList<String>();
			for (DummyContainer robot : SessionUtils.getRobotList(this.getDbManager(), this.getUserID(), this.getUserIDField()))
				results.add(robot.toJson());
			return this.getOk(results);
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	// Return a tree of stations concerning the user
	@GET @Key("stations") @Path("/stations") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getStations() {
		try {
			List<String> results = new ArrayList<String>();
			for (RestNode station : SessionUtils.getStations(this.getDbManager(), this.getUserID(), this.getUserIDField()))
				results.add(((Station) station).toJson());
			return this.getOk(results);
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	@GET @Key("roles") @Path("/roles") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getRoles(@QueryParam("all") @DefaultValue("false") String all) {
		try {
			List<String> results = new ArrayList<String>();
			for (RestNode role : all.contentEquals("false")
					? SessionUtils.getRoles(this.getDbManager(), this.getUserID(), this.getUserIDField())
					: SessionUtils.getSubroles(this.getDbManager(), this.getUserID(), this.getUserIDField()))
				results.add(((Station) role).toJson());
			return this.getOk(results);
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}
	
	@GET @Path("/test_writer") @Produces(MediaType.APPLICATION_JSON) @PermitAll
	public Response testWriter(@QueryParam("path") String path, @QueryParam("ind") long ind, @QueryParam("value") String value) {
		try {
			FileMngr.upload(path, ind, value);
			return this.getOk("tutt'apporno");
		} catch (IOException e) { e.printStackTrace(); return this.getNotAllowed(); }
	}
}
