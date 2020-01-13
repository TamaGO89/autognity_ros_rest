package rest.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mysql.cj.protocol.x.XProtocolError;

import rest.pojo.BagInfo;
import rest.pojo.BagRaw;
import rest.pojo.ConfigContainer;
import rest.provider.Key;
import rest.util.SessionUtils;

@Path("/bag")
public class BagResource extends RestResource {
	
	@GET @Key("overview") @Path("/view") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getOverview(@QueryParam("from") long from_ts,
								@QueryParam("to") @DefaultValue("253402300799000") long to_ts,
								@QueryParam("rob") String robot) {
		try {
			return this.getOk(SessionUtils.getBagOverview(this.getDbManager(), this.getUserID(),
														  this.getUserIDField(), from_ts, to_ts, robot));
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	@GET @Key("config") @Path("/cfg") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("robot")
	public Response getConfig(@QueryParam("from") long from_ts,
							  @QueryParam("keys") @DefaultValue("false") String keys) {
		try {
			return this.getOk(SessionUtils.getBagConfig(this.getDbManager(), this.getUserID(),
														this.getUserIDField(), from_ts, new Boolean(keys)));
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	@POST @Key("config") @Path("/cfg") @RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	synchronized public Response postConfig(ConfigContainer config_container) {
		try {
			return this.getCreated(SessionUtils.setBagConfig(this.getDbManager(), this.getUserID(),
															 this.getUserIDField(), config_container));
		} catch (NullPointerException | XProtocolError e) { this.putException(e); return this.getNotAllowed(); }
	}

	@GET @Key("info") @Path("/info") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getInfo(@QueryParam("from") long from_ts,
							@QueryParam("to") @DefaultValue("253402300799000") long to_ts) {
		try {
			List<String> results = new ArrayList<String>();
			for (BagInfo bag : SessionUtils.getBagInfo(this.getDbManager(), this.getUserID(),
													   this.getUserIDField(), Math.max(from_ts, 4000000), to_ts))
				results.add(bag.toJson());
			if (results.isEmpty())
				throw new NullPointerException("No bags for "+this.getUserID()+" from "+from_ts+" to "+to_ts);
			return this.getOk(results);
		} catch (NullPointerException e) { this.putException(e); return this.getNotFound(); }
	}

	@POST @Key("info") @Path("/info") @RolesAllowed("robot")
	@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	synchronized public Response postInfo(BagInfo bag_info) {
		try {
			return this.getCreated(SessionUtils.setBagInfo(this.getDbManager(), this.getUserID(),
														   this.getUserIDField(), bag_info));
		} catch (NullPointerException | XProtocolError e) { this.putException(e); return this.getNotAllowed(); } 
	}

	@GET @Key("raw") @Path("/raw") @Produces(MediaType.APPLICATION_JSON) @RolesAllowed("user")
	public Response getRaw(@QueryParam("id") long bag_id) {
		try {
			return this.getOk(SessionUtils.getBagRaw(this.getDbManager(), this.getUserID(), this.getUserIDField(), bag_id));
		} catch (NullPointerException | IOException e) { this.putException(e); return this.getNotFound(); }
	}

	@POST @Key("raw") @Path("/raw") @RolesAllowed("robot")
	@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	synchronized public Response postRaw(BagRaw bag_raw) {
		try {
			// if the sequence expected is different from the one he got return an error with the right sequence number
			return this.getCreated(SessionUtils.setBagRaw(this.getDbManager(), this.getUserID(),
														  this.getUserIDField(), bag_raw));
		} catch (NullPointerException | IOException e) { this.putException(e); return this.getNotAllowed(); }
	}
}
