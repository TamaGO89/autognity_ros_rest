package rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/*==============================================================================================================================
 * CONFIGURATION MANAGER * Provide the name of the JSON with the properties and a dictionary of all the voices in it
 *============================================================================================================================*/
public class CfgMngr {

	/*--------------------------------------------------------------------------------------------------------------------------
	 * UTILITIES * To call the right properties
	 *------------------------------------------------------------------------------------------------------------------------*/
	private static Map<String,Map<String,Object>> properties = new HashMap<String,Map<String,Object>>(); 

	// To parse the JSON properties into the configuration of the restful application
	public static Map<String,Object> jsonProperties() {return jsonProperties("config.json"); }
	public static Map<String,Object> jsonProperties(String properties_file) {
		if (properties.get(properties_file) != null) return properties.get(properties_file);
		InputStream inputStream = CfgMngr.class.getResourceAsStream("config.json");
		if (inputStream != null) {
			try {
				properties.put(properties_file, new ObjectMapper().readValue(inputStream,
												new TypeReference<Map<String, Object>>() {}));
			} catch (IOException e) { e.printStackTrace(); }
		}
		return properties.get(properties_file);
	}

	// Might be useful for classes that won't implement the @Context Configuration
	public static Object getProperty(String property_name) { return getProperty("config.json", property_name); }
	public static Object getProperty(String properties_file, String property_name) {
		return properties.get(properties_file).get(property_name);
	}
}