package rest.pojo;

import java.util.ArrayList;
import java.util.List;

/* DUMMY CONTAINER * Describes a dummy, with his/its roles and stations */
public class DummyContainer extends RestObject{

	// Instance variables
	private boolean banned;
	private int violations;
	private List<String> roles = new ArrayList<String>(),
						 stations = new ArrayList<String>();
	private Dummy dummy;

	// Constructors
	public DummyContainer() { this.dummy = new User(); }
	public DummyContainer(Dummy dummy) { this.dummy = dummy; }
	public DummyContainer(Dummy dummy, boolean banned, int violations) {
		this.dummy = dummy; this.banned = banned; this.violations = violations; }
	public DummyContainer(Dummy dummy, List<String> roles, List<String> stations) {
		this.dummy = dummy; this.roles = roles; this.stations = stations; }
	public DummyContainer(Dummy dummy, boolean banned, int violations, List<String> roles, List<String> stations) {
		this.dummy = dummy; this.banned = banned; this.violations = violations; this.roles = roles; this.stations = stations; }

	// Getters, Setters and Comparing functions
	public long getID() {						return this.dummy.getID(); }
	public String getID_field() {				return this.dummy.getID_field(); }
	public String getName() {					return this.dummy.getName(); }
	public void setName(String name) {				   this.dummy.setName(name); }
	public Dummy getDummy() {					return this.dummy; }
	public void setDummy(Dummy dummy) {				   this.dummy = dummy; }
	public int getViolations() {				return this.violations; }
	public void setViolations(int violations) {		   this.violations = violations; }
	public int addViolation() {					return ++this.violations; }
	public boolean isBanned() {					return this.banned; }
	public void setBanned(boolean banned) {			   this.banned = banned; }
	public List<String> getRoles() {			return this.roles; }
	public void setRoles(List<String> roles) {		   this.roles = roles; }
	public boolean addRole(String role) {		return this.roles.contains(role) ? false : this.roles.add(role); }
	public boolean addRoles(List<String> roles) {
		for (String role : roles) if (!this.roles.contains(role)) this.roles.add(role);
		return true; }
	public List<String> getStations() {			return this.stations; }
	public void setStations(List<String> stations) {   this.stations = stations; }
	public boolean addStation(String station) {	return this.stations.contains(station) ? false : this.stations.add(station); }
	public boolean addStations(List<String> stations) {
		for (String station : stations) if (!this.stations.contains(station)) this.stations.add(station);
		return true; }
	public User getRegistrant() { return Robot.class.isInstance(this.dummy) ? ((Robot) this.dummy).getUser() : null; }
	public void setRegistrant(User user) { if (Robot.class.isInstance(this.dummy)) ((Robot) this.dummy).setUser(user); }
	public String getFirstname() { return User.class.isInstance(this.dummy) ? ((User) this.dummy).getFirstname() : null; }
	public void setFirstname(String name) { if (User.class.isInstance(this.dummy)) ((User) this.dummy).setFirstname(name); }
	public String getLastname() { return User.class.isInstance(this.dummy) ? ((User) this.dummy).getLastname() : null; }
	public void setLastname(String name) { if (User.class.isInstance(this.dummy)) ((User) this.dummy).setLastname(name); }
	public String getAddress() { return User.class.isInstance(this.dummy) ? ((User) this.dummy).getAddress() : null; }
	public void setAddress(String addr) { if (User.class.isInstance(this.dummy)) ((User) this.dummy).setAddress(addr); }

	@Override public boolean equals(Object obj) {
		try { return ((DummyContainer) obj).getDummy() == this.getDummy() &&
					 ((DummyContainer) obj).getRoles() == this.getRoles() &&
					 ((DummyContainer) obj).getStations() == this.getStations(); }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
	@Override public String toJson() {
		return String.format("{\"client\":%s,\"roles\":[\"%s\"],\"stations\":[\"%s\"]}", this.dummy.toJson(),
							 String.join("\",\"",this.getRoles()), String.join("\",\"",this.getStations()));
	}
}
