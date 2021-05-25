package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * This class contains all the global application configuration stuff.
 * @author bmilojkovic
 *
 */
public class AppConfig {

	public static WorkDirectory workDirectory;

	public static File storageDir;

	/**
	 * Convenience access for this servent's information
	 */
	public static ServentInfo myServentInfo;
	

	
	public static boolean INITIALIZED = false;

	public static String BOOTSTRAP_IP;
	public static int BOOTSTRAP_PORT;
	public static int SERVENT_COUNT;
	
	public static ChordState chordState;
	
	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <br/>
	 * <code><br/>
	 * servent_count=3 			- number of servents in the system <br/>
	 * chord_size=64			- maximum value for Chord keys <br/>
	 * bs.port=2000				- bootstrap server listener port <br/>
	 * servent0.port=1100 		- listener ports for each servent <br/>
	 * servent1.port=1200 <br/>
	 * servent2.port=1300 <br/>
	 * 
	 * </code>
	 * <br/>
	 * So in this case, we would have three servents, listening on ports:
	 * 1100, 1200, and 1300. A bootstrap server listening on port 2000, and Chord system with
	 * max 64 keys and 64 nodes.<br/>
	 * 
	 * @param configName name of configuration file
	 * @param serventId id of the servent, as used in the configuration file
	 */
	public static void readConfig(String configName, int serventId){
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
			
		} catch (IOException e) {
			Logger.timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}


		try {
			BOOTSTRAP_IP = properties.getProperty("bs.ip");
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading bootstrap_ip. Exiting...");
			System.exit(0);
		}

		try {
			BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bs.port"));
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading bootstrap_port. Exiting...");
			System.exit(0);
		}
		
		try {
			SERVENT_COUNT = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}
		
		try {
			int chordSize = Integer.parseInt(properties.getProperty("chord_size"));
			
			ChordState.CHORD_SIZE = chordSize;
			chordState = new ChordState();
			
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading chord_size. Must be a number that is a power of 2. Exiting...");
			System.exit(0);
		}
		
		String portProperty = "servent"+serventId+".port";

		int serventPort = -1;
		
		try {
			serventPort = Integer.parseInt(properties.getProperty(portProperty));
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
			System.exit(0);
		}

		String workDirProperty = "servent"+serventId+".work_dir";
		String serventWorkDir = "";

		try {
			serventWorkDir = properties.getProperty(workDirProperty);
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading " + serventWorkDir + ". Exiting...");
			System.exit(0);
		}

		File workDir = new File(serventWorkDir);

		if (workDir.exists()) {
			try {
				deleteDirectoryJava8(workDir.toPath());
			} catch (Exception e) {
				Logger.timestampedErrorPrint("Cannot delete work dir " + workDir.getAbsolutePath() + ". Exiting...");
				System.exit(0);
			}
		}
		if (!workDir.mkdir()) {
			Logger.timestampedErrorPrint("Cannot create work dir " + workDir.getAbsolutePath() + ". Exiting...");
			System.exit(0);
		}

		workDirectory = new WorkDirectory(workDir);

		String storageProperty = "servent"+serventId+".storage";
		String serventStorageDir = "";

		try {
			serventStorageDir = properties.getProperty(storageProperty);
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading " + storageProperty + ". Exiting...");
			System.exit(0);
		}

		storageDir = new File(serventStorageDir);

		if (storageDir.exists()) {
			storageDir.delete();
		}

		if (!storageDir.mkdir()) {
			Logger.timestampedErrorPrint("Cannot create storageDir dir " + storageDir.getAbsolutePath() + ". Exiting...");
			System.exit(0);
		}

		String teamProperty = "servent"+serventId+".team";
		String serventTeam = "";

		try {
			serventTeam = properties.getProperty(teamProperty);
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Problem reading " + teamProperty + ". Exiting...");
			System.exit(0);
		}
		
		myServentInfo = new ServentInfo("localhost", serventPort, serventTeam);
	}


	private static void deleteDirectoryJava8(Path path) throws IOException {

//		Path path = Paths.get(dir);

		// read java doc, Files.walk need close the resources.
		// try-with-resources to ensure that the stream's open directories are closed
		try (Stream<Path> walk = Files.walk(path)) {
			walk
					.sorted(Comparator.reverseOrder())
					.forEach(AppConfig::deleteDirectoryJava8Extract);
		}

	}

	// extract method to handle exception in lambda
	private static void deleteDirectoryJava8Extract(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			System.err.printf("Unable to delete this path : %s%n%s", path, e);
		}
	}
}
