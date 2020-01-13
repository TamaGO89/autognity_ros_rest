package rest.pojo;

import java.sql.Timestamp;

/* BAG CONFIGURATION * Class that describes the instance of a configuration */
public class BagConfig extends RestObject {

	// Instance variables
	private final long config_id;
	private final Timestamp created;
	private final User user;
	private String content;

	// Constructor
	public BagConfig() { this.config_id = 0; this.user = new User(); this.created = null; }
	public BagConfig(long config_id) { this.user = new User(); this.config_id = config_id; this.created = null; }
	public BagConfig(String content) {
		this.user = new User(); this.config_id = 0; this.created = null; this.content = content; }
	public BagConfig(long config_id, String user, String content, Timestamp created) {
		this.config_id = config_id; this.user = new User(user); this.content = content; this.created = created; }

	// Getters for FINAL values
	public long getConfig_id() { return this.config_id; }
	public Timestamp getCreated() { return this.created; }
	public String getClient() { return this.user.getName(); }
	// Getters and Setters for everything else 
	public String getContent() { return this.content; }
	public void setContent(String content) { this.content = content; }

	// Utility methods
	@Override public String toJson() {
		return String.format("{\"id\":%d,\"client\":\"%s\",\"content\":\"%s\",\"created\":\"%s\"}",
							 this.config_id, this.user.getName(), this.content, this.created);
	}

	@Override public boolean equals(Object obj) {
		try { return ((BagConfig) obj).config_id == this.config_id; }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
}
