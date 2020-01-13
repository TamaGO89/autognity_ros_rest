package rest.pojo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/* BAG INFORMATIONS * Class that describes the instance of a Bag Object, contains data and description of a record */
public class BagInfo extends RestObject {

	// instance variables
	private long bag_id;
	private final Robot robot;
	private final Timestamp created;
	private String content;
	private List<Secret> secrets = new ArrayList<Secret>();
	private BagConfig config;

	// Constructor
	public BagInfo() { this.robot = null; this.created = null; }
	public BagInfo(long bag_id, Robot robot, BagConfig bag_config, String content, Timestamp created) {
		this.bag_id = bag_id; this.robot = robot; this.created = created; this.content = content; this.config = bag_config;
	}
	public BagInfo(long bag_id, Robot robot, BagConfig bag_config, String content, List<Secret> secrets, Timestamp created) {
		this.bag_id = bag_id; this.robot = robot; this.created = created;
		this.secrets = secrets; this.content = content; this.config = bag_config;
	}

	// Getters for FINAL values
	public long getBag_id() {			return this.bag_id; }
	public Robot getRobot() {			return this.robot; }
	public Timestamp getCreated() {		return this.created; }
	// Getters and Setters for everything else
	public BagConfig getConfig() {		return this.config; }
	public void setConfig(BagConfig config) {  this.config = config; }
	public long getConfig_id() {		return this.config.getConfig_id(); }
	public void setId(long config_id) {		   this.config = new BagConfig(config_id); }
	public String getContent() {		return this.content; }
	public void setContent(String content) {   this.content = content; }
	public List<Secret> getSecrets() {	return this.secrets; }
	public Secret getSecret(String key_owner) { 
		for (Secret secret : this.secrets)
			if (secret.getName().equalsIgnoreCase(key_owner)) return secret;
		return null;
	}
	public void setSecrets(List<Secret> secrets) { this.secrets = secrets; }
	public boolean addSecret(Secret secret) {
		return this.secrets.contains(secret) ? false : this.secrets.add(secret);
	}

	@Override public String toString() { return this.toJson(); }
	// Utility methods
	@Override public String toJson() {
		List<String> json_secrets = new ArrayList<String>(this.secrets.size());
		for (Secret secret : secrets) json_secrets.add(secret.toJson());
		return String.format("{\"id\":%d,\"config\":%s,\"client\":%s,\"content\":\"%s\",\"secrets\":[%s],\"created\":\"%s\"}",
							 this.bag_id,this.config.toJson(),this.robot.toJson(),this.content,
							 String.join(",",json_secrets),this.created);
	}
	@Override public boolean equals(Object obj) {
		try { return ((BagInfo) obj).getBag_id() == this.bag_id; }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
}
