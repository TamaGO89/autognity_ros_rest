package rest.pojo;

/* SECRET * Describe a secret: <key used to encrypt it, encrypted key> */
public class Secret extends RestObject {

	// instance variables
	private final long secret_id;
	private PubKey public_key;
	private String secret;

	// Constructors
	public Secret() { this.secret_id = 0; }
	public Secret(String secret) { this.secret_id = 0; this.secret = secret; }
	public Secret(String key_owner, String secret) {
		this.secret_id = 0; this.public_key = new PubKey(key_owner); this.secret = secret; }
	public Secret(long secret_id, String key_owner, String secret) {
		this.secret_id = secret_id; this.public_key = new PubKey(key_owner); this.secret = secret; }
	public Secret(long secret_id, PubKey public_key, String secret) {
		this.secret_id = secret_id; this.public_key = public_key; this.secret = secret; }

	// Getters
	public long getId() { 	  		  return this.secret_id; }
	public PubKey getKey() {		  return this.public_key; }
	public String getName() {		  return this.public_key.getName(); }
	public void setName(String key_owner) {	 this.public_key = new PubKey(key_owner); }
	public String getContent() { 	  return this.secret; }
	public void setContent(String secret) {	 this.secret = secret; }

	// Utility methods
	@Override public String toString() { return this.toJson(); }
	@Override public String toJson() {
		return String.format("{\"name\":\"%s\",\"content\":\"%s\"}", this.public_key.getName(), this.secret); }
	@Override public boolean equals(Object obj) {
		try { return ((Secret) obj).getContent().equalsIgnoreCase(this.secret); }
		catch(Exception e) { System.out.println(e.getMessage()); }
		return false;
	}
}