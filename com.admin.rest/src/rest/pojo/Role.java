package rest.pojo;

import java.util.ArrayList;
import java.util.List;

public class Role extends RestNode {

	private final long role_id;
	private String name;
	private RestNode parent;
	private List<RestNode> children = new ArrayList<RestNode>();
	
	public Role() { this.role_id = 0; }
	public Role(long role_id) { this.role_id = role_id; }
	public Role(String name) { this.role_id = 0; this.name = name; }
	public Role(long role_id, String name) { this.role_id = role_id; this.name = name; }
	public Role(long role_id, String name, String parent) {
		this.role_id = role_id; this.name = name; this.parent = new Role(parent); }
	public Role(long role_id, String name, long parent) {
		this.role_id = role_id; this.name = name; this.parent = new Role(parent); }

	public long getRole_id() { return this.role_id; }
	@Override public long getID() { return this.role_id; }
	@Override public String getName() { return this.name; }
	@Override public RestNode getParent() { return this.parent; }
	@Override public void setName(String name) { this.name = name; }
	@Override public boolean setParent(String name) { RestNode parent = new Role(name);
		parent.addChild(this); this.setParent(parent); return true; }
	@Override public boolean setParent(RestNode parent) { parent.addChild(this); this.parent = parent; return true; }
	@Override public List<RestNode> getChildren() { return this.children; }
	@Override public boolean addChild(String name) { RestNode child = new Role(name);
		if (!this.children.contains(child)) return this.children.add(child); return false; }
	@Override public boolean addChild(RestNode child) { 
		if (!this.children.contains(child)) return this.children.add(child); return false; }

	@Override public boolean equals(Object obj) {
		try { return this.name.equals(((Role) obj).getName()); }
		catch (Exception e) { System.out.println(e.getMessage()); return false; }}
	@Override public String toJson() {
		List<String> children = new ArrayList<String>();
		for (RestNode child : this.children) children.add(child.toJson());
		return String.format("{\"name\":\"%s\",\"sub\":[%s]}", this.name, String.join(":", children));
	}
}