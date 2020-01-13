package rest.provider;

import java.security.Principal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import rest.pojo.DummyContainer;
import rest.pojo.Robot;
import rest.pojo.User;
import rest.util.DbMngr;

/* USER PRINCIPAL * Describes the instance of a user connected to the web service */
public class UserPrincipal implements Principal {

	// Static configuration
	private static final long STD_ID = 1;
	private static final String STD_NAME = "guest";
	// Instance variables
	private String username, password, token;
	private List<String> paths = new ArrayList<String>();
	private DbMngr db_mngr;
	private DummyContainer dummy_container;

	// Constructors
	public UserPrincipal() { 
		this.dummy_container = new DummyContainer(new User(STD_ID, STD_NAME, new Timestamp(System.currentTimeMillis()), null));
		this.dummy_container.setViolations(0); this.dummy_container.setBanned(false); this.db_mngr = DbMngr.getDbMngr("base"); }

	public UserPrincipal(long id, String name) {
		this.dummy_container = new DummyContainer(new User(id, name, new Timestamp(System.currentTimeMillis()), null));
		this.username = null;
		this.password = null;
		this.dummy_container.setViolations(0);
		this.dummy_container.setBanned(false);
	}

	public UserPrincipal(long id, String name, String db) {
		this.dummy_container = new DummyContainer(new User(id, name, new Timestamp(System.currentTimeMillis()), null));
		this.username = null;
		this.password = null;
		this.dummy_container.setViolations(0);
		this.dummy_container.setBanned(false);
		this.db_mngr = DbMngr.getDbMngr(db);
	}

	public UserPrincipal(long robot, long user, String name, String username, String password,
						 int violations, boolean banned, Timestamp created, Timestamp expired) {
		this.dummy_container = new DummyContainer(robot > 0 ? new Robot(robot,name,created,expired)
															: new User(user,name,created,expired));
		this.username = username;
		this.password = password;
		this.dummy_container.setViolations(violations);
		this.dummy_container.setBanned(banned);
	}
	
	public UserPrincipal(long robot, long user, String name, String username, String password, int violations, boolean banned,
			 			 Timestamp created, Timestamp expired, String firstname, String lastname, String address) {
		this.dummy_container = new DummyContainer(robot > 0 ? new Robot(robot, name, created, expired,
																		new User(user, firstname, lastname, address))
															: new User(user,name,created,expired,firstname,lastname,address));
		this.username = username;
		this.password = password;
		this.dummy_container.setViolations(violations);
		this.dummy_container.setBanned(banned);
	}

	// Getters, Setters and Comparing functions
	@Override public String getName() {					return this.dummy_container.getDummy().getName(); }
	public void setName(String name) {						   this.dummy_container.getDummy().setName(name); }
	public boolean isRobot() {							return Robot.class.isInstance(this.dummy_container.getDummy()); }
	public long getRobot_id() {							return this.isRobot() ? this.dummy_container.getID() : 0; }
	public long getUser_id() {							return this.isRobot() ? 0 : this.dummy_container.getID(); }
	public long getID() {								return this.dummy_container.getID(); }
	public String getID_field() {						return this.dummy_container.getID_field(); }
	public DummyContainer getContainer() {				return this.dummy_container; }
	public User getRegistrant() {						return this.dummy_container.getRegistrant(); }
	public void setRegistrant(User registrant) {			   this.dummy_container.setRegistrant(registrant); }
	public String getFirstname() {						return this.dummy_container.getFirstname(); }
	public void setFirstname(String firstname) {			   this.dummy_container.setFirstname(firstname); }
	public String getLastname() {						return this.dummy_container.getLastname(); }
	public void setLastname(String lastname) {				   this.dummy_container.setLastname(lastname); }
	public String getAddress() {						return this.dummy_container.getAddress(); }
	public void setAddress(String address) {				   this.dummy_container.setAddress(address); }
	public String getUsername() {						return this.username; }
	public void setUsername(String username) {				   this.username = username; }
	public String getPassword() {						return this.password; }
	public void setPassword(String password) {				   this.password = password; }
	public int getViolations() {						return this.dummy_container.getViolations(); }
	public int addViolation() {							return this.dummy_container.addViolation(); }
	public boolean isBanned() {							return this.dummy_container.isBanned(); }
	public Timestamp getCreated() {						return this.dummy_container.getDummy().getCreated(); }
	public Timestamp getExpires() {						return this.dummy_container.getDummy().getExpires(); }
	public List<String> getRoles() {					return this.dummy_container.getRoles(); }
	public void setRoles(List<String> roles) {				   this.dummy_container.setRoles(roles); }
	public boolean addRole(String role) {				return this.dummy_container.addRole(role); }
	public boolean addRoles(List<String> roles) {		return this.dummy_container.addRoles(roles); }
	public boolean isRole(String role) {				return this.dummy_container.getRoles().contains(role); }
	public boolean isRole(List<String> roles) {			return this.dummy_container.getRoles().containsAll(roles); }
	public List<String> getStations() {					return this.dummy_container.getStations(); }
	public void setStations(List<String> stations) {		   this.dummy_container.setStations(stations); }
	public void addStation(String station) {				   this.dummy_container.addStation(station); }
	public void addStations(List<String> stations) {		   this.dummy_container.addStations(stations); }
	public boolean isStation(String station) {			return this.dummy_container.getStations().contains(station); }
	public boolean isStation(List<String> stations) {	return this.dummy_container.getStations().containsAll(stations); }
	public List<String> getPaths() {					return this.paths; }
	public void setPaths(List<String> paths) {				   this.paths = paths; }
	public void addPath(String path) {						   this.paths.add(path); }
	public boolean isPath(String path) {				return this.paths.contains(path); }
	public boolean isPath(List<String> path) {			return this.paths.containsAll(paths); }
	public DbMngr getDbMngr() {							return this.db_mngr; }
	public void setDbMngr(DbMngr db_mngr) {					   this.db_mngr = db_mngr; }
	public String getToken() throws NullPointerException {
		if (this.token == null) throw new NullPointerException("There's no token for "+this.getName());
		return this.token; }
	public void setToken(String token) {					   this.token = token; }
	public boolean isToken(String token) {				return this.token.contentEquals(token); }

	@Override public boolean equals(Object obj) {
		try { return ((UserPrincipal) obj).getRobot_id() == this.getRobot_id() &&
					 ((UserPrincipal) obj).getUser_id() == this.getUser_id(); }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
}
