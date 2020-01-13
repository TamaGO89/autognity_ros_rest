package rest.pojo;

import java.sql.Timestamp;

/* USER * Rapresent a User with name, id, date of creation and expiry date */
public class User extends Dummy {

	private String firstname, lastname, address;
	
	public User() { super(); }
	public User(String name) { super(name); }
	public User(long id, String name, Timestamp created, Timestamp expired) { super(id, name, created, expired); }
	public User(long id, String name, Timestamp created, Timestamp expired, String firstname, String lastname, String address) {
		super(id, name, created, expired);
		this.firstname = firstname;
		this.lastname = lastname;
		this.address = address;
	}
	public User(long id, String firstname, String lastname, String address) {
		super(id);
		this.firstname = firstname;
		this.lastname = lastname;
		this.address = address;
	}
	
	public long getUser_id() {			  return this.id; }
	public String getFirstname() {		  return this.firstname; }
	public void setFirstname(String firstname) { this.firstname = firstname; }
	public String getLastname() {		  return this.lastname; }
	public void setLastname(String lastname) {	 this.lastname = lastname; }
	public String getAddress() {		  return this.address; }
	public void setAddress(String address) {	 this.address = address; }

	@Override public boolean equals(Object obj) {
		try { return super.equals((User) obj); }
		catch(Exception e) { System.out.println(e.getMessage()); return false; }
	}

	@Override public String toJson() {
		return String.format("{\"name\":\"%s\",\"created\":\"%s\",\"expires\":\"%s\",\"type\":\"user\",\"firstname\":\"%s\"," +
							 "\"lastname\":\"%s\",\"address\":\"%s\"}", this.name, this.created, this.expired,
							 this.firstname, this.lastname, this.address);
	}
}
