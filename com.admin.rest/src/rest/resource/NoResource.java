package rest.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class NoResource extends RestResource {

	// Every other request get redirected here
	@GET @Path("{path:.*}") @Produces(MediaType.APPLICATION_JSON)
	public Response getUnknown(@PathParam("path") String path) { return this.getNotFound(); }
	@POST @Path("{path:.*}") @Produces(MediaType.APPLICATION_JSON)
	public Response postUnknown(@PathParam("path") String path) { return this.getNotAllowed(); }
	@PUT @Path("{path:.*}") @Produces(MediaType.APPLICATION_JSON)
	public Response putUnknown(@PathParam("path") String path) { return this.getNotAllowed(); }
	@HEAD @Path("{path:.*}") @Produces(MediaType.APPLICATION_JSON)
	public Response headUnknown(@PathParam("path") String path) { return this.getNotAllowed(); }
	@OPTIONS @Path("{path:.*}") @Produces(MediaType.APPLICATION_JSON)
	public Response optUnknown(@PathParam("path") String path) { return this.getNotAllowed(); }
	@DELETE @Path("{path:.*}") @Produces(MediaType.APPLICATION_JSON)
	public Response delUnknown(@PathParam("path") String path) { return this.getNotAllowed(); }
}
