package rest.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class FileMngr {
	private static Map<String,Object> file_map;
	private static String BASE_PATH, FILE_EXT;
	private static int FOLDER_SIZE, FOLDER_DEPTH;
	// Set the MAP for the DB and get INT and STRING from it
	public static void setFileMap(Map<String,Object> db_map) {
		file_map = db_map;
		BASE_PATH = FileMngr.getStr("base_path");
		FILE_EXT = FileMngr.getStr("file_extension");
		FOLDER_SIZE = FileMngr.getInt("folder_size");
		FOLDER_DEPTH = FileMngr.getInt("folder_depth");
	}
	private static String getStr(String key) { return (String) file_map.get(key); }
	private static int getInt(String key) { return (int) file_map.get(key); }
	private static String buildPath(String robot, long id) {
		String[] path = new String[FOLDER_DEPTH + 1];
		long temp = id;
		for (int i = FOLDER_DEPTH - 1; i >= 0; i--) {
			temp = temp / FOLDER_SIZE;
			path[i] = String.valueOf(temp);
		}
		path[FOLDER_DEPTH] = robot;
		return String.join("/", path);
	}
	public static void upload(String robot, long id, String data) throws IOException {
		FileMngr.upload(FileMngr.buildPath(robot, id), String.format("%d.%s", id, FILE_EXT), data); }
	public static void upload(String path, String file, String data) throws IOException {
		InputStream stream = new ByteArrayInputStream(data.getBytes());
		int read = 0;
		byte[] bytes = new byte[1024];
		File dir = new File(String.format("%s/%s", BASE_PATH, path));
		if (!dir.exists()) dir.mkdirs();
		OutputStream out = new FileOutputStream(new File(String.format("%s/%s/%s", BASE_PATH, path, file)), true);
		while ((read = stream.read(bytes)) != -1) out.write(bytes, 0, read);
		out.flush();
		out.close();
	}
	public static byte[] download(String robot, long id) throws IOException {
		return FileMngr.download(String.format("%s/%d.%s", FileMngr.buildPath(robot, id), id, FILE_EXT)); }
	public static byte[] download(String path) throws IOException {
		File file = new File(String.format("%s/%s", BASE_PATH, path));
		byte[] bytesArray = new byte[(int) file.length()]; 
		InputStream fis = new FileInputStream(file);
		fis.read(bytesArray);
		fis.close();
		return bytesArray;
	}
}
