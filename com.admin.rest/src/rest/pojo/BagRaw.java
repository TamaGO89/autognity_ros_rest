package rest.pojo;

/* BAG RAW DATA * Class that contains the raw data of the BAG, the number of fragments is specified by the BagInfo count */
public class BagRaw extends RestObject {

	// Instance variables
	private long bag_id;
	private boolean last = false;
	private String content, hash;

	// Constructor
	public BagRaw() {}
	public BagRaw(String content) { this.content = content; }
	public BagRaw(long bag_id, String content) { this.bag_id = bag_id; this.content = content; }
	
	// Getters and Setters
	public long getId() {		  return this.bag_id; }
	public void setId(long bag_id) {	 this.bag_id = bag_id; }
	public String getContent() {	  return this.content; }
	public void setContent(String content) { this.content = content; }
	public String getHash() {		  return this.hash; }
	public void setHash(String hash) {		 this.hash = hash; }
	public boolean isLast() {		  return this.last; }
	public void setLast(boolean last) {		 this.last = last; }
	
	// Utility methods
	@Override public String toString() { return this.toJson(); }
	@Override public String toJson() { return String.format("{\"id\":%d,\"content\":\"%s\"}", this.bag_id, this.content); }
	@Override public boolean equals(Object obj) {
		try { return ((BagRaw) obj).getId() == this.bag_id; }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
}