package rest.provider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Path;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import rest.util.SessionUtils;

/*==============================================================================================================================
 * REGISTRATION FEATURE * Depending on the requested resource, manage the filters and interceptors needed
 *============================================================================================================================*/
@Provider
public class RegFeature implements DynamicFeature {

	/*--------------------------------------------------------------------------------------------------------------------------
	 * METHODS * Override of configure from DynamicFeature
	 *------------------------------------------------------------------------------------------------------------------------*/	
	@Override public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		Method resource_method = resourceInfo.getResourceMethod();
		// Prepare auth_filter and log_filter
		LogFilter auth_filter = new LogFilter();
		// Set the confidentiality level for the log_filter
		context.register(auth_filter);
		if (resource_method.isAnnotationPresent(SecretRequest.class)) auth_filter.setSecretRequest();
		if (resource_method.isAnnotationPresent(SecretResponse.class)) auth_filter.setSecretResponse();
		if (resource_method.isAnnotationPresent(Log.class))
			auth_filter.setLogLevel(resource_method.getAnnotation(Log.class).value()); 
		// If there's the annotation DenyAll this isn't needed, move on
		if (!resourceInfo.getResourceClass().getPackage().getName().contentEquals("rest.resource")
			|| resource_method.isAnnotationPresent(DenyAll.class)) {
			auth_filter.setDenyAll();
			return; }
		List<String> roles = new ArrayList<String>(),
				 stations = new ArrayList<String>();
		// Register the roles allowed in the resource if there is any
		if (resource_method.isAnnotationPresent(RolesAllowed.class))
			roles = Arrays.asList(resource_method.getAnnotation(RolesAllowed.class).value());
		if (resource_method.isAnnotationPresent(StationsAllowed.class))
			stations.addAll(Arrays.asList(resource_method.getAnnotation(StationsAllowed.class).value()));
		if (resource_method.isAnnotationPresent(Key.class)) {
			String path = "";
			if (resourceInfo.getResourceClass().isAnnotationPresent(Path.class))
				path += resourceInfo.getResourceClass().getAnnotation(Path.class).value();
			if (resource_method.isAnnotationPresent(Path.class))
				path += resource_method.getAnnotation(Path.class).value();
			SessionUtils.addPath(resource_method.getAnnotation(Key.class).value(), path, roles, stations);
		}
		// If the resource is PermitAll and no other roles are specified, move on
		auth_filter.setRolesAllowed(roles);
		auth_filter.setStationsAllowed(stations);
		if (resource_method.isAnnotationPresent(PermitAll.class) && roles.isEmpty() && stations.isEmpty())
			auth_filter.setPermitAll();
	}
}
