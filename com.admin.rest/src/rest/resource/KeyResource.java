package rest.resource;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import rest.pojo.PubKey;
import rest.provider.Key;
import rest.util.SessionUtils;

@Path("/key")
public class KeyResource extends RestResource {

	@GET @Key("keys") @Path("/key") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("robot")
	public Response getKeys() {
		try {
			List<String> results = new ArrayList<String>();
			for (PubKey key : SessionUtils.getKeys(this.getDbManager(), this.getUserIDField(), this.getUserID()))
				results.add(key.toJson());
			if (results.isEmpty()) throw new NullPointerException("No keys for "+this.getUserID());
			return this.getOk(results);
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	@POST @Key("keys") @Path("/key") @RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	synchronized public Response postKey(PubKey key) {
		try {
			return this.getCreated(SessionUtils.setKey(this.getDbManager(), this.getUserIDField(), this.getUserID(), key));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NullPointerException e) {
			this.putException(e); return this.getNotAllowed(); }
	}
}
