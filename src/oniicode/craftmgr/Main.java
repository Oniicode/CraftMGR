package oniicode.craftmgr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import org.ini4j.Ini;

import oniicode.craftmgr.interfaces.CLI;
import oniicode.craftmgr.interfaces.HTTPD;
import oniicode.craftmgr.interfaces.SQ;

public class Main {

	public static String version = "v1.6pb";
	public static String appname = "CraftMGR";
	public static Ini config;
	public static Lang lang;
	
	public static File deploymentDir;
	public static File templatesDir;
	public static ArrayList<MCServer> servers;
	
	/* Interfaces ^_^ */
	private static CLI cli; //Command line
	public static HTTPD httpd; //Web interface
	public static SQ sq; //ServerSocket
	
	public static String textheader = "C r a f t  M G R\n(c) Oniicode 2018\n";
	
	public static void main(String[] args) {
		System.out.println(textheader);
		System.out.println("Version: " + version);
		
		//Environment Check
		try {
			System.out.println("Deployment-Path: " + Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (Exception e1) {
			System.out.println("Deployment-Path: " + "ERROR");
			return;
		}
		
		//Load Config
		if(!loadConfig())return;
		
		//Load Human Language
		lang = new Lang(config.get("Language", "language"));
		try {
			lang.load();
		} catch (Exception e2) {
			System.err.println("[!] Error: Couldn't load human language file.");
			e2.printStackTrace();
			return;
		}
		
		File templates = new File("templates");
		if(!templates.exists()) if(!templates.mkdir()) {
				System.err.println("[!] Error: Couldn't create templates directory.");
				return;
		}
		
		
		
		//Init Dirs
		if(!loadDirs())return;
		
		//load MCServer
		loadServers();
		
		//Try to start web interface until its running, if enabled.
		int trys = 3;
		while (trys>0) {
			if(!(config.get("HTTPD").get("enabled").equalsIgnoreCase("true"))) {
				System.out.println("Webserver-Port: <DEAKTIVIERT>");
				break;
			}
			try {
				httpd = new HTTPD(config);
				httpd.Start();
			} catch (Throwable e) {
				System.err.println("[!] Fehler beim Starten des Web Interface. (Versuche noch "+trys+" mal)");
				e.printStackTrace();
				try {
					Thread.sleep(1200L);
				} catch (InterruptedException e1) {
					System.out.println("Interrupted.");
					break;
				}
				trys--;
				continue;
			}
			break;
		}
		
		trys = 3;
		while (trys>0) {
			if(!(config.get("ServerQuery").get("enabled").equalsIgnoreCase("true"))) {
				System.out.println("Query-Port: <DEAKTIVIERT>");
				break;
			}
			try {
				sq = new SQ(config);
				sq.Start();
			} catch (Throwable e) {
				System.err.println("/!\\ Fehler beim Starten des ControlSocket Interface. (Versuche noch "+trys+" mal)");
				e.printStackTrace();
				try {
					Thread.sleep(1200L);
				} catch (InterruptedException e1) {
					System.out.println("Abgebrochen.");
					break;
				}
				trys--;
				continue;
			}
			break;
		}
		
		//Shutdown Hook
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
		    @Override
		    public void run()
		    {
		    	if(httpd != null)
		    		httpd.Stop();
		    	for(MCServer srv : servers)
					srv.kill();
		    	System.out.println("\n\nBye, Hacker-san :3");
		    }
		});
		
		System.out.println("\nDone! Gib \"help\" ein, um Befehle aufzulisten");
		
		//Finally, start the [C]ommand [L]ine [I]nterface
		cli = new CLI();
		Scanner scan = new Scanner(System.in);
		while (scan.hasNext()){
			String s = scan.nextLine();
			String[] cmd = s.trim().split("\\s+");
			
			if(cmd[0].equalsIgnoreCase("quit")) {
				break;
			}else {
				cli.command(cmd);
			}
			System.out.println("");
			continue;
		}
		scan.close();
	}
	
	/**
	 * Initialisiert Einstellungsdatei
	 * @return
	 */
	private static boolean loadConfig() {
		File inifile = new File("config.ini");
		if(!inifile.exists()) { //Erstelle Default wenn nicht gefunden.
			System.out.println("Einstellungsdatei nicht gefunden, erstelle Default.");
			try { 
				FileOutputStream f = new FileOutputStream(inifile); 
				String defaultConfig = 
						"[Language]\n"+
						"language=en_GB\n"+
						"[HTTPD]\n" + 
						"enabled=true\n" +
						"port=9000\n" +
						"passwd=$2a$10$GKrVPAsVEFYYUS1di0iej.A8f2oimGTnoAo0xPBDX/TAugr9Rf5Na\n" + //Default: imnotgerman
						"\n" +
						"[ServerQuery]\n" +
						"enabled=false\n" +
						"port=9002\n" +
						"authkey=h1zZdasIsjfelAdfo93Ashj31erHeilSatan666asadsdfLolicon5\n" +
						"\n" +
						"[Dirs]\n" +
						"templates-dir=templates\n" +
						"deploymnt-dir=servers\n" +
						"\n" +
						"[Limits]\n" +
						"port-range=25000-25999";
				f.write(defaultConfig.getBytes("UTF-8"));
				f.flush();
				f.close();
			} catch (IOException e) {
				System.err.println("/!\\_ Fehler beim erstellen der Default-Einstellungsdatei.");
				e.printStackTrace();
			}
		}
		try {
			config = new Ini(inifile);
		} catch (IOException e) {
			System.err.println("/!\\_ Fehler beim Laden der Einstellungsdatei.");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Initialisiert alle Verzeichnisse nach Einstellungsdatei.
	 * Obviously: Die Einstellungsdatei muss daf�r vorher geladen worden sein.
	 * @return false bei Fehler
	 */
	private static boolean loadDirs() {
		//Server-Deployment-Verzeichnis
		if(config.get("Dirs", "deploymnt-dir") == null) {
			System.err.println("[!] Fehler: Einstellungsdatei ung�ltig. (deploymnt-dir = null !!!)");
			return false;
		}
		deploymentDir = new File(config.get("Dirs", "deploymnt-dir"));
		if(!deploymentDir.exists()) {
			System.out.println("Deploymentverzeichnis nicht gefunden, erstelle Default.");
			deploymentDir.mkdir();
		}
		else if(deploymentDir.isFile()) {
			System.err.println("[!] Fehler beim Erstellen des Deploymentverzeichnisses.");
			return false;
		}
		
		//Servervorlagen-Verzeichnis
		if(config.get("Dirs", "templates-dir") == null) {
			System.err.println("[!] Fehler: Einstellungsdatei ung�ltig. (templates-dir = null !!!)");
			return false;
		}
		templatesDir = new File(config.get("Dirs", "templates-dir"));
		if(!deploymentDir.exists()) {
			System.out.println("Servervorlagenverzeichnis nicht gefunden, erstelle Default.");
			deploymentDir.mkdir();
		}
		else if(deploymentDir.isFile()) {
			System.err.println("[!] Fehler beim Erstellen des Servervorlagenverzeichnisses.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Initialisiert Server im Deploymentverzeichnis.
	 */
	private static void loadServers() {
		servers = new ArrayList<MCServer>();
		for(File f : deploymentDir.listFiles()) {
			if(!f.isDirectory())
				continue;
			if(!Arrays.asList(f.list()).contains("files"))
				continue;
			try {
				servers.add(new MCServer(Integer.parseInt(f.getName())));
			}catch (NumberFormatException ex) {
				continue;
			}
		}
	}
}
