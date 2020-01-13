package rest.pojo;

import java.sql.Timestamp;

/* ROBOT * Rapresent a robot with name, id, date of creation and expiry date */
public class Robot extends Dummy {

	private User user;
	
	public Robot() { super(); this.user = new User();}
	public Robot(String name) { super(name); this.user = new User(); }
	public Robot(long id, String name, Timestamp created, Timestamp expired) {
		super(id, name, created, expired);
		this.user = new User();
	}
	public Robot(long id, String name, Timestamp created, Timestamp expired, User user) {
		super(id, name, created, expired);
		this.user = user;
	}

	public long getRobot_id() { return this.id; }
	public User getUser() {		return this.user; }
	public void setUser(User user) {   this.user = user; }

	@Override public boolean equals(Object obj) {
		try { return super.equals((Robot) obj); }
		catch(Exception e) { System.out.println(e.getMessage()); return false; }
	}

	@Override public String toJson() {
		return String.format("{\"name\":\"%s\",\"created\":\"%s\",\"expires\":\"%s\",\"type\":\"robot\",\"registrant\":%s}",
							 this.name, this.created, this.expired, this.user.toJson());
	}
}
