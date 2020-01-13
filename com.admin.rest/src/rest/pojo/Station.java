package rest.pojo;

import java.util.ArrayList;
import java.util.List;

public class Station extends RestNode {

	private final long station_id;
	private String name;
	private RestNode parent;
	private List<RestNode> children = new ArrayList<RestNode>();

	public Station() { this.station_id = 0; }
	public Station(long station_id) { this.station_id = station_id; }
	public Station(long station_id, String name) { this.station_id = station_id; this.name = name; }
	public Station(String name) { this.station_id = 0; this.name = name; }
	public Station(String name, long parent) { this.station_id = 0; this.name = name; this.parent = new Station(parent); }
	public Station(String name, RestNode parent) { this.station_id = 0; this.name = name; this.parent = parent; }
	public Station(long station_id, String name, long parent) {
		this.station_id = station_id; this.name = name; this.parent = new Station(parent); }

	public long getStation_id() { return this.station_id; }
	@Override public long getID() { return this.station_id; }
	@Override public String getName() { return this.name; }
	@Override public RestNode getParent() { return this.parent; }
	@Override public void setName(String name) { this.name = name; }
	@Override public boolean setParent(String name) { RestNode parent = new Station(name);
		parent.addChild(this); this.parent = parent; return true; }
	@Override public boolean setParent(RestNode parent) { parent.addChild(this); this.parent = parent; return true; }
	@Override public List<RestNode> getChildren() { return this.children; }
	@Override public boolean addChild(String name) { RestNode child = new Station(name, this);
		if (!this.children.contains(child)) return this.children.add(child); return false; }
	@Override public boolean addChild(RestNode child) { 
		if (!this.children.contains(child)) return this.children.add(child); return false; }

	@Override public boolean equals(Object obj) {
		try { return this.name.equals(((Station) obj).getName()); }
		catch (Exception e) { System.out.println(e.getMessage()); return false; }}
	@Override public String toJson() {
		List<String> children = new ArrayList<String>();
		for (RestNode child : this.children) children.add(child.toJson());
		return String.format("{\"name\":\"%s\",\"sub\":[%s]}", this.name, String.join(",", children));
	}
}
