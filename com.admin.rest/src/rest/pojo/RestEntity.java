package rest.pojo;

public class RestEntity extends RestObject {
	
	private final String reason;

	public RestEntity(String reason) { this.reason = reason; }
	public RestEntity(int code) { this.reason = ""; }

	public String getReason() { return this.reason; }

	@Override public String toString() { return this.toJson(); }
	@Override public String toJson() { return String.format("{\"content\":\"%s\"}", this.reason); }
	@Override public boolean equals(Object obj) { 
		try { return ((RestEntity) obj).getReason() == this.reason; }
		catch(Exception e) { System.out.println(e.getMessage()); return false; }
	}
}
