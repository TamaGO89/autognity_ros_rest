package rest.pojo;

import java.sql.Timestamp;

import rest.util.DbMngr.DbMap;

/* DUMMY * Abstract class for Robots and Users to extend */
public abstract class Dummy extends RestObject {

	// Instance variables
	protected final long id;
	protected String name;
	protected final Timestamp created, expired;
	
	// Constructors
	public Dummy() { this.id = 0; this.name = null; this.created = null; this.expired = null; }
	public Dummy(long id) { this.id = id; this.name = null; this.created = null; this.expired = null; }
	public Dummy(String name) { this.id = 0; this.name = name; this.created = null; this.expired = null; }
	public Dummy(long id, String name, Timestamp created, Timestamp expired) {
		this.id = id; this.name = name; this.created = created; this.expired = expired; }

	// Getters
	public long getID() {			return this.id; }
	public String getID_field() {	return Robot.class.isInstance(this) ? DbMap.ID_ROBOT : DbMap.ID_USER; }
	public String getName() {		return this.name; }
	public void setName(String name) {	   this.name = name; }
	public Timestamp getCreated() { return this.created; }
	public Timestamp getExpires() { return this.expired; }
	
	// Utilities
	@Override public boolean equals(Object obj) {
		try { return this.getName().equalsIgnoreCase(((Dummy) obj).getName()); }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
	
	@Override public String toJson() {
		return Robot.class.isInstance(this) ? ((Robot) this).toJson() : ((User) this).toJson();
	}
}
