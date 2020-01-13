package rest.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import rest.util.LogUtils;

@Provider @PreMatching public class HttpFilter implements ContainerRequestFilter {

	@Context protected HttpServletRequest http_req;

	@Override public void filter(ContainerRequestContext req_ctx) throws IOException {
		// Set a default security context that can be used for logging reason if a specified one can't be assigned
		req_ctx.setSecurityContext(new UserContext(LogUtils.getGuestUser()));
		List<String> logger = new ArrayList<String>();
		logger.add(String.format("%s:%d", http_req.getRemoteAddr(), http_req.getRemotePort()));
		req_ctx.setProperty(LogUtils.LOGGER, logger);
		// TODO verify the IP address, the application used to login, etc...
		// Since it's not that hard to spoof all of this, it's not a priority now and can be implemented later
	}
}
