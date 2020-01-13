package rest.util;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.security.auth.login.CredentialException;

import com.mysql.cj.protocol.x.XProtocolError;
import com.mysql.cj.xdevapi.Row;

import rest.pojo.BagConfig;
import rest.pojo.BagInfo;
import rest.pojo.BagRaw;
import rest.pojo.ConfigContainer;
import rest.pojo.DummyContainer;
import rest.pojo.PubKey;
import rest.pojo.RestMap;
import rest.pojo.RestNode;
import rest.pojo.RestRes;
import rest.pojo.Robot;
import rest.pojo.Secret;
import rest.pojo.Station;
import rest.pojo.Role;
import rest.pojo.User;
import rest.util.DbMngr.DbMap;

/*==============================================================================================================================
 * SESSION UTILITIES * General utilities used over the entire web service
 *============================================================================================================================*/
public class SessionUtils {

	// variables
	private static List<RestRes> paths = new ArrayList<RestRes>();
	private static MailMngr mail_mngr;

	/*--------------------------------------------------------------------------------------------------------------------------
	 * INITIAL SETUP METHODS * Initialize the email manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	public static void setMailMngr(MailMngr mail_manager) { mail_mngr = mail_manager; }

	/*--------------------------------------------------------------------------------------------------------------------------
	 * CLIENT CONFIG METHODS * Getters for the map of the website and other useful informations for the client 
	 *------------------------------------------------------------------------------------------------------------------------*/
	public static RestMap getPaths(DummyContainer dummy_container) {
		RestMap result = new RestMap();
		for (RestRes path : SessionUtils.paths)
			if ((path.isRole(dummy_container.getRoles()) || path.getRoles().isEmpty()) &&
				(path.isStation(dummy_container.getStations()) || path.getStations().isEmpty()))
			result.set(path.getKey(), path.getPath());
		return result;
	}
	public static void addPath(String key, String path, List<String> roles, List<String> stations) {
		paths.add(new RestRes(key, path, roles, stations)); }

	/*--------------------------------------------------------------------------------------------------------------------------
	 * CLIENT CONFIG METHODS * Getters for the map of the website and other useful informations for the client 
	 *------------------------------------------------------------------------------------------------------------------------*/
	public static Map<String,String> getBagOverview(DbMngr db_mngr, long id, String id_field, long from_ts,
													long to_ts, String robot) throws NullPointerException {
		Map<Long,Robot> robots = SessionUtils.getRobots(db_mngr, id, id_field);
		long id_robot = 0;
		for (Robot rob : robots.values()) if (rob.getName().contentEquals(robot)) {
			id_robot = rob.getID();	break; }
		if (id_robot == 0) throw new NullPointerException("robot not found");
		Row row = db_mngr.selectRow(DbMap.V_CONFIGS, DbMap.F_CONFIG, new Object[] { id_robot },
									String.format("%s LIKE :0", DbMap.ID_ROBOT));
		ConfigContainer config_container = new ConfigContainer(
												   new BagConfig(row.getLong(DbMap.ID_CONFIG),row.getString(DbMap.USER),
																 row.getString(DbMap.CONTENT),row.getTimestamp(DbMap.CREATED)));
		for (Row r : db_mngr.selectList(DbMap.V_CONFIGS, new String[] {DbMap.ID_ROBOT},
										new Object[] { row.getLong(DbMap.ID_CONFIG), new ArrayList<Long>(robots.keySet()) },
										String.format("%s LIKE :0 AND %s IN :1", DbMap.ID_CONFIG, DbMap.ID_ROBOT)))
			config_container.addRobot(robots.get(r.getLong(DbMap.ID_ROBOT)));
		// Retrieve the data 
		List<Row> rows = db_mngr.selectList(DbMap.V_INFOS, DbMap.F_INFO,
											new Object[] {new Timestamp(Math.max(DbMap.MIN_LONG, from_ts)).toString(),
														  new Timestamp(Math.min(DbMap.MAX_LONG, to_ts)).toString(), id_robot },
											String.format("%s BETWEEN :0 AND :1 AND %s LIKE :2", DbMap.CREATED,DbMap.ID_ROBOT));
		List<String> bags = new ArrayList<String>();
		List<Secret> secrets;
		BagInfo bag;
		for (Row r : rows) {
			// If there are no corresponding keys the data can't be retrieved
			secrets = getSecrets(db_mngr, r.getLong(DbMap.ID_INFO), id, id_field);
			if (secrets.isEmpty()) continue;
			bag = new BagInfo(r.getLong(DbMap.ID_INFO), robots.get(r.getLong(DbMap.ID_ROBOT)),
							  getCfg(db_mngr, DbMap.ID_ROBOT, r.getLong(DbMap.ID_ROBOT), r.getLong(DbMap.ID_CONFIG)),
							  r.getString(DbMap.CONTENT), r.getTimestamp(DbMap.CREATED));
			bag.setSecrets(secrets);
			bags.add(bag.toJson());
		}
		// Prepare the Map with the results
		Map<String,String> rest_map = new HashMap<String,String>();
		rest_map.put("content", config_container.toJson());
		rest_map.put("list", String.format("[%s]", String.join(",", bags)));
		return rest_map;
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * BAG CONFIG METHODS * Getters and Setters for BAG configurations that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Get a Configuration based on time-stamp, retrieve the most recent one (The view that this method use is already sorted)
	public static ConfigContainer getBagConfig(DbMngr db_mngr, long id, String id_field, long timestamp, boolean keys)
								  throws NullPointerException {
		Row row = db_mngr.selectRow(DbMap.V_CONFIGS, DbMap.F_CONFIG, new Object[] { id, new Timestamp(timestamp) },
									String.format("%s LIKE :0 AND %s > :1", id_field, DbMap.CREATED));
		ConfigContainer config_container = new ConfigContainer(
												   new BagConfig(row.getLong(DbMap.ID_CONFIG),row.getString(DbMap.USER),
																 row.getString(DbMap.CONTENT),row.getTimestamp(DbMap.CREATED)));
		if (keys) config_container.setPublic_keys(SessionUtils.getKeys(db_mngr, id_field, id));
		Map<Long,Robot> robots = SessionUtils.getRobots(db_mngr, id, id_field);
		for (Row r : db_mngr.selectList(DbMap.V_CONFIGS, new String[] {DbMap.ID_ROBOT},
										new Object[] { row.getLong(DbMap.ID_CONFIG), new ArrayList<Long>(robots.keySet()) },
										String.format("%s LIKE :0 AND %s IN :1", DbMap.ID_CONFIG, DbMap.ID_ROBOT)))
			config_container.addRobot(robots.get(r.getLong(DbMap.ID_ROBOT)));
		return config_container;
	}
	// Set a Configuration based on a ConfigContainer, retrieve the robot list, the key list, compare the authorized ones
	public static ConfigContainer setBagConfig(DbMngr db_mngr, long id, String id_field,
											   ConfigContainer config_container) throws NullPointerException {
		// Retrieve the IDs and check for mismatch
		Map<Long,Robot> robots = getRobots(db_mngr, id, id_field);
		for (long key : new ArrayList<Long>(robots.keySet()))
			if (!config_container.getRobots().contains(robots.get(key)))
				robots.remove(key);
		if (!robots.values().containsAll(config_container.getRobots()))
			throw new NullPointerException(String.format("%s %d can't edit these robots", id_field, id));
		// Execute the insertion of the needed rows
		long config_id = setCfg(db_mngr, id_field, id, config_container.getConfig());
		Map<String,Object> fields = new HashMap<String,Object>();
		fields.put(DbMap.ID_CONFIG, config_id);
		for (Robot robot : robots.values()) {
			fields.put(DbMap.ID_ROBOT, robot.getID());
			db_mngr.insert(DbMap.X_ROBOT_CONFIG, fields);
		}
		return new ConfigContainer(getCfg(db_mngr,id_field,id,config_id), new ArrayList<Robot>(robots.values()));
	}
	// Used by the two methods above to upload the configurations to the DB
	private static long setCfg(DbMngr db_mngr, String id_field, long id, BagConfig bag_config) throws XProtocolError {
		Map<String,Object> fields = new HashMap<String,Object>();
		fields.put(DbMap.CONTENT, bag_config.getContent());
		fields.put(id_field, id);
		return db_mngr.insertID(DbMap.T_CONFIG, fields);
	}
	// Used internally by getBagInfo to retrieve the corresponding setting
	private static BagConfig getCfg(DbMngr db_mngr, String id_field, long id, long config_id) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(DbMap.ID_CONFIG, config_id);
		wheres.put(id_field, id);
		Row row = db_mngr.selectRow(DbMap.V_CONFIGS, DbMap.F_CONFIG, wheres);
		return new BagConfig(row.getLong(DbMap.ID_CONFIG), row.getString(DbMap.USER),
							 row.getString(DbMap.CONTENT), row.getTimestamp(DbMap.CREATED));
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * BAG INFORMATIONS METHODS * Getters and Setters for BAG informations that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Upload a bag info to the DB
	public static BagInfo setBagInfo(DbMngr db_mngr, long id, String id_field, BagInfo bag_info) throws NullPointerException {
		// Get the configuration referred by the informations and the keys relative to the configuration and the robot 
		Map<String,Object> fields = new HashMap<String,Object>(3);
		BagConfig bag_config = getCfg(db_mngr, id_field, id, bag_info.getConfig_id());
		List<String> key_owners = new ArrayList<String>();
		for (PubKey key : getKeys(db_mngr, id_field, id)) key_owners.add(key.getName());
		for (Secret secret : bag_info.getSecrets()) if (!key_owners.remove(secret.getName()))
			throw new NullPointerException("secrets missmatch for ID "+id);
		// Upload the new information scheme and proceed to upload the secrets
		fields.put(id_field, id);
		fields.put(DbMap.ID_CONFIG, bag_config.getConfig_id());
		fields.put(DbMap.CONTENT, bag_info.getContent());
		fields.put(DbMap.HASH, "");
		long bag_id = db_mngr.insertID(DbMap.T_INFOS, fields);
		PubKey public_key;
		fields.clear();
		fields.put(DbMap.ID_INFO, bag_id);
		for (Secret secret : bag_info.getSecrets()) {
			public_key = getK(db_mngr, secret.getName());
			fields.put(DbMap.ID_USER, public_key.getId());
			fields.put(DbMap.CONTENT, secret.getContent());
			db_mngr.insert(DbMap.T_SECRETS, fields);
		}
		bag_info.setConfig(bag_config);
		return new BagInfo(bag_id, getRobots(db_mngr, id, id_field).get(id), bag_config, bag_info.getContent(),
											 bag_info.getSecrets(), new Timestamp(0));
	}
	// Download a list of bags, starting from a time stamp to the latest one
	public static List<BagInfo> getBagInfo(DbMngr db_mngr,long id,String id_field,long timestamp) throws NullPointerException {
		return getBagInfo(db_mngr, id, id_field, timestamp, DbMap.MAX_LONG);
	}
	// Download a list of bags, with time window specified by (from_ts,to_ts)
	public static List<BagInfo> getBagInfo(DbMngr db_mngr, long id, String id_field, long from_ts, long to_ts)
								throws NullPointerException {
		// Retrieve user Robots and prepare the statement
		Map<Long,Robot> robots = getRobots(db_mngr, id, id_field);
		List<Long> robot_ids = new ArrayList<Long>(robots.size());
		for (Robot robot : robots.values()) robot_ids.add(robot.getRobot_id());
		// Retrieve the data 
		List<Row> rows = db_mngr.selectList(DbMap.V_INFOS, DbMap.F_INFO,
											new Object[] {new Timestamp(Math.max(DbMap.MIN_LONG, from_ts)).toString(),
													  	  new Timestamp(Math.min(DbMap.MAX_LONG, to_ts)).toString(), robot_ids},
											String.format("%s BETWEEN :0 AND :1 AND %s IN :2", DbMap.CREATED, DbMap.ID_ROBOT));
		List<BagInfo> bags = new ArrayList<BagInfo>();
		List<Secret> secrets;
		for (Row row : rows) {
			// If there are no corresponding keys the data can't be retrieved
			secrets = getSecrets(db_mngr, row.getLong(DbMap.ID_INFO), id, id_field);
			if (secrets.isEmpty()) continue;
			bags.add(new BagInfo(row.getLong(DbMap.ID_INFO), robots.get(row.getLong(DbMap.ID_ROBOT)),
								 getCfg(db_mngr, DbMap.ID_ROBOT, row.getLong(DbMap.ID_ROBOT), row.getLong(DbMap.ID_CONFIG)),
								 row.getString(DbMap.CONTENT), row.getTimestamp(DbMap.CREATED)));
			bags.get(bags.size() - 1).setSecrets(secrets);
		}
		return bags;
	}
	// Get the secrets associated with the requested BAG, within the key possessed by the User asking for them
	private static List<Secret> getSecrets(DbMngr db_mngr, long bag_id, long id, String id_field) throws NullPointerException {
		// The secrets that the user can read are only those corresponding to private key possessed by the user
		List<Row> rows = db_mngr.selectList(DbMap.V_SECRETS, DbMap.F_SECRET, new Object[] { bag_id, id },
											String.format("%s LIKE :0 AND %s LIKE :1", DbMap.ID_INFO, id_field));
		List<Secret> secrets = new ArrayList<Secret>(rows.size());
		for (Row row : rows)
			secrets.add(new Secret(row.getLong(DbMap.ID_SECRET), row.getString(DbMap.NAME), row.getString(DbMap.CONTENT)));
		return secrets;
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * BAG RAW DATA METHODS * Getters and Setters for BAG raw data that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Upload the chunk of a bag to the DB if the sequence number matches
	public static BagRaw setBagRaw(DbMngr db_mngr, long id, String id_field, BagRaw bag_raw) throws NullPointerException,
																									IOException {
		// Check the HASH of the BAG_ID created by ROBOT_ID still under going (CREATED = MIN_TIME)
		Object[] wheres = new Object[] { id, bag_raw.getId(), DbMap.MIN_TIME };
		String where_conditions = String.format("%s LIKE :0 AND %s LIKE :1 AND %s <= :2",
												id_field, DbMap.ID_INFO, DbMap.CREATED);
		Row bag_info = db_mngr.selectRow(DbMap.V_INFOS, new String[] {DbMap.ROBOT, DbMap.HASH}, wheres, where_conditions);
		if (!CryptUtils.compareHash(bag_raw.getHash(),CryptUtils.getHash(bag_info.getString(DbMap.HASH),bag_raw.getContent())))
			throw new NullPointerException("BAG_ID " + bag_raw.getId() + " hash doesn't match!");
		// Insert the new raw data and update the info with the new HASH and eventually the final time stamp
		FileMngr.upload(bag_info.getString(DbMap.ROBOT), bag_raw.getId(), CryptUtils.decodeToString(bag_raw.getContent()));
		Map<String,Object> fields = new HashMap<String,Object>(1);
		fields.put(DbMap.HASH, bag_raw.getHash());
		if (bag_raw.isLast()) fields.put(DbMap.CREATED, new Timestamp(System.currentTimeMillis()));
		db_mngr.update(DbMap.T_INFOS, fields, wheres, where_conditions);
		return bag_raw;
	}
	// Download bag chunks from the DB
	public static BagRaw getBagRaw(DbMngr db_mngr, long id, String id_field, long bag_id) throws NullPointerException,
																								 IOException {
		// Retrieve BAG INFOS with BAG_ID, check that the user has at least one of the secrets and owns the robot
		if (db_mngr.count(DbMap.V_SECRETS, new Object[] { bag_id, id },
						  String.format("%s LIKE :0 AND %s LIKE :1", DbMap.ID_INFO, id_field)) == 0)
			throw new NullPointerException(id_field + " " + id + " has no secrets associated with bag_id " + bag_id);
		List<Long> robot_ids = new ArrayList<Long>();
		for (Robot robot : getRobots(db_mngr, id, id_field).values()) robot_ids.add(robot.getRobot_id());
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(DbMap.ID_INFO, bag_id);
		return new BagRaw(bag_id, CryptUtils.encodeToString(FileMngr.download(
				db_mngr.selectRow(DbMap.V_INFOS, new String[]{DbMap.ROBOT}, new Object[]{bag_id, robot_ids},
						String.format("%s LIKE :0 AND %s IN :1",DbMap.ID_INFO,DbMap.ID_ROBOT)).getString(DbMap.ROBOT),bag_id)));
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * KEY METHODS * Getters and Setters for KEYS, that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Get the keys with a specific key_owner
	public static List<PubKey> getKeys(DbMngr db_mngr, List<PubKey> pub_keys) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		Row row;
		List<PubKey> public_keys = new ArrayList<PubKey>(pub_keys.size());
		for (PubKey public_key : pub_keys) {
			wheres.put(DbMap.NAME, public_key.getName());
			row = db_mngr.selectRow(DbMap.V_KEYS, DbMap.F_KEY, wheres);
			public_keys.add(new PubKey(row.getLong(DbMap.ID_USER),row.getString(DbMap.NAME),row.getString(DbMap.PUBLIC_KEY),
									   row.getTimestamp(DbMap.CREATED),row.getTimestamp(DbMap.EXPIRES)));
		}
		return public_keys;
	}
	// Used internally to retrieve the keys, matching a specific field
	public static List<PubKey> getKeys(DbMngr db_mngr, String id_field, long id_value) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(id_field,id_value);
		List<Long> roles = db_mngr.selectListID(DbMap.V_ROLES, DbMap.ID_ROLE, wheres);
		List<Long> stations = db_mngr.selectListID(DbMap.V_STATIONS, DbMap.ID_STATION, wheres);
		List<Long> ids = new ArrayList<Long>();
		for (Row row : db_mngr.selectList(DbMap.U_ROLES, new String[] {DbMap.ID_USER}, new Object[] {roles},
										  String.format("%s IN :0", DbMap.ID_SUB)))
			if (!ids.contains(row.getLong(DbMap.ID_USER))) ids.add(row.getLong(DbMap.ID_USER));
		List<Long> owners = new ArrayList<Long>();
		for (Row row : db_mngr.selectList(DbMap.U_STATIONS, new String[] {DbMap.ID_USER}, new Object[] {ids,stations,stations},
										  String.format("%s IN :0 AND (%s IN :1 OR %s IN :2)",
														DbMap.ID_USER, DbMap.ID_SUB, DbMap.ID_STATION)))
			if (!owners.contains(row.getLong(DbMap.ID_USER))) owners.add(row.getLong(DbMap.ID_USER));
		List<Row> rows = db_mngr.selectList(DbMap.V_KEYS, DbMap.F_KEY, new Object[] { System.currentTimeMillis(), owners },
											String.format("%s > :0 AND %s IN :1", DbMap.EXPIRES, DbMap.ID_USER));
		List<PubKey> public_keys = new ArrayList<PubKey>(rows.size());
		for(Row row : rows)
			public_keys.add(new PubKey(row.getLong(DbMap.ID_USER),row.getString(DbMap.NAME),row.getString(DbMap.PUBLIC_KEY),
									   row.getTimestamp(DbMap.CREATED), row.getTimestamp(DbMap.EXPIRES)));
		return public_keys;
	}
	// Used internally to retrieve the keys, matching a specific field
	private static PubKey getK(DbMngr db_mngr, String key_owner) throws NullPointerException {
		Row row = db_mngr.selectRow(DbMap.V_KEYS, DbMap.F_KEY, new Object[] { key_owner, System.currentTimeMillis() },
									String.format("%s LIKE :0 AND %s > :1", DbMap.NAME, DbMap.EXPIRES));
		return new PubKey(row.getLong(DbMap.ID_USER), row.getString(DbMap.NAME), row.getString(DbMap.PUBLIC_KEY),
						  row.getTimestamp(DbMap.CREATED), row.getTimestamp(DbMap.EXPIRES));
	}
	// Insert a new Key
	public static PubKey setKey(DbMngr db_mngr, String id_field, long id, PubKey key)
								throws InvalidKeySpecException, NoSuchAlgorithmException, NullPointerException {
		Map<String,Object> fields = new HashMap<String,Object>(1);
		fields.put(DbMap.PUBLIC_KEY, CryptUtils.verifyPublicKey(key.getContent()));
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		// TODO : this method must be revisited, it's just a big mess
		if (key.getName() != null) wheres.put(DbMap.NAME, key.getName());
		else wheres.put(id_field, id);
		Row key_owner = db_mngr.selectRow(DbMap.V_CLIENTS, new String[] {DbMap.ID_ROBOT, DbMap.ID_USER}, wheres);
		Map<String,Object> w = new HashMap<String,Object>(1);
		w.put(id_field, id);
		// If the key owner and the user mismatch and the key owner is not a machine registered by the user
		if (id != key_owner.getLong(id_field)) {
			String owner_field = key_owner.getLong(DbMap.ID_ROBOT) > 0 ? DbMap.ID_ROBOT : DbMap.ID_USER;
			long owner_id = key_owner.getLong(owner_field);
			List<Long> roles = db_mngr.selectListID(DbMap.V_ROLES, DbMap.ID_ROLE, w);
			List<Long> stations = db_mngr.selectListID(DbMap.V_STATIONS, DbMap.ID_STATION, w);
			// Check that the owner of the key is controlled by the user and that the current key is null
			if (db_mngr.selectList(DbMap.V_ROLES, new String[] {owner_field}, new Object[] {owner_id, roles},
								   String.format("%s LIKE :0 AND %s IN :1", owner_field, DbMap.ID_SUB)).isEmpty()
				|| db_mngr.selectList(DbMap.V_STATIONS, new String[] {owner_field}, new Object[] {owner_id,stations,stations},
									  String.format("%s LIKE :0 AND (%s IN :1 OR %s IN :2)",
													owner_field, DbMap.ID_SUB, DbMap.ID_STATION)).isEmpty()
				|| db_mngr.selectRow(DbMap.V_KEYS, new String[] {owner_field}, wheres) != null)
				throw new NullPointerException (String.format("%s %d is not controlled by %s %d or the key is already set",
															  owner_field, owner_id, id_field, id));
		} else if (!db_mngr.selectList(DbMap.V_SECRETS, new String[] {DbMap.ID_SECRET}, w).isEmpty())
			throw new NullPointerException (String.format("%s %d has secrets associated with his key", id_field, id));
		db_mngr.update(key_owner.getLong(DbMap.ID_ROBOT) > 0 ? DbMap.T_ROBOTS : DbMap.T_USERS, fields, wheres);
		return key;
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * ROBOT-USER METHODS * Getters and Setters for ROBOTS and USERS, that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Get a list of robots for the given user
	private static Map<Long,Robot> getRobots(DbMngr db_mngr, long id, String id_field) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(id_field,id);
		if (id_field.contentEquals(DbMap.ID_ROBOT)) {
			Row row = db_mngr.selectRow(DbMap.V_CLIENTS, DbMap.F_CLIENT, wheres);
			Map<Long,Robot> robots = new HashMap<Long,Robot>(1);
			robots.put(id, new Robot(row.getLong(DbMap.ID_ROBOT), row.getString(DbMap.NAME),
									 row.getTimestamp(DbMap.CREATED), row.getTimestamp(DbMap.EXPIRES)));
			return robots;
		}
		List<Long> roles = db_mngr.selectListID(DbMap.V_ROLES, DbMap.ID_ROLE, wheres);
		List<Long> stations = db_mngr.selectListID(DbMap.V_STATIONS, DbMap.ID_STATION, wheres);
		List<Row> rows = db_mngr.selectList(DbMap.R_ROLES, new String[] {DbMap.ID_ROBOT}, new Object[] {roles},
				   							String.format("%s IN :0", DbMap.ID_SUPER));
		List<Long> ids = new ArrayList<Long>(rows.size());
		for (Row row : rows) if (!ids.contains(row.getLong(DbMap.ID_ROBOT))) ids.add(row.getLong(DbMap.ID_ROBOT));
		rows = db_mngr.selectList(DbMap.R_STATIONS, new String[] {DbMap.ID_ROBOT}, new Object[] {ids, stations, stations},
								  String.format("%s IN :0 AND (%s IN :1 OR %s IN :2)",
												DbMap.ID_ROBOT, DbMap.ID_SUPER, DbMap.ID_STATION));
		ids.clear();
		for (Row row : rows) if (!ids.contains(row.getLong(DbMap.ID_ROBOT))) ids.add(row.getLong(DbMap.ID_ROBOT));
		rows = db_mngr.selectList(DbMap.V_CLIENTS,DbMap.F_CLIENT,new Object[]{ids},String.format("%s IN :0", DbMap.ID_ROBOT));
		Map<Long,Robot> robots = new HashMap<Long,Robot>(rows.size());
		Row r;
		for (Row row : rows) {
			r = db_mngr.selectRow(DbMap.V_CLIENTS, DbMap.F_CLIENT, new Object[] {row.getLong(DbMap.ID_USER)},
								  String.format("%s IS NULL AND %s LIKE :0", DbMap.ID_ROBOT, DbMap.ID_USER));
			robots.put(row.getLong(DbMap.ID_ROBOT), new Robot(row.getLong(DbMap.ID_ROBOT), row.getString(DbMap.NAME),
															  row.getTimestamp(DbMap.CREATED),row.getTimestamp(DbMap.EXPIRES),
															  new User(r.getLong(DbMap.ID_USER), r.getString(DbMap.NAME),
																	   r.getTimestamp(DbMap.CREATED),
																	   r.getTimestamp(DbMap.EXPIRES),
																	   r.getString(DbMap.FIRSTNAME),r.getString(DbMap.LASTNAME),
																	   r.getString(DbMap.ADDRESS))));
		}
		return robots;
	}
	// Populate a dummy container with his roles and stations
	public static DummyContainer getDummy(DbMngr db_mngr, DummyContainer dummy) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(dummy.getID_field(), dummy.getID());
		DummyContainer dummy_container = new DummyContainer(dummy.getDummy());
		for (Row r : db_mngr.selectList(DbMap.V_ROLES, new String[] {DbMap.ROLE}, wheres))
			if (!dummy_container.getRoles().contains(r.getString(DbMap.ROLE)))
				dummy_container.addRole(r.getString(DbMap.ROLE));
		for (Row r : db_mngr.selectList(DbMap.V_STATIONS, new String[] {DbMap.STATION}, wheres))
			if (!dummy_container.getStations().contains(r.getString(DbMap.STATION)))
				dummy_container.addStation(r.getString(DbMap.STATION));
		return dummy_container;
	}
	// Retrieve a list of users based on stations and roles
	public static List<DummyContainer> getUserList(DbMngr db_mngr, long id, String id_field) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(id_field,id);
		List<Long> roles = db_mngr.selectListID(DbMap.V_ROLES, DbMap.ID_ROLE, wheres);
		List<Long> stations = db_mngr.selectListID(DbMap.V_STATIONS, DbMap.ID_STATION, wheres);
		Map<Long,DummyContainer> users = new HashMap<Long,DummyContainer>();
		// The results are ordered by ID, this simplify the code a little
		for (Row row : db_mngr.selectList(DbMap.U_ROLES, new String[] {DbMap.ID_USER, DbMap.ROLE},
										   new Object[] {roles}, String.format("%s IN :0", DbMap.ID_SUPER))) {
			if (!users.containsKey(row.getLong(DbMap.ID_USER)))
				users.put(row.getLong(DbMap.ID_USER), new DummyContainer());
			users.get(row.getLong(DbMap.ID_USER)).addRole(row.getString(DbMap.ROLE));
		}
		for (Row row : db_mngr.selectList(DbMap.U_STATIONS, new String[] {DbMap.ID_USER, DbMap.STATION},
										  new Object[] {new ArrayList<Long>(users.keySet()), stations, stations},
										  String.format("%s IN :0 AND (%s IN :1 OR %s IN :2)",
														DbMap.ID_USER, DbMap.ID_SUPER, DbMap.ID_STATION)))
			users.get(row.getLong(DbMap.ID_USER)).addStation(row.getString(DbMap.STATION));
		for (long key : new ArrayList<Long>(users.keySet()))
			if (users.get(key).getStations().isEmpty()) users.remove(key);
		for (Row row : db_mngr.selectList(DbMap.V_CLIENTS, DbMap.F_CLIENT, new Object[]{new ArrayList<Long>(users.keySet())},
										  String.format("%s IS NULL AND %s IN :0", DbMap.ID_ROBOT, DbMap.ID_USER))) {
			users.get(row.getLong(DbMap.ID_USER)).setDummy(new User(row.getLong(DbMap.ID_USER), row.getString(DbMap.NAME),
																	row.getTimestamp(DbMap.CREATED),
																	row.getTimestamp(DbMap.EXPIRES),
																	row.getString(DbMap.FIRSTNAME),
																	row.getString(DbMap.LASTNAME),
																	row.getString(DbMap.ADDRESS)));
			users.get(row.getLong(DbMap.ID_USER)).setBanned(row.getBoolean(DbMap.BANNED));
			users.get(row.getLong(DbMap.ID_USER)).setViolations(row.getInt(DbMap.VIOLATIONS));
		}
		return new ArrayList<DummyContainer>(users.values());
	}
	// Retrieve a list of robots based on stations and roles
	public static List<DummyContainer> getRobotList(DbMngr db_mngr, long id, String id_field) throws NullPointerException {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		wheres.put(id_field,id);
		List<Long> roles = db_mngr.selectListID(DbMap.V_ROLES, DbMap.ID_ROLE, wheres);
		List<Long> stations = db_mngr.selectListID(DbMap.V_STATIONS, DbMap.ID_STATION, wheres);
		Map<Long,DummyContainer> dummies = new HashMap<Long,DummyContainer>();
		// The results are ordered by ID, this simplify the code a little
		for (Row row : db_mngr.selectList(DbMap.R_ROLES, new String[] {DbMap.ID_ROBOT, DbMap.ROLE},
										   new Object[] {roles}, String.format("%s IN :0", DbMap.ID_SUPER))) {
			if (!dummies.containsKey(row.getLong(DbMap.ID_ROBOT)))
				dummies.put(row.getLong(DbMap.ID_ROBOT), new DummyContainer());
			dummies.get(row.getLong(DbMap.ID_ROBOT)).addRole(row.getString(DbMap.ROLE));
		}
		for (Row row : db_mngr.selectList(DbMap.R_STATIONS, new String[] {DbMap.ID_ROBOT, DbMap.STATION},
										  new Object[] {new ArrayList<Long>(dummies.keySet()), stations, stations},
										  String.format("%s IN :0 AND (%s IN :1 OR %s IN :2)",
														DbMap.ID_ROBOT, DbMap.ID_SUPER, DbMap.ID_STATION)))
			dummies.get(row.getLong(DbMap.ID_ROBOT)).addStation(row.getString(DbMap.STATION));
		for (long key : new ArrayList<Long>(dummies.keySet()))
			if (dummies.get(key).getStations().isEmpty()) dummies.remove(key);
		for (Row row : db_mngr.selectList(DbMap.V_CLIENTS, DbMap.F_CLIENT, new Object[] {new ArrayList<Long>(dummies.keySet())},
										  String.format("%s IN :0", DbMap.ID_ROBOT))) {
			dummies.get(row.getLong(DbMap.ID_ROBOT)).setDummy(new Robot(row.getLong(DbMap.ID_ROBOT), row.getString(DbMap.NAME),
																		row.getTimestamp(DbMap.CREATED),
																		row.getTimestamp(DbMap.EXPIRES)));
			dummies.get(row.getLong(DbMap.ID_ROBOT)).setBanned(row.getBoolean(DbMap.BANNED));
			dummies.get(row.getLong(DbMap.ID_ROBOT)).setViolations(row.getInt(DbMap.VIOLATIONS));
		}
		return new ArrayList<DummyContainer>(dummies.values());
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * STATION AND ROLE METHODS * Getters and Setters for STATIONS and ROLES, that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Get the roles of the user and his subroles
	public static List<RestNode> getRoles(DbMngr db_mngr, long id, String id_field) {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		List<RestNode> roles = new ArrayList<RestNode>();
		wheres.put(id_field, id);
		List<Row> rows = new ArrayList<Row>(db_mngr.selectList(DbMap.V_ROLES, new String[] {DbMap.ID_ROLE, DbMap.ROLE,
				DbMap.ID_SUB, DbMap.SUB, DbMap.PARENT}, wheres));
		Role role;
		for (Row row : rows) {
			role = new Role(row.getLong(DbMap.ID_ROLE), row.getString(DbMap.ROLE));
			if (!roles.contains(role)) roles.add(role); }
		Row row;
		while (!rows.isEmpty()) {
			row = rows.remove(0);
			if (row.getLong(DbMap.ID_SUB) <= 0) continue;
			role = new Role(row.getLong(DbMap.ID_SUB), row.getString(DbMap.SUB), row.getLong(DbMap.PARENT));
			if (!insertRestNode(roles, role)) rows.add(row);
		}
		return roles;
	}
	// Get only the subroles of the user
	public static List<RestNode> getSubroles(DbMngr db_mngr, long id, String id_field) {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		List<RestNode> roles = new ArrayList<RestNode>();
		wheres.put(id_field, id);
		List<Row> rows = new ArrayList<Row>(db_mngr.selectList(DbMap.V_ROLES, new String[] {DbMap.ID_ROLE, DbMap.ROLE,
				DbMap.ID_SUB, DbMap.SUB, DbMap.PARENT}, wheres));
		Role role;
		for (Row row : rows) {
			role = new Role(row.getLong(DbMap.ID_ROLE), row.getString(DbMap.ROLE));
			if (!roles.contains(role)) roles.add(role); }
		Row row;
		while (!rows.isEmpty()) {
			row = rows.remove(0);
			if (row.getLong(DbMap.ID_SUB) <= 0) continue;
			role = new Role(row.getLong(DbMap.ID_SUB), row.getString(DbMap.SUB), row.getLong(DbMap.PARENT));
			if (!insertRestNode(roles, role)) rows.add(row);
		}
		List<RestNode> result = new ArrayList<RestNode>();
		for (RestNode r : roles) for (RestNode n : r.getChildren()) if (!result.contains(n)) result.add(n);
		return result;
	}
	// Get a list of strings rapresenting the subroles of the user
	private static List<String> getSR(DbMngr db_mngr, long id, String id_field) {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		List<String> roles = new ArrayList<String>();
		wheres.put(id_field, id);
		for (Row row : db_mngr.selectList(DbMap.V_ROLES, new String[] {DbMap.SUB}, wheres))
			roles.add(row.getString(DbMap.SUB));
		return roles;
	}
	// Return the stations organized in a tree, I might find duplicates, i'll let the client clean this up
	public static List<RestNode> getStations(DbMngr db_mngr, long id, String id_field) {
		Map<String,Object> wheres = new HashMap<String,Object>(1);
		List<RestNode> stations = new ArrayList<RestNode>();
		wheres.put(id_field, id);
		List<Row> rows = new ArrayList<Row>(db_mngr.selectList(DbMap.V_STATIONS, new String[] {DbMap.ID_STATION, DbMap.STATION,
				DbMap.ID_SUB, DbMap.SUB, DbMap.PARENT}, wheres));
		Station station;
		for (Row row : rows) {
			station = new Station(row.getLong(DbMap.ID_STATION), row.getString(DbMap.STATION));
			if (!stations.contains(station)) stations.add(station); }
		Row row;
		while (!rows.isEmpty()) {
			row = rows.remove(0);
			if (row.getLong(DbMap.ID_SUB) <= 0) continue;
			station = new Station(row.getLong(DbMap.ID_SUB), row.getString(DbMap.SUB), row.getLong(DbMap.PARENT));
			if (!insertRestNode(stations, station)) rows.add(row);
		}
		return stations;
	}
	// Support class for getStations, getRoles and getSubroles
	private static boolean insertRestNode(List<RestNode> tree, RestNode node) {
		boolean result = false;
		for (RestNode root : tree) {
			if (root.getID() == node.getParent().getID()) return root.addChild(node);
			else result = insertRestNode(root.getChildren(), node);
			if (result) break;
		} return result;
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * REGISTRATION METHODS * Getters and Setters for ROBOTS and USERS, that interact with the underlying DB Manager 
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Register a new User, check roles and privileges
	public static DummyContainer registerUser(DbMngr db_mngr, DummyContainer admin, DummyContainer user)
								 throws CredentialException {
		long user_id = 0;
		try {
			// Verify the correct format of the registration values and refuse similarities between values
			if (!(CryptUtils.verifyValue(user.getName()) && CryptUtils.verifyEmail(user.getAddress())
				  && CryptUtils.verifyName(user.getFirstname()) && CryptUtils.verifyName(user.getLastname())))
				throw new CredentialException("the registration contains unacceptable characters");
			if (db_mngr.selectRow(DbMap.V_CLIENTS, new String[]{DbMap.ID_USER}, new Object[]{user.getName(),user.getAddress()},
								  String.format("%s LIKE :0 OR (%s IS NULL AND %s LIKE :1)",
										   		DbMap.NAME, DbMap.ID_ROBOT, DbMap.ADDRESS)) != null)
				throw new CredentialException("the user "+user.getName()+" - "+user.getAddress()+" can't be overwritten");
			if (user.getRoles().isEmpty() && user.getStations().isEmpty()) {
				user.addStation(DbMap.INIT_STATION);
				user.addRole(DbMap.INIT_ROLE);
			} else
				if (!(getSR(db_mngr, admin.getID(), admin.getID_field()).containsAll(user.getRoles())
						&& admin.getStations().containsAll(user.getStations())))
					throw new CredentialException("the user "+user.getName()+" can't be associated with these stations/roles");
			// Randomly chose a one time password
			/* TODO : This is a huge stretch, these data are sensitive, I should encrypt them, at least the email address
			 * Solution 1 : Use the email address instead of the username
			 * Weakness   : Not that hard to recover an email address, since it's easily guessable
			 * Solution 2 : encrypt the email address with a key(username in plaintext + salt)
			 * Weakness   : I can only send emails to the client if he just connected to the server, same as above
			 * Solution 3 : Use a key stored in the config file of the server, store the encrypted emails in the DB
			 * Weakness   : I think this would only slows down an attacker, at least i would be able to recover the email easily
			 */
			String onetimepass = CryptUtils.getRandomOtp();
			Map<String,Object> fields = new HashMap<String,Object>();
			fields.put(DbMap.NAME, user.getName());
			fields.put(DbMap.USERNAME, CryptUtils.getHash(onetimepass));
			fields.put(DbMap.PASSWORD, CryptUtils.encodeToString(CryptUtils.getHash(CryptUtils.getRandomString()).getBytes()));
			fields.put(DbMap.FIRSTNAME, user.getFirstname());
			fields.put(DbMap.LASTNAME, user.getLastname());
			fields.put(DbMap.ADDRESS, user.getAddress());
			fields.put(DbMap.CREATED, new Timestamp(System.currentTimeMillis()));
			fields.put(DbMap.EXPIRES, fields.get(DbMap.CREATED));
			user_id = db_mngr.insertID(DbMap.T_USERS, fields);
			if (user_id < 1) throw new XProtocolError("Failed on "+DbMap.T_USERS);
			fields.clear();
			System.out.println("user registrato");
			fields.put(DbMap.ID_USER, user_id);
			for (Row row : db_mngr.selectList(DbMap.T_ROLES, new String[] {DbMap.ID_ROLE}, new Object[] {user.getRoles()},
											  String.format("%s IN :0", DbMap.NAME))) {
				fields.put(DbMap.ID_ROLE, row.getLong(DbMap.ID_ROLE));
				if (db_mngr.insertID(DbMap.X_USER_ROLE, fields) < 1)
					throw new XProtocolError("failed on "+DbMap.X_USER_ROLE);
			}
			System.out.println("ruoli registrati");
			fields.remove(DbMap.ID_ROLE);
			for (Row row : db_mngr.selectList(DbMap.T_STATIONS, new String[] { DbMap.ID_STATION },
											  new Object[] {user.getStations()}, String.format("%s IN :0", DbMap.NAME))) {
				fields.put(DbMap.ID_STATION, row.getLong(DbMap.ID_STATION));
				if (db_mngr.insertID(DbMap.X_USER_STATION, fields) < 1)
					throw new XProtocolError("failed on "+DbMap.X_USER_STATION);
			}
			System.out.println("invio l'email");
			// Send a verification email to the provided email address
			Map<String,String> mail_map = new HashMap<String,String>(1);
			mail_map.put("fname", user.getFirstname());
			mail_map.put("lname", user.getLastname());
			mail_map.put("otp", onetimepass);
			mail_map.put("expires", String.valueOf(CryptUtils.SIGNIN_EXPIRES / 60));
			mail_mngr.send(user.getAddress(), "new_user", mail_map);
			return user;
		} catch (Exception e) {
			if (user_id > 0) {
				Map<String,Object> wheres = new HashMap<String,Object>(1);
				wheres.put(DbMap.ID_USER, Long.valueOf(user_id));
				try { db_mngr.delete(DbMap.X_USER_STATION, wheres); } catch (Exception ignore) {}
				try { db_mngr.delete(DbMap.X_USER_ROLE, wheres); } catch (Exception ignore) {}
				try { db_mngr.delete(DbMap.T_USERS, wheres); } catch (Exception ignore) {}
			}
			e.printStackTrace();
			throw new CredentialException(String.format("The server failed to register the new user %s with the message %s",
														user.getName(), e.getMessage()));
		}
	}
	// Register a new robot, check roles and stations
	public static DummyContainer registerRobot(DbMngr db_mngr, DummyContainer admin, DummyContainer robot)
								 throws CredentialException {
		long robot_id = 0;
		try {
			if (!CryptUtils.verifyValue(robot.getName()))
				throw new CredentialException("the registration contains unacceptable characters");
			Map<String,Object> fields = new HashMap<String,Object>();
			fields.put(DbMap.NAME, robot.getName());
			if (db_mngr.selectRow(DbMap.V_CLIENTS, new String[] { DbMap.ID_ROBOT }, fields) != null)
				throw new CredentialException("the robot "+robot.getName()+" can't be overwritten");
			if (!(getSR(db_mngr, admin.getID(), admin.getID_field()).containsAll(robot.getRoles())
					&& admin.getStations().containsAll(robot.getStations())))
				throw new CredentialException("the robot "+robot.getName()+" can't be associated with these stations/roles");
			// Randomly chose username and password for the device
			String username = CryptUtils.getRandomPassword();
			String password = CryptUtils.getRandomPassword();
			fields.put(DbMap.USERNAME, CryptUtils.getHash(username));
			fields.put(DbMap.PASSWORD, CryptUtils.encodeToString(CryptUtils.getArgonHash(password).getBytes()));
			fields.put(DbMap.ID_USER, admin.getID());
			// The insertion will fail if the username isn't unique
			robot_id = db_mngr.insertID(DbMap.T_ROBOTS, fields);
			if (robot_id < 1) throw new XProtocolError("Failed on "+DbMap.T_ROBOTS);
			fields.clear();
			fields.put(DbMap.ID_ROBOT, robot_id);
			for (Row row : db_mngr.selectList(DbMap.T_ROLES, new String[] {DbMap.ID_ROLE}, new Object[] {robot.getRoles()},
											  String.format("%s IN :0", DbMap.NAME))) {
				fields.put(DbMap.ID_ROLE, row.getLong(DbMap.ID_ROLE));
				if (db_mngr.insertID(DbMap.X_ROBOT_ROLE, fields) < 1)
					throw new XProtocolError("failed on "+DbMap.X_ROBOT_ROLE);
			}
			fields.remove(DbMap.ID_ROLE);
			for (Row row : db_mngr.selectList(DbMap.T_STATIONS, new String[] { DbMap.ID_STATION },
											  new Object[] {robot.getStations()}, String.format("%s IN :0", DbMap.NAME))) {
				fields.put(DbMap.ID_STATION, row.getLong(DbMap.ID_STATION));
				if (db_mngr.insertID(DbMap.X_ROBOT_STATION, fields) < 1)
					throw new XProtocolError("failed on "+DbMap.X_ROBOT_STATION);
			}
			fields.remove(DbMap.ID_STATION);
			fields.put(DbMap.ID_CONFIG, DbMap.INIT_CONFIG);
			if (db_mngr.insertID(DbMap.X_ROBOT_CONFIG, fields) < 1) throw new XProtocolError("failed on "+DbMap.X_ROBOT_CONFIG);
			// Send a verification email to the provided email address
			Map<String,String> mail_map = new HashMap<String,String>(2);
			mail_map.put("name",robot.getName());
			mail_map.put("username",username);
			mail_map.put("password",password);
			mail_mngr.send(admin.getAddress(), "new_robot", mail_map);
			return robot;
		} catch (Exception e) {
			if (robot_id > 0) {
				Map<String,Object> wheres = new HashMap<String,Object>(1);
				wheres.put(DbMap.ID_ROBOT, Long.valueOf(robot_id));
				try { db_mngr.delete(DbMap.X_ROBOT_STATION, wheres); } catch (Exception ignore) {}
				try { db_mngr.delete(DbMap.X_ROBOT_ROLE, wheres); } catch (Exception ignore) {}
				try { db_mngr.delete(DbMap.X_ROBOT_CONFIG, wheres); } catch (Exception ignore) {}
				try { db_mngr.delete(DbMap.T_ROBOTS, wheres); } catch (Exception ignore) {}
			}
			throw new CredentialException("The server failed to register the new user " + robot.getName() +
				  	  " with the message " + e.getMessage());
		}
	}
	// VERIFY USER : Check the username and the token of the newly registered user, eventually register the user in the DB
	public static DummyContainer verifyUser(DbMngr db_mngr, DummyContainer user, String username, String password)
								 throws NullPointerException, CredentialException, XProtocolError, MessagingException {
		if (!(CryptUtils.verifyPassword(password) && CryptUtils.verifyUsername(username))
			|| CryptUtils.similar(username, new String[] {user.getFirstname(), user.getLastname(),
														  user.getName(), user.getAddress()})
			|| CryptUtils.similar(password, new String[] {username, user.getFirstname(), user.getLastname(),
														  user.getName(), user.getAddress()}))
			throw new CredentialException("the verification contains unacceptable characters");
		Map<String,Object> fields = new HashMap<String,Object>(3);
		fields.put(DbMap.USERNAME, CryptUtils.getHash(username));
		fields.put(DbMap.PASSWORD, CryptUtils.encodeToString(CryptUtils.getArgonHash(password).getBytes()));
		fields.put(DbMap.EXPIRES, new Timestamp(System.currentTimeMillis() + (CryptUtils.ACCOUNT_EXPIRES * 60000)));
		if (db_mngr.updateCount(DbMap.T_USERS, fields, new Object[] {user.getID()},
								String.format("%s LIKE :0 AND %s LIKE %s", DbMap.ID_USER, DbMap.CREATED, DbMap.EXPIRES)) != 1)
			throw new CredentialException("Something went wrong, "+user.getID()+" "+user.getName()+" can't be activated");
		Map<String,String> mail_map = new HashMap<String,String>(1);
		mail_map.put("fname", user.getFirstname());
		mail_map.put("lname", user.getLastname());
		mail_map.put("user", username);
		mail_map.put("pass", password);
		mail_mngr.send(user.getAddress(), "activate_user", mail_map);
		return user;
	}
}
