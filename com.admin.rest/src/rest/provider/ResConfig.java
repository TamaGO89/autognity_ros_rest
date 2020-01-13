package rest.provider;

import java.util.Map;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import rest.util.CfgMngr;
import rest.util.CryptUtils;
import rest.util.DbMngr;
import rest.util.FileMngr;
import rest.util.ResUtils;
import rest.util.SessionUtils;
import rest.util.LogUtils;
import rest.util.MailMngr;

/* This class specify the base URL for the REST requests */
@ApplicationPath("/ros_rest/")
public class ResConfig extends ResourceConfig {
	
	// Initial configuration that runs for each call (for what I've been able to understand)
	@SuppressWarnings("unchecked") public ResConfig() {
		// Configuration Properties
		this.addProperties(CfgMngr.jsonProperties());
		// DB Manager
		DbMngr.setDbMap((Map<String,Object>) getProperties().get("database"));
		MailMngr.setMailMap((Map<String,Object>) getProperties().get("mail"));
		FileMngr.setFileMap((Map<String,Object>) getProperties().get("filesystem"));
		// Services
		this.register(HttpFilter.class);
		this.register(RegFeature.class);
		// Packages
		this.packages("rest.resource");
		// Authentication utilities
		CryptUtils.setCryptMap((Map<String,Object>) getProperties().get("cryptography"));
		LogUtils.setDbMngr(DbMngr.getDbMngr("login"));
		SessionUtils.setMailMngr(MailMngr.getMailMngr("signin"));
		LogUtils.setLoginMap((Map<String,Object>) getProperties().get("authentication"));
		ResUtils.setResourceMap((Map<String,Object>) getProperties().get("resources"));
		// Configuration over
		System.out.println("\n\tREADY -> SET -> GO\n");
	}
}
