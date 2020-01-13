package rest.pojo;

import java.sql.Timestamp;

/* PUBLIC KEY * Describes the instance of a key, with owner, value and id */
public class PubKey extends RestObject {

	// instance variables
	private final long key_id;
	private final Timestamp created;
	private Timestamp expires;
	private String key_owner, key_value;

	// Constructors
	public PubKey() { this.key_id = 0; this.created = null; }
	public PubKey(String key_owner) { this.key_id = 0; this.key_owner = key_owner; this.created = null; }
	public PubKey(String key_owner, String key_value) {
		this.key_id = 0; this.key_owner = key_owner; this.key_value = key_value; this.created = null; }
	public PubKey(long key_id, String key_owner, String public_key) {
		this.key_id = key_id; this.key_owner = key_owner; this.key_value = public_key; this.created = null; }
	public PubKey(long key_id, String key_owner, String public_key, Timestamp created, Timestamp expires) {
		this.key_id = key_id;
		this.key_owner = key_owner;
		this.key_value = public_key;
		this.created = created;
		this.expires = expires; }

	// Getters
	public long getId() { 	  			  return this.key_id; }
	public String getName() {			  return this.key_owner; }
	public void setName(String key_owner) {		 this.key_owner = key_owner; }
	public String getContent() { 		  return this.key_value; }
	public void setContent(String key_value) {	 this.key_value = key_value; }
	public Timestamp getCreated() {		  return this.created; }
	public Timestamp getExpires() {		  return this.expires; }

	// Utility methods
	@Override public String toJson() {
		return String.format("{\"name\":\"%s\",\"content\":\"%s\",\"created\":\"%s\",\"expires\":\"%s\"}",
							 this.key_owner, this.key_value, this.created, this.expires);
	}

	@Override public boolean equals(Object obj) {
		try { return ((PubKey) obj).getName().equalsIgnoreCase(this.key_owner); }
		catch(Exception e) { System.out.println(e.getMessage()); return false; }
	}
}
