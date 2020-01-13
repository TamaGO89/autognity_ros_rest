package rest.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

/* REST MAP * Class describing the instance of a general object, his values are contained in a simple HashMap */
public class RestMap extends RestObject {

	// Instance variable
	private Map<String,Object> map = new HashMap<String,Object>();

	// Constructor
	public RestMap() {}
	public RestMap(Map<String,Object> map) { this.map.putAll(map); }
	public RestMap(List<String> keys, List<Object> values) throws IllegalArgumentException {
		if (!(keys.size() > 0 && keys.size() == values.size()))
			throw new IllegalArgumentException("Keys.size() != values.size() OR keys.size() == 0");
		for (int i = 0; i < keys.size(); i++) this.map.put(keys.get(i), values.get(i)); 
	}
	public RestMap(String[] keys, Object[] values) throws IllegalArgumentException {
		if (!(keys.length > 0 && keys.length == values.length))
			throw new IllegalArgumentException("keys.length != values.length");
		for (int i = 0; i < keys.length; i++) this.map.put(keys[i], values[i]);
	}

	// Getter and Setter
	public Object get(String key) {	return this.map.get(key); }
	public void set(String key, Object value) { this.map.putIfAbsent(key, value); }

	// Conversion methods
	@Override public String toString() { return this.map.toString(); }
	@Override public Map<String,Object> toMap() { return this.map; }
	@Override public String toJson() {
		try { return object_mapper.writeValueAsString(this.map); }
		catch (JsonProcessingException e) { System.out.println(e.getMessage()); return null; }
	}
	@Override public boolean equals(Object obj) {
		try { return ((RestMap) obj).map.equals(this.map); }
		catch(Exception e) { System.out.println(e.getMessage()); return false; }
	}
}
