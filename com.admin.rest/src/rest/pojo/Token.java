package rest.pojo;

import java.util.ArrayList;
import java.util.List;

public class Token {
	private static final short MEMORY = 5;
	private final short memory;
	private final List<String> tokens;
	private final List<Long> timestamps;
	public Token(String token) {
		this.memory = Token.MEMORY;
		this.tokens = new ArrayList<String>(this.memory);
		this.timestamps = new ArrayList<Long>(this.memory);
		this.tokens.add(token); this.timestamps.add(System.currentTimeMillis()); }
	public Token(String token, short memory) {
		this.memory = memory;
		this.tokens = new ArrayList<String>(this.memory);
		this.timestamps = new ArrayList<Long>(this.memory);
		this.tokens.add(token); this.timestamps.add(System.currentTimeMillis()); }
	public void updateToken(String token) {
		if (this.tokens.size() >= this.memory) { this.tokens.remove(0); this.timestamps.remove(0); }
		this.tokens.add(token); this.timestamps.add(System.currentTimeMillis());}
	public boolean isToken(String token, int delay) {
		try { return this.timestamps.get(this.tokens.indexOf(token)) >= System.currentTimeMillis() - delay; }
		catch (Exception ignored) { return false; }
	}
}
