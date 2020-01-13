package rest.pojo;

import java.util.ArrayList;
import java.util.List;

/* CONFIG CONTAINER * Object that contains a configuration bag, a list of public keys and a list of robots */
public class ConfigContainer extends RestObject {

	// Instance variables
	private BagConfig bag_config;
	private List<PubKey> public_keys = new ArrayList<PubKey>();
	private List<Robot> robots = new ArrayList<Robot>();

	public ConfigContainer() {}
	public ConfigContainer(BagConfig bag_config) { this.bag_config = bag_config; }
	public ConfigContainer(BagConfig bag_config, List<Robot> robots) { this.bag_config = bag_config; this.robots = robots; }
	public ConfigContainer(BagConfig bag_config, List<Robot> robots, List<PubKey> public_keys) {
		this.bag_config = bag_config;	this.robots = robots; this.public_keys = public_keys; }

	// Getters and Setters
	public long getId() { return this.bag_config.getConfig_id(); }
	public String getUser() { return this.bag_config.getClient(); }
	public BagConfig getConfig() { return this.bag_config; }
	public void setConfig(BagConfig bag_config) { this.bag_config = bag_config; }
	public void setContent(String content) { this.bag_config = new BagConfig(content); }
	public List<PubKey> getKeys() { return this.public_keys; }
	public void setPublic_keys(List<PubKey> public_keys) { this.public_keys = public_keys; }
	public void setKeys(List<String> key_owners) {
		public_keys = new ArrayList<PubKey>(key_owners.size());
		for (String key_owner : key_owners) public_keys.add(new PubKey(key_owner)); }
	public boolean addPublic_key(PubKey public_key) {
		if (this.public_keys.contains(public_key)) return false;
		return this.public_keys.add(public_key); }
	public PubKey getPublic_key(String key_owner) {
		for (PubKey public_key : this.public_keys) if (public_key.getName() == key_owner) return public_key;
		return null; }
	public List<Robot> getRobots() { return this.robots; }
	public void setRobots(List<Robot> robots) { this.robots = robots; }
	public void setClient(List<String> robots) {
		this.robots = new ArrayList<Robot>(robots.size());
		for (String robot : robots) this.robots.add(new Robot(robot)); }
	public boolean addRobot(String robot) {
		if (this.getRobot(robot) != null) return false;
		return this.robots.add(new Robot(robot)); }
	public boolean addRobot(Robot robot) {
		if (this.robots.contains(robot)) return false;
		return this.robots.add(robot); }
	public Robot getRobot(String rob) {
		for (Robot robot : this.robots) if (robot.getName() == rob) return robot;
		return null; }

	// Utility methods
	@Override public boolean equals(Object obj) {
		try { return this.bag_config.equals(((ConfigContainer) obj).getConfig());
		} catch (Exception e) { System.out.println(e.getMessage()); return false; }
	}

	@Override public String toJson() {
		return String.format("{\"config\":%s,\"clients\":%s,\"keys\":%s}", this.bag_config, this.robots, this.public_keys);
	}
}
