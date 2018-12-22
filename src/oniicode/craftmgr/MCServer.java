package oniicode.craftmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

import org.ini4j.Ini;

public class MCServer {
	
	public enum State{
		ONLINE,
		OFFLINE,
		STARTING,
		ERROR
	}
	
	private Thread consoleThread; 
    private Process process; //java-process of the server
	private String console;
	private BufferedWriter consoleWriter;
	
	private File dir;

	public MCServer(int id) {
		this.id = id;
		this.dir = new File(Main.deploymentDir.getPath() + File.separator + this.id);
	}
	
	public int pid = 0;
	private int id = -1;
	
	/**
	 * 
	 * @return Server-ID des Servers :D
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Installationsverzeichnis dieses Servers
	 * @return
	 */
	public File getDir() {
		return this.dir;
	}
	
	//TODO: Bungeecord Port Detection
	public int getPort() {
		File f_server_properties = new File(this.dir.getPath() + File.separator + "files" + File.separator + "server.properties");
		if (f_server_properties.exists()) {
			try {
				String[] lines = new String(Files.readAllBytes(f_server_properties.toPath())).split("[\\r\\n]+");
				for (String s : lines) {
					if (s.contains("server-port")) {
						return Integer.parseInt(s.split("=")[1]);
					}
				}
				return -2;
			} catch (Throwable e) {
				e.printStackTrace();
				return -1;
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * @return Beschreibung des Servers, "ERROR" bei Lesefehler.
	 */
	public String getDesc() {
		String s = this.getIni().get("Server", "Desc");
		if(s!=null)
			return s;
		else {
			System.err.println("[Server #"+id+"] [!] Ung�ltige Desc-Angabe in server.ini ("+s+")");
			return "ERROR";
		}
	}
	
	/**
	 * 
	 * @return Anzahl an zugewiesenem Arbeitsspeicher in MB. ("512" bei Fehler)
	 */
	public int getMemory() {
		String s = this.getIni().get("Server", "Memory").replace("M", "");
		try {
			return Integer.parseInt(s);
		}catch (NumberFormatException e) {
			System.err.println("[Server #"+id+"] [!] Ung�ltige Memory-Angabe in server.ini ("+s+"). Benutze Fallback: 512M");
			return 512;
		}
		
	}
	
	public Ini getIni() {
		File f = new File(this.dir.getPath() + File.separator + "server.ini");
		try {
			return new Ini(f);
		} catch (Throwable t) {
			System.err.println("[Server #"+id+"] [!] Ein Fehler beim Lesen der server.ini ist aufgetreten. ("+f.getPath()+")");
			t.printStackTrace();
			return null;
		}
	}
	
	public MCServer.State getState() {
		if (process == null || !process.isAlive()) {
			return MCServer.State.OFFLINE;
		} else {
			return MCServer.State.ONLINE;
		}
	}
	
	public String getConsole() {
		return this.console;
	}
	
	public String[] getCmdline() {
		String s = "java-default";
		try {
			s = this.getIni().get("Server", "Cmdline");
			if(!s.equalsIgnoreCase("java-default"))
				return s.trim().split("\\s+");
		}catch (Exception e) {}
		System.out.println("[Server #"+id+"] Cmdline: "+s);
		return new String[] {"java", "-Xmx"+this.getMemory()+"M", "-jar", "server.jar"};
	}
	
	/**
	 * Startet den Server.
	 * @return true bei erfolg
	 */
	public boolean start() {
		if(this.process != null && process.isAlive())
			return false;
        try {
        	ProcessBuilder builder = new ProcessBuilder(this.getCmdline());
    		builder.redirectErrorStream(true);
    		builder.directory(new File(this.dir.getPath()+File.separator+"files"));
			this.process = builder.start();
			this.consoleThread = new Thread(processReader);
	        this.consoleThread.start();
	        this.consoleWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
	        Ini i = this.getIni();
	        i.put("Server", "Recently-Started", Util.curDate());
	        i.store();
	        return true;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
        
	}

	public boolean stop() {
		return this.sendCommand("stop");
	}
	
	public boolean kill() {
		if(process != null && process.isAlive()) {
			System.out.println("[Server #"+id+"] wird zwangsbeendet . . .");
			this.process.destroyForcibly();
			return true;
		}else return false;
	}
	
	public boolean sendCommand(String cmd) {
		if(consoleWriter != null) try {
			this.consoleWriter.write(cmd+"\n");
			this.consoleWriter.flush();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} else return false;
	}
	
	private Runnable processReader = new Runnable() {
        public void run() {
        	try {
        		System.out.println("[SERVER #"+id+"] Wird gestartet . . .");
        		consolePrintln("\n\n"+Main.lang.hashie.get("c_server_starting"));
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String s;
                try {
                	while ((s = br.readLine()) != null) {
                        System.out.println("[SERVER #"+id+"]: " + s);
                        consolePrintln(s);
                    }
                }catch(IOException e) {
                	System.err.println("[!] IOE");
                }
                
                process.waitFor();
                System.out.println("[SERVER #"+id+"] Beendet. (" + process.exitValue() + ")");
                consolePrintln("\n\n"+Main.lang.hashie.get("c_server_exited").replace("%s", process.exitValue()+""));
                consoleWriter.close();
                consoleWriter = null;
                process.destroy();
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
    };
	private void consolePrintln(String x) {
		this.console += x + "\n";
	}
    
	
	public File getFile(String path) {
		return new File(this.dir.getPath() + File.separator + "files" + File.separator + path);
	}
	
	/**
	 * Erstellt einen Server im Deploymentverzeichnis und registriert ihn.
	 * @param desc Serverbeschreibung.
	 * @param port Port auf den der Server gelegt werden soll.
	 * @param template Name der Template (*.jar oder *.zip) anhand welcher der Server erstellt werden soll.
	 * @param memory Anzahl an RAM welche dem Server zugewiesen werden soll (in MB)
	 * @param Soll der Server automatisch beim Starten des Systems mitgestartet werden?
	 * @return ID des erstellten Servers, Bei error ein Negativer INT der den Errorcode darstellt.
	 */
	public static int Create(String desc, int port, String template, int memory, boolean autostart) {
		//Neue ID bestimmen
		int id = 0;
		for(String f : Main.deploymentDir.list()) {
			try {
				int i = Integer.parseInt(f);
				if(i > id)
					id = i;
			}catch (NumberFormatException e) {
				continue;
			}
		}
		id++;
		File serverdir;
		while(true) {
			serverdir = new File(Main.deploymentDir.getPath() + File.separator + id);
			if(serverdir.exists()) {
				id++;
				continue;
			}else break;
		}
		
		//Template check.
		if(!getTemplates().contains(template)) 
			return -1;
		
		
		//Ordner erstellen.
		serverdir.mkdir();
		File serverdir_files = new File(serverdir.getPath() + File.separator + "files");
		serverdir_files.mkdir();
		File serverdir_backups = new File(serverdir.getPath() + File.separator + "backups");
		serverdir_backups.mkdir();
		
		//server.ini erstellen.
		try {
			File inifile = new File(serverdir.getPath() + File.separator + "server.ini");
			if(!inifile.createNewFile())
				return -2;
			FileOutputStream f = new FileOutputStream(inifile);
			String defaultConfig = 
					"[Server]\n" + 
					"Desc="+desc.replace("\n", " ")+"\n" +
					"Memory="+memory+"M\n" +
					"Autostart="+Boolean.toString(autostart)+"\n" +
					"Cmdline=java-default\n" +
					"Time-Created="+Util.curDate()+"\n" +
					"\n";
			f.write(defaultConfig.getBytes("UTF-8"));
			f.flush();
			f.close();
		} catch (Throwable e) {
			e.printStackTrace();
			return -2;
		}
		
		//eula.txt erstellen.
		try {
			File fi = new File(serverdir_files.getPath() + File.separator + "eula.txt");
			if(!fi.createNewFile())
				return -3;
			FileOutputStream f = new FileOutputStream(fi);
			f.write("eula=true".getBytes("UTF-8"));
			f.flush();
			f.close();
		} catch (Throwable e) {
			e.printStackTrace();
			return -3;
		}
		
		//server.properties erstellen.
		try {
			File fi = new File(serverdir_files.getPath() + File.separator + "server.properties");
			if(!fi.createNewFile())
				return -3;
			FileOutputStream f = new FileOutputStream(fi);
			String s = "server-port=" + port;
			f.write(s.getBytes("UTF-8"));
			f.flush();
			f.close();
		} catch (Throwable e) {
			e.printStackTrace();
			return -3;
		}
		
		//Template kopieren.
		File f_template = new File("templates" + File.separator + template);
		if(f_template.getName().endsWith(".zip") || f_template.getName().endsWith(".ZIP")) {
			try {
				File dest = new File(serverdir_files.getPath() + File.separator + "server.zip");
				Util.copy(f_template, dest);
				if(!Util.unzip(dest, serverdir_files))
					return -6;
				else {
					File lin = new File(serverdir_files.getPath() + File.separator + "cmdline-temp.txt");
					if(lin.exists() && lin.isFile() && !lin.isDirectory()) {
						Ini iniichan = new Ini(new File(serverdir.getPath() + File.separator + "server.ini"));
					    Scanner sc = new Scanner(lin);
					    if(sc.hasNextLine()) {
					    	iniichan.put("Server", "Cmdline", sc.nextLine());
							iniichan.store();
					    }
					    sc.close();
					    lin.delete();
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				return -5;
			}
		}else if(f_template.getName().endsWith(".jar") || f_template.getName().endsWith(".jar")) {
			try {
				Util.copy(f_template, new File(serverdir_files.getPath() + File.separator + "server.jar"));
			} catch (Throwable e) {
				e.printStackTrace();
				return -5;
			}
		}else if(f_template.isDirectory()){
			try {
				Util.copy(f_template, serverdir_files);
			} catch (Throwable e) {
				e.printStackTrace();
				return -5;
			}
		}
		
		System.out.println("[Server #"+id+"] Erfolgreich erstellt.");
		Main.servers.add(new MCServer(id));
		return id;
	}

	/**
	 * L�scht einen Server aus dem Deploymentverzeichnis und deregistriert ihn.
	 * @param id ID des zu l�schenden Servers
	 * @return true bei Erfolg.
	 */
	public static boolean Delete(int id) {
		try {
			MCServer srvr = MCServer.getByID(id);
			if(srvr != null) {
				srvr.kill();
				Util.deleteFileOrFolder(new File(Main.deploymentDir.getPath() + File.separator + srvr.getID()).toPath());
				Main.servers.remove(srvr);
				System.out.println("[Server #"+id+"] Deleted.");
				return true;
			}
			else {
				System.err.println("404: Server #"+id+" nicht gefunden.");
				return false;
			}
		}catch(Throwable t) {
			System.err.println("Fehler beim l�schen von Server #"+id);
			t.printStackTrace();
			return false;
		}
		
	}

	/**
	 * Sucht einen MCServer nach ID
	 * 
	 * @param id Server-ID der gesuchten Servers
	 * @return Der gefundene Server. NULL falls nicht gefunden
	 */
	public static MCServer getByID(int id) {
		for(MCServer srv : Main.servers) {
			if(srv.id == id)
				return srv;
			else continue;
		}
		return null;
	}
	
	public static ArrayList<String> getTemplates() {
		ArrayList<String> s = new ArrayList<String>();
		File f = Main.templatesDir;
		if(!f.exists()) {
			f.mkdir();
		}else if(!f.isDirectory()) {
			System.err.println("[!] Fehler: Template-Verzeichnis nicht erstellbar.");
			return s;
		}
		for(String l : f.list())
			s.add(l);
		return s;
	}

	public ArrayList<String> getBackups() {
		ArrayList<String> s = new ArrayList<String>();
		File f = new File(this.dir.getPath() + File.separator + "backups");
		if(!f.exists()) {
			f.mkdir();
		}else if(!f.isDirectory()) {
			System.err.println("[!] Fehler: Backups-Verzeichnis nicht erstellbar.");
			return s;
		}
		for(String l : f.list())
			s.add(l);
		return s;
	}
	
	public boolean createBackup(String desc) {
		File zip = new File(this.dir.getPath() + File.separator + "backups" + File.separator + desc + ".zip");
		if(zip.exists()) {
			System.err.println("[!] [Server #\"+this.id+\"] Fehleler beim Erstellen des Backups: Ein Backup mit der Beschreibung \""+desc+"\" existiert bereits.");
			return false;
		}
		
		try {
			if(!zip.createNewFile()) {
				System.err.println("[!] [Server #"+this.id+"] Fehler beim Erstellen des Backups \""+desc+".zip\". (Datei kann nicht erstellt werden)");
				return false;
			}
			if(Util.mkzip(new File(this.dir.getPath() + File.separator + "files"), zip)) {
				System.out.println("[Server #"+this.id+"] Backup erstellt.");
				return true;
			}else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (SecurityException e) {
			System.err.println("[!] [Server #"+this.id+"] Fehler beim Erstellen des Backups \""+desc+".zip\". (Zugriff verweigert)");
			return false;
		}
	}
	
	public boolean deleteBackup(String desc) {
		File zip = new File(this.dir.getPath() + File.separator + "backups" + File.separator + desc + ".zip");
		if(zip.exists()) {
			if(zip.delete()) {
				System.out.println("[Server #"+this.id+"] Backup \""+desc+"\" gel�scht.");
				
				return true;
			}else {
				System.err.println("[!] [Server #"+this.id+"] Beim L�schen des Backups \""+desc+"\" ist ein Fehler aufgetreten.");
				return false;
			}
		}else {
			System.err.println("[!] Fehler: Zu l�schendes Backup existiert nicht.");
			return false;
		}
	}

	public boolean applyBackup(String desc) {
		if(this.getBackups().contains(desc+ ".zip")) {
			System.out.println("[Server #"+this.id+"] Backup \""+desc+"\" wird aufgespielt . . .");
			
			File backup = new File(this.dir.getPath() + File.separator + "backups" + File.separator + desc + ".zip");
			if(!backup.exists() || backup.isDirectory()) {
				System.err.println("[!] [Server #"+this.id+"] Beim Aufspielen des Backups \""+desc+"\" ist ein Fehler aufgetreten. (0)");
				return false;
			}
			try {
				Util.deleteFileOrFolder(new File(this.dir.getPath() + File.separator + "files").toPath());
			} catch (IOException e) {
				System.err.println("[!] [Server #"+this.id+"] Beim Aufspielen des Backups \""+desc+"\" ist ein Fehler aufgetreten. (1)");
				e.printStackTrace();
				return false;
			}
			File files_dir = new File(this.dir.getPath() + File.separator + "files");
			if(!files_dir.exists() && !files_dir.mkdir()) {
				System.err.println("[!] [Server #"+this.id+"] Beim Aufspielen des Backups \""+desc+"\" ist ein Fehler aufgetreten. (2)");
				return false;
			}
			if(!Util.unzip(backup, files_dir)) {
				System.err.println("[!] [Server #"+this.id+"] Beim Aufspielen des Backups \""+desc+"\" ist ein Fehler aufgetreten. (3)");
				return false;
			}
			System.out.println("[Server #"+this.id+"] Backup erfolgreich geladen.");
			return true;
		}else {
			System.err.println("[!] Fehler: Konnte unbekanntes Backup \""+desc+"\" nicht aufspielen.");
			return false;
		}
		
		
		
	}

	
	
	
}
