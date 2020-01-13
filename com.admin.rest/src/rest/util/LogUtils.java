package rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AccountException;
import javax.security.auth.login.CredentialException;

import com.mysql.cj.xdevapi.Row;

import rest.pojo.Token;
import rest.provider.UserPrincipal;
import rest.util.CryptUtils;
import rest.util.DbMngr.DbMap;

/*==============================================================================================================================
 * AUTHENTICATION UTILITIES * Provide cryptographic functions, query the DB and Manage Maps for Violations and Tokens
 * TODO : Add send an email to the user if his account get violated several times
 *============================================================================================================================*/
public class LogUtils {

	// variables
	private static DbMngr db_mngr;
	// private static MailMngr mail_mngr;
	private static Map<String,Token> token_map = new HashMap<String,Token>();
	private static Map<String,Object> login_map;
	public static String AUTH, BASIC, SIGNIN, TOKEN, COOKIE, LOG_ID, LOG_SPLIT, LOG_SECRET, LOGGER;
	public static int LOG_MAX, TOKEN_EXPIRES, TOKEN_DELAY;
	public static short TOKEN_MEMORY;

	/*--------------------------------------------------------------------------------------------------------------------------
	 * MANAGE THE DATABASE * Private methods to manage the authentications' database
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Initial configuration: Put the properties for the DB and the connection
	public static void setDbMngr(DbMngr database_manager) { db_mngr = database_manager; }
	// public static void setMailMngr(MailMngr mail_manager) { mail_mngr = mail_manager; }
	private static List<String> getListID(String table, String field, Map<String,Object> wheres) {
		List<String> list = new ArrayList<String>();
		for(Row row : db_mngr.selectList(table, new String[] {field}, wheres))
			list.add(row.getString(field));
		return list;
	}
	// Private method to retrieve the User Principal
	private static UserPrincipal getUser(String user_hash) throws NullPointerException {
		Map<String,Object> log_wheres = new HashMap<String,Object>(1);
		log_wheres.put(DbMap.USERNAME, user_hash);
		Row row = db_mngr.selectRow(DbMap.V_LOGINS, DbMap.F_LOGIN, log_wheres);
		return new UserPrincipal(row.getLong(DbMap.ID_ROBOT), row.getLong(DbMap.ID_USER), row.getString(DbMap.NAME), user_hash,
								 CryptUtils.decodeToString(row.getString(DbMap.PASSWORD)), row.getInt(DbMap.VIOLATIONS),
								 row.getBoolean(DbMap.BANNED), row.getTimestamp(DbMap.CREATED), row.getTimestamp(DbMap.EXPIRES),
								 row.getString(DbMap.FIRSTNAME), row.getString(DbMap.LASTNAME), row.getString(DbMap.ADDRESS));
	}
	// Private method to update the user informations about Roles, Stations and Path associated and check for allowance
	private static void setUserInfo(UserPrincipal user_prin)
								 throws AccountException {
		Map<String,Object> id_wheres = new HashMap<String,Object>(1);
		id_wheres.put( user_prin.getID_field(), user_prin.getID() );
		user_prin.setRoles(getListID(DbMap.V_ROLES, DbMap.ROLE, id_wheres));
		user_prin.addRoles(getListID(DbMap.V_ROLES, DbMap.SUB, id_wheres));
		user_prin.setStations(getListID(DbMap.V_STATIONS, DbMap.STATION, id_wheres));
		user_prin.addStations(getListID(DbMap.V_STATIONS, DbMap.SUB, id_wheres));
	}
	// Private method to set the Token associated with the user
	private static void setUserToken(String username, UserPrincipal user_prin) {
		String token = CryptUtils.getRandomString();
		if (token_map.containsKey(user_prin.getUsername()))
			token_map.get(user_prin.getUsername()).updateToken(CryptUtils.getHash(token));
		else token_map.put(user_prin.getUsername(), new Token(CryptUtils.getHash(token), LogUtils.TOKEN_MEMORY));
		user_prin.setToken(CryptUtils.encodeToString(String.format("%s:%s", username, token).getBytes()));
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * PUBLIC METHODS * get clients informations at login and create the activity log
	 *------------------------------------------------------------------------------------------------------------------------*/
	// BASIC AUTHENTICATION : Get an ID from the DB, set the roles and set a new Token
	public static UserPrincipal checkUserPass(String userpass)
								throws CredentialException, AccountException, NullPointerException {
		String[] credentials = CryptUtils.decodeToString(userpass).split(":");
		String user_hash = CryptUtils.getHash(credentials[0]);
		UserPrincipal user_prin = getUser(user_hash);
		try {
			if (!CryptUtils.verifyPassword(user_prin.getPassword(), credentials[1]))
				throw new CredentialException(String.format("Password verification for %s failed", user_prin.getName()));
			if (user_prin.isBanned() || user_prin.getExpires().getTime() < System.currentTimeMillis())
				throw new AccountException(String.format("%s is not authorized to access:\n\tExpires in %s | %s",
														 user_prin.getName(), user_prin.getExpires(),
														 user_prin.isBanned() ? "Banned" : "not Banned"));
			// The DB manager is chosen based on the roles of the user and the roles allowed in the resource
			setUserInfo(user_prin);
			// Set the new token, to return as a cookie
			setUserToken(credentials[0], user_prin);
			return user_prin;
		} catch ( CredentialException | AccountException e ) {
			Map<String,Object> log_wheres = new HashMap<String,Object>(1);
			Map<String,Object> log_updates = new HashMap<String,Object>(1);
			log_wheres.put(DbMap.USERNAME, user_hash);
			log_updates.put(DbMap.VIOLATIONS, user_prin.addViolation());
			db_mngr.update(user_prin.isRobot() ? DbMap.T_ROBOTS : DbMap.T_USERS, log_updates, log_wheres);
			throw e;
		}
	}
	// COOKIE AUTHENTICATION : Get an ID from the DB, set the roles and update the Token
	public static UserPrincipal checkCookie(String cookie)
								throws NullPointerException, AccountException, CredentialException {
		String[] credentials = CryptUtils.decodeToString(cookie).split(":");
		String user_hash = CryptUtils.getHash(credentials[0]);
		UserPrincipal user_prin = getUser(user_hash);
		try {
			// DELAY and EXPIRES are expressed in minutes
			if (!token_map.get(user_hash).isToken(CryptUtils.getHash(credentials[1]),
												  (LogUtils.TOKEN_DELAY + LogUtils.TOKEN_EXPIRES) * 60000))
				throw new CredentialException(String.format("Token verification for %s failed", user_prin.getName()));
			if (user_prin.isBanned() || user_prin.getExpires().getTime() < System.currentTimeMillis())
				throw new AccountException(String.format("%s is not authorized to access:\n\tExpires on %s | %s",
														 user_prin.getName(), user_prin.getExpires(),
														 user_prin.isBanned() ? "Banned":"not Banned"));
			setUserInfo(user_prin);
			// Set the new token, to return as a cookie
			setUserToken(credentials[0], user_prin);
			return user_prin;
		} catch ( CredentialException | AccountException e ) {
			Map<String,Object> log_wheres = new HashMap<String,Object>(1);
			Map<String,Object> log_updates = new HashMap<String,Object>(1);
			log_wheres.put(DbMap.USERNAME, user_hash);
			log_updates.put(DbMap.VIOLATIONS, user_prin.addViolation());
			db_mngr.update(user_prin.isRobot() ? DbMap.T_ROBOTS : DbMap.T_USERS, log_updates, log_wheres);
			throw e;
		}
	}
	// VERIFICATION FOR NEW ACCOUNTS : Access the resource to update the password
	public static UserPrincipal checkVerification(String verification)
								throws NullPointerException, AccountException, CredentialException {
		UserPrincipal user_prin = getUser(CryptUtils.getHash(CryptUtils.decodeToString(verification)));
		if (user_prin.getExpires().getTime() != user_prin.getCreated().getTime()) {
			Map<String,Object> log_wheres = new HashMap<String,Object>(1);
			Map<String,Object> log_updates = new HashMap<String,Object>(1);
			log_wheres.put(DbMap.USERNAME, CryptUtils.getHash(verification));
			log_updates.put(DbMap.VIOLATIONS, user_prin.addViolation());
			db_mngr.update(user_prin.isRobot() ? DbMap.T_ROBOTS : DbMap.T_USERS, log_updates, log_wheres);
			throw new AccountException(user_prin.getName() + "got violated by the verification system");
		}
		else if (user_prin.isBanned() ||
				 user_prin.getExpires().getTime() < (System.currentTimeMillis() - CryptUtils.SIGNIN_EXPIRES * 60000)) {
			Map<String,Object> log_wheres = new HashMap<String,Object>(1);
			log_wheres.put(DbMap.USERNAME, CryptUtils.getHash(verification));
			db_mngr.delete(user_prin.isRobot() ? DbMap.T_ROBOTS : DbMap.T_USERS, log_wheres);
			throw new AccountException(String.format("%s is not authorized to access:\n\tExpires in %s | %s",
													 user_prin.getName(), user_prin.getExpires(),
													 user_prin.isBanned() ? "Banned":"not Banned"));
		}
		setUserInfo(user_prin);
		return user_prin;
	}
	public static void checkPermissions(UserPrincipal user_prin, List<String> roles_allowed, List<String> stations_allowed)
			 throws AccountException {
		List<String> roles = new ArrayList<String>(),
					 stations = new ArrayList<String>();
		for (String role : roles_allowed) if (user_prin.isRole(role)) roles.add(role);
		for (String station: stations_allowed) if (user_prin.isStation(station)) stations.add(station);
		if ((!roles_allowed.isEmpty() && roles.isEmpty()) || (!stations_allowed.isEmpty() && stations.isEmpty()))
		throw new AccountException( String.format("Access denied for %s:\n\tUser roles are %s\n\tAllowed roles are %s" +
												  "\n\tUser stations are %s\n\tAllowed stations are %s",
												  user_prin.getName(), user_prin.getRoles(), roles_allowed,
												  user_prin.getStations(), stations_allowed));
		user_prin.setDbMngr( DbMngr.getDbMngr(roles.isEmpty() ? user_prin.getRoles() : roles) );
	}
	// GET REQUEST ID : Get an ID that allow future synchronization between user requests and activity log
	public static String getID(UserPrincipal user_prin, List<String> logger) {
		String id = CryptUtils.getRandomString(DbMap.LOG_LENGTH);
		// Start the assembling of the content: IP address, path, query params
		String content = String.join(LogUtils.LOG_SPLIT, logger);
		Map<String,Object> fields = new HashMap<String,Object>(3);
		fields.put(user_prin.getID_field(), user_prin.getID());
		fields.put(DbMap.ID, id);
		fields.put(DbMap.CONTENT,
				   CryptUtils.encodeToString(content.substring(0, Math.min(content.length(), LogUtils.LOG_MAX)).getBytes()));
		// TODO: Remove the standard output log
		System.out.println(content.substring(0, Math.min(content.length(), LogUtils.LOG_MAX)));
		db_mngr.insert(user_prin.getID_field().contentEquals(DbMap.ID_ROBOT) ? DbMap.T_RLOG : DbMap.T_ULOG, fields);
		return id;
	}
	// Get a user profile, might be useless but like this any device that connects to the system gets a a server-side ID
	public static UserPrincipal getGuestUser() { return new UserPrincipal(); }
	public static UserPrincipal getGuestUser(Long id, String db_name) { return new UserPrincipal(id, "Guest", db_name); }

	/*--------------------------------------------------------------------------------------------------------------------------
	 * UTILITY METHODS * To setup the properties' map
	 *------------------------------------------------------------------------------------------------------------------------*/
	private static String getStr(String key) { return (String) login_map.get(key); }
	private static int getInt(String key) { return (int) login_map.get(key); }
	private static short getShort(String key) { return new Integer((int) login_map.get(key)).shortValue(); }
	public static void setLoginMap(Map<String,Object> map) {
		login_map = map;
		// setup public variables
		AUTH = getStr("auth");
		BASIC = getStr("basic");
		SIGNIN = getStr("signin");
		TOKEN = getStr("token");
		COOKIE = getStr("cookie");
		LOG_ID = getStr("id");
		LOGGER = getStr("logger");
		LOG_SPLIT = getStr("split");
		LOG_MAX = getInt("log_max");
		LOG_SECRET = getStr("confidential");
		TOKEN_EXPIRES = getInt("token_expires");
		TOKEN_DELAY = getInt("token_delay");
		TOKEN_MEMORY = getShort("token_memory");
	}
}
