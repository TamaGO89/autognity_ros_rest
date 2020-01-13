package rest.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.mysql.cj.protocol.x.XProtocolError;
import com.mysql.cj.xdevapi.DeleteStatement;
import com.mysql.cj.xdevapi.InsertResult;
import com.mysql.cj.xdevapi.Result;
import com.mysql.cj.xdevapi.Row;
import com.mysql.cj.xdevapi.RowResult;
import com.mysql.cj.xdevapi.Schema;
import com.mysql.cj.xdevapi.SelectStatement;
import com.mysql.cj.xdevapi.Session;
import com.mysql.cj.xdevapi.SessionFactory;
import com.mysql.cj.xdevapi.Statement;
import com.mysql.cj.xdevapi.Table;
import com.mysql.cj.xdevapi.UpdateStatement;

/*==============================================================================================================================
 * DATABASE MANAGER * Manage the connection and the queries to any SQL SCHEME
 *============================================================================================================================*/
public class DbMngr {
	
	// constants
	public static class DbMap {
		public static int LOG_LENGTH; // Configurable values, may need an update DB side
		public static final long // Standard values
				MIN_LONG = 0,						MAX_LONG = 253402297199000L,		INIT_CONFIG = 1;
		public static final String // General, for all DB connections
				T_ROBOTS = "robots",				T_USERS = "users",					V_ROLES = "log_roles",
				V_STATIONS = "log_stations",		V_RAWS = "raws_view",				V_CONFIGS = "configs_view",
				V_KEYS = "keys_view",				V_INFOS = "bags_view",				V_CLIENTS = "clients",
				ID = "id",							ID_ROBOT = "robot_id",				ID_USER = "user_id",
				ID_CONFIG = "config_id",			ID_INFO = "bag_id",					ID_RAW = "raw_id",
				ID_STATION = "station_id",			ID_ROLE = "role_id",				ID_SECRET = "secret_id",
				ID_SUB = "sub_id",					ID_SUPER = "sup_id",				ROLE = "role",
				STATION = "station",				NAME = "name",						USER = "user",
				ROBOT = "robot",					CONTENT = "content",				PUBLIC_KEY = "public_key",
				CREATED = "created",				EXPIRES = "expired",				HASH = "hash",
				SUPER = "sup",						SUB = "sub",						PARENT = "parent",
				RECORD = "raw",
				// Fields and tables specific for the login queries
				V_LOGINS = "logins",				USERNAME = "username",				PASSWORD = "password",
				VIOLATIONS = "violations",			BANNED = "banned",					V_ALOG = "activity_log",
				T_RLOG = "robot_logs",				T_ULOG = "user_logs",				X_USER_STATION = "user_to_station",
				X_USER_ROLE = "user_to_role",		T_ROLES = "roles",					T_STATIONS = "stations",
				// Fields and tables specific for robots operations
				T_INFOS = "bag_infos",				T_SECRETS = "bag_secrets",			T_RAWS = "bag_raws",
				// Fields and tables specific for regular users operations
				T_CONFIG = "bag_configs",			X_ROBOT_CONFIG = "robot_to_config",	V_ROBOTS = "user_robots",
				V_SECRETS = "secrets_view",			V_RAW = "raws_view",				R_ROLES = "robot_roles",
				U_ROLES = "user_roles",				R_STATIONS = "robot_stations",		U_STATIONS = "user_stations",
				FIRSTNAME = "firstname",			LASTNAME = "lastname",				ADDRESS = "address",
				// Fields and tables specific for the administrators
				X_ROBOT_ROLE = "robot_to_role",		X_ROBOT_STATION = "robot_to_station",
				// Standard values
				MIN_TIME = "1970-01-01 01:00:01",	MAX_TIME = "9999-12-31 23:59:59",
				INIT_STATION = "guests",			INIT_ROLE = "guest";
		public static final String[] // Collection of fields to shorten the prepared statements setup
				F_USER =	new String[] { "user_id", "robot", "created", "expired" },
				F_CONFIG =	new String[] { "config_id", "user", "robot", "content", "created" },
				F_KEY =		new String[] { "user_id", "robot_id", "name", "public_key", "created", "expired" },
				F_INFO =	new String[] { "bag_id", "config_id", "robot_id", "content", "created" },
				F_LOGIN =	new String[] { "robot_id", "user_id", "name", "password", "violations", "banned",
										   "created", "expired", "firstname", "lastname", "address" },
				F_CLIENT =	new String[] { "robot_id", "user_id", "name", "violations", "banned",
										   "created", "expired", "firstname", "lastname", "address" },
				F_ROBOT =	new String[] { "robot_id", "robot", "created", "expired" },
				F_SECRET =	new String[] { "secret_id", "bag_id", "user_id", "name", "content" },
				F_ALOG =	new String[] { "name", "id", "content" };
	}
	// static variables
	private static Map<String,Object> database_map = new HashMap<String,Object>();
	private static Map<String,DbMngr> db_mngr_map = new HashMap<String,DbMngr>();
	// instance variables
	private Map<String,Object> session_map;
	private Session session;
	private Schema database;
	private final String name;
	private String id;
	private final int priority;
	//private List<String> table_names;
	@SuppressWarnings("rawtypes") private Map<String,Statement> statement_map = new HashMap<String,Statement>();

	/*--------------------------------------------------------------------------------------------------------------------------
	 * CONSTRUCTOR AND FINALIZER * Takes only one kind of input, specified in the JSON containing login informations
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Constructor, get a session_map as input
	public DbMngr(Map<String,Object> session_map) {
		this.session_map = session_map;
		String database_url = String.format("%s://%s:%s@%s:%d/%s", getStr("protocol"), getStr("username"),
											getStr("password"), getStr("host"), getInt("port"), getStr("schema"));
		this.session = new SessionFactory().getSession(database_url);
		this.database = this.session.getDefaultSchema();
		this.name = getStr("name");
		this.id = getStr("id");
		this.priority = getInt("priority");
	}
	// CLOSE CONNECTION method: close the connection if it's still on
	public boolean close() { if (this.session != null) this.session.close(); return true; }
	// FINALIZE OBJECT method: override to specify that this has to properly close the connection
	@Override protected void finalize() throws Throwable{ this.close(); super.finalize(); }

	/*--------------------------------------------------------------------------------------------------------------------------
	 * PREPARE STATEMENTS * method to prepare the statements
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Return a formatted Where Condition with given format and and where's fields
	private static String getWhereConditions(Set<String> wheres) {
		int i = 0;
		String[] where_condition = new String[wheres.size()];
		for (String where : wheres)
			where_condition[i] = String.format("%s LIKE :%s", where, i++);
		return String.join(" AND ", where_condition);
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * GETTERS * Self explanatory
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Getters for Db Manager NAME, PRIORITY and TABLES and for the ID field (user or robot) 
	public String getName() { return this.name; }
	public String getID() { return this.id; }
	public void setID(String id) { this.id = id; }
	public int getPriority() { return this.priority; }
	private Table getTable(String table_name) {return this.database.getTable(table_name);}
	// Get the Db Manager for the user, if a list is provided always chose the role with the lower priority
	public static DbMngr getDbMngr(String key) { return getDbMngr().get(key); }
	// TODO I don't want this based on priority, but on roles hierarchy getting information from the DB itself
	public static DbMngr getDbMngr(List<String> keys) {
		DbMngr db_temp, db_mngr = null;
		for (String key : keys) {
			db_temp = getDbMngr(key);
			if (db_temp != null && (db_mngr == null || db_mngr.getPriority() > db_temp.getPriority()))
				db_mngr = db_temp;
		}
		return db_mngr;
	}
	// If the list of Database Manager is empty, fill it
	@SuppressWarnings("unchecked") public static Map<String,DbMngr> getDbMngr() {
		if (!db_mngr_map.isEmpty()) return db_mngr_map;
		DbMngr temp_mngr;
		for (Map<String,Object> mngr_map : (List<Map<String,Object>>) database_map.get("managers"))
			try {
				temp_mngr = new DbMngr(mngr_map);
				db_mngr_map.putIfAbsent(temp_mngr.getName(), temp_mngr);
			} catch (XProtocolError e) { System.out.println(mngr_map + " : " + e.getMessage()); }
		return db_mngr_map;
	}
	// Set the MAP for the DB and get INT and STRING from it
	public static void setDbMap(Map<String,Object> db_map) {
		database_map = db_map;
		DbMap.LOG_LENGTH = (int) database_map.get("log_length"); }
	private String getStr(String key) {
		return (this.session_map.containsKey(key)) ? (String) this.session_map.get(key) : (String) database_map.get(key); }
	private int getInt(String key) {
		return (this.session_map.containsKey(key)) ? (int) this.session_map.get(key) : (int) database_map.get(key); }

	/*--------------------------------------------------------------------------------------------------------------------------
	 * COUNT METHODS * counts the number of row according to specified restraint
	 *------------------------------------------------------------------------------------------------------------------------*/
	public long count(String table_name, Map<String,Object> wheres) {
		RowResult result = this.select(table_name, new String[] {wheres.keySet().iterator().next()},
									   wheres.values().toArray(), getWhereConditions(wheres.keySet()));
		return result != null ? result.fetchAll().size() : 0;
	}
	public long count(String table_name, Object[] wheres, String where_condition) {
		RowResult result = this.select(table_name, new String[] {where_condition.split(" ")[0]}, wheres, where_condition);
		return result != null ? result.fetchAll().size() : 0;
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * SELECT METHODS * various forms of selection from the DB
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Gets the "field" ID by name (common field for many tables)
	public long selectID(String table, String field, String name) {
		Map<String,Object> wheres = new HashMap<String,Object>();
		wheres.put(DbMap.NAME, name);
		return this.selectRow(table, new String[] { field }, wheres).getLong(field);
	}
	public long selectID(String table, String field, Map<String,Object> wheres) {
		return this.selectRow(table, new String[] { field }, wheres).getLong(field);
	}
	public Long selectID(String table, String field, Object[] wheres, String where_conditions) {
		return this.selectRow(table, new String[] { field }, wheres, where_conditions).getLong(field);
	}
	public List<Long> selectListID(String table, String field, Map<String,Object> wheres) {
		List<Long> result = new ArrayList<Long>();
		for (Row row : selectList(table, new String[] { field }, wheres))
			if (!result.contains(row.getLong(field))) result.add(row.getLong(field));
		return result;
	}
	// SELECT (ONE,ALL) : SELECT fields FROM table_name WHERE wheres.keys LIKE wheres.values
	public Row selectRow(String table_name, String[] fields, Map<String,Object> wheres) {
		return this.select(table_name, fields, wheres.values().toArray(), getWhereConditions(wheres.keySet())).fetchOne(); }
	public List<Row> selectList(String table_name, String[] fields, Map<String,Object> wheres) {
		return this.select(table_name, fields, wheres.values().toArray(), getWhereConditions(wheres.keySet())).fetchAll(); }
	public RowResult select(String table_name, String[] fields, Map<String,Object> wheres) {
		return this.select(table_name, fields, wheres.values().toArray(), getWhereConditions(wheres.keySet())); }
	// SELECT (ONE,ALL) : SELECT fields FROM table_name WHERE 'CONDITION'
	public Row selectRow(String table_name, String[] fields, Object[] wheres, String where_condition) {
		return this.select(table_name, fields, wheres, where_condition).fetchOne(); }
	public List<Row> selectList(String table_name, String[] fields, Object[] wheres, String where_condition) {
		return this.select(table_name, fields, wheres, where_condition).fetchAll(); }
	synchronized public RowResult select(String table_name, String[] fields, Object[] wheres, String where_condition) {
		Table select_table = this.getTable(table_name);
		String statement_key = String.format("Select:%s:%s:%s", table_name, Arrays.toString(fields), where_condition);
		if (this.statement_map.containsKey(statement_key))
			return this.bind((SelectStatement) this.statement_map.get(statement_key), wheres, where_condition).execute();
		SelectStatement prepared_statement = select_table.select(fields).where(where_condition);
		this.statement_map.put(statement_key, prepared_statement);
		return this.bind(prepared_statement, wheres, where_condition).execute();
	}
	// replace lists in wheres with different indices and in the where_condition I've to put multiple placeholders
	@SuppressWarnings("unchecked")
	private SelectStatement bind(SelectStatement prepared_statement, Object[] wheres, String where_cond)
			throws NullPointerException {
		int offset, j, i = wheres.length;
		List<Object> where_list = new ArrayList<Object>(Arrays.asList(wheres));
		List<Object> where_temp;
		while (--i >= 0 && List.class.isInstance(wheres[i])) {
			j = offset = i;
			where_temp = (List<Object>) wheres[i];
			if (where_temp.isEmpty()) throw new NullPointerException("One of the list is empty\n\t" + where_cond + " ["+i+"]");
			while (--j >= 0 && List.class.isInstance(wheres[j])) offset += ((List<?>) wheres[j]).size()-1;
			where_cond = where_cond.replace(":"+ i,
											Arrays.toString(IntStream.range(offset, offset + where_temp.size()).toArray())
											.replaceAll("([0-9]+)", ":$1").replaceAll("\\[(.*?)\\]", "($1)"));
			where_list.remove(i);
			where_list.addAll(i, where_temp);
		}
		return i < wheres.length - 1 ? prepared_statement.where(where_cond).bind(where_list) :
									   prepared_statement.bind(where_list);
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * UPDATE METHODS * various forms of updates for the DB
	 *------------------------------------------------------------------------------------------------------------------------*/
	// UPDATE (WARNING,COUNT) : UPDATE table_name SET fields.keys = fields.values WHERE wheres.keys LIKE wheres.values
	public int updateWarning(String table_name, Map<String,Object> fields, Map<String,Object> wheres) {
		return this.update(table_name, fields, wheres.values().toArray(),
						   getWhereConditions(wheres.keySet())).getWarningsCount(); }
	public long updateCount(String table_name, Map<String,Object> fields, Map<String,Object> wheres) {
		return this.update(table_name, fields, wheres.values().toArray(),
						   getWhereConditions(wheres.keySet())).getAffectedItemsCount(); }
	public Result update(String table_name, Map<String,Object> fields, Map<String,Object> wheres) {
		return this.update(table_name, fields, wheres.values().toArray(),
						   getWhereConditions(wheres.keySet())); }
	// UPDATE (WARNING,COUNT) : UPDATE table_name SET fields.keys = fields.values WHERE 'CONDITION'
	public int updateWarning(String table_name, Map<String,Object> fields, Object[] wheres, String where_condition) {
		return this.update(table_name,  fields, wheres, where_condition).getWarningsCount(); }
	public long updateCount(String table_name, Map<String,Object> fields, Object[] wheres, String where_condition) {
		return this.update(table_name,  fields, wheres, where_condition).getAffectedItemsCount(); }
	synchronized public Result update(String table_name, Map<String,Object> fields, Object[] wheres, String where_condition) {
		Table update_table = this.getTable(table_name);
		String statement_key = String.format("Update:%s:%s:%s", table_name, fields.keySet().toString(), where_condition);
		if (this.statement_map.containsKey(statement_key))
			return this.bind((UpdateStatement) this.statement_map.get(statement_key), wheres, where_condition)
																 .set(fields).execute();
		UpdateStatement prepared_statement = update_table.update().where(where_condition);
		this.statement_map.put(statement_key, prepared_statement);
		return this.bind(prepared_statement, wheres, where_condition).set(fields).execute();
	}
	// replace lists in wheres with different indices and in the where_condition I've to put multiple placeholders
	@SuppressWarnings("unchecked")
	private UpdateStatement bind(UpdateStatement prepared_statement, Object[] wheres, String where_cond)
							throws NullPointerException {
		int offset, j, i = wheres.length;
		List<Object> where_list = new ArrayList<Object>(Arrays.asList(wheres));
		List<Object> where_temp;
		while (--i >= 0 && List.class.isInstance(wheres[i])) {
			j = offset = i;
			where_temp = (List<Object>) wheres[i];
			if (where_temp.isEmpty()) throw new NullPointerException("One of the list is empty");
			while (--j >= 0 && List.class.isInstance(wheres[j])) offset += ((List<?>) wheres[j]).size()-1;
			where_cond = where_cond.replace(":"+ i,
											Arrays.toString(IntStream.range(offset, offset + where_temp.size()).toArray())
											.replaceAll("([0-9]+)", ":$1").replaceAll("\\[(.*?)\\]", "($1)"));
			where_list.remove(i);
			where_list.addAll(i, where_temp);
		}
		return i < wheres.length - 1 ? prepared_statement.where(where_cond).bind(where_list) :
									   prepared_statement.bind(where_list);
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * INSERT METHODS * various forms of insertion for the DB
	 *------------------------------------------------------------------------------------------------------------------------*/
	// INSERT (ID,WARNING,COUNT) : INSERT INTO table_name (fields.keys) VALUES (fields.values)
	public long insertID(String table_name, Map<String,Object> fields) {
		return this.insert(table_name, fields).getAutoIncrementValue(); }
	public int insertWarning(String table_name, Map<String,Object> fields) {
		return this.insert(table_name, fields).getWarningsCount(); }
	public long insertCount(String table_name, Map<String,Object> fields) {
		return this.insert(table_name, fields).getAffectedItemsCount();}
	synchronized public InsertResult insert(String table_name, Map<String,Object> fields) {
		return this.getTable(table_name).insert(fields).execute(); }

	/*--------------------------------------------------------------------------------------------------------------------------
	 * DELETE METHODS * various forms of deletion for the DB
	 *------------------------------------------------------------------------------------------------------------------------*/
	// DELETE : DELETE FROM table_name WHERE wheres.keys LIKE wheres.values
	public Result delete(String table_name, Map<String,Object> wheres) {
		return this.delete(table_name, wheres.values().toArray(), getWhereConditions(wheres.keySet())); }
	synchronized public Result delete(String table_name, Object[] wheres, String where_condition) {
		Table delete_table = this.getTable(table_name);
		String statement_key = String.format("Delete:%s:%s", table_name, where_condition);
		if (this.statement_map.containsKey(statement_key))
			return this.bind((DeleteStatement) this.statement_map.get(statement_key), wheres, where_condition).execute();
		DeleteStatement prepared_statement = delete_table.delete().where(where_condition);
		this.statement_map.put(statement_key, prepared_statement);
		return this.bind(prepared_statement, wheres, where_condition).execute();
	}
	// replace lists in wheres with different indices and in the where_condition I've to put multiple placeholders
	@SuppressWarnings("unchecked")
	private DeleteStatement bind(DeleteStatement prepared_statement, Object[] wheres, String where_cond)
							throws NullPointerException {
		int offset, j, i = wheres.length;
		List<Object> where_list = new ArrayList<Object>(Arrays.asList(wheres));
		List<Object> where_temp;
		while (--i >= 0 && List.class.isInstance(wheres[i])) {
			j = offset = i;
			where_temp = (List<Object>) wheres[i];
			if (where_temp.isEmpty()) throw new NullPointerException("One of the list is empty");
			while (--j >= 0 && List.class.isInstance(wheres[j])) offset += ((List<?>) wheres[j]).size()-1;
			where_cond = where_cond.replace(":"+ i,
											Arrays.toString(IntStream.range(offset, offset + where_temp.size()).toArray())
											.replaceAll("([0-9]+)", ":$1").replaceAll("\\[(.*?)\\]", "($1)"));
			where_list.remove(i);
			where_list.addAll(i, where_temp);
		}
		return i < wheres.length - 1 ? prepared_statement.where(where_cond).bind(where_list) :
									   prepared_statement.bind(where_list);
	}
}
