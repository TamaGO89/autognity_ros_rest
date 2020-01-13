package rest.pojo;

import java.util.List;

public class RestRes extends RestObject{
	private final String key;
	private final String path;
	private final List<String> roles;
	private final List<String> stations;

	public RestRes(String key, String path, List<String> roles, List<String> stations) {
		this.key = key; this.path = path; this.roles = roles; this.stations = stations; }

	public String getKey() { return this.key; }
	public String getPath() { return this.path; }
	public List<String> getRoles() { return this.roles; }
	public List<String> getStations() { return this.stations; }
	public boolean isRole(List<String> roles) {
		if (this.roles.isEmpty()) return true;
		for (String role : this.roles) if (roles.contains(role)) return true;
		return false; }
	public boolean isStation(List<String> stations) {
		if (this.roles.isEmpty()) return true;
		for (String station : this.stations) if (stations.contains(station)) return true;
		return false; }

	@Override public boolean equals(Object obj) {
		try { return this.path.equals(((RestRes) obj).getPath()); }
		catch (Exception e) { System.out.println(e.getMessage()); return false; }}
	@Override public String toString() {
		return String.format("\"%s\":\"%s\"", this.key, this.path); }
	@Override public String toJson() {
		return String.format("{\"%s\":\"%s\"}", this.key, this.path); }
}
