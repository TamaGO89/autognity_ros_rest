package rest.pojo;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/* REST OBJECT * General class, needs to be extended by other classes */
public abstract class RestObject {

	// Static variable
	protected static ObjectMapper object_mapper = new ObjectMapper();
	
	// Conversion methods
	@Override public String toString() {
		return this.toJson();
	}
	public String toJson() {
		try { return object_mapper.writeValueAsString(this); }
		catch (JsonProcessingException e) { System.out.println(e.getMessage()); return null; }}
	@SuppressWarnings("unchecked") public Map<String,Object> toMap() {
		return (Map<String,Object>) object_mapper.convertValue(this, Map.class); }
	@Override public abstract boolean equals(Object obj);
}
