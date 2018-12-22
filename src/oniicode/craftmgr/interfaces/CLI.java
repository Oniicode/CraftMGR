package oniicode.craftmgr.interfaces;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.ini4j.Ini;

import oniicode.craftmgr.MCServer;
import oniicode.craftmgr.Main;
import oniicode.craftmgr.Util;

/**
 * Command Line INTERFACE - bedient das System vom Terminal aus.
 * 
 * Ausserdem:
 * Check mal wie viele zeilen diese Klasse hat :3
 * 
 * @author Dargen_
 *
 */
public class CLI {
	
	public PrintStream out;
	public PrintStream err;
	
	public abstract interface Command {
		
		public abstract String getName();
		
		public abstract String getUsage();

		public abstract String getDescription();

		public abstract String[] getAliases();

		public abstract boolean onCommand(String[] args, PrintStream out, PrintStream err);
	}
	
	private ArrayList<Command> commands = new ArrayList<Command>();
	
	public CLI() {
		this(System.out, System.err);
	}
	
	public CLI(PrintStream out, PrintStream err) {
		this.out = out;
		this.err = err;
		this.commands.add(new CommandList());
		this.commands.add(new CommandStart());
		this.commands.add(new CommandStop());
		this.commands.add(new CommandKill());
		this.commands.add(new CommandHttpd());
		this.commands.add(new CommandCommand());
		this.commands.add(new CommandVersion());
		this.commands.add(new CommandCreate());
		this.commands.add(new CommandDelete());
		this.commands.add(new CommandConfig());
		this.commands.add(new CommandBackup());
	}

	private Command getCommand(String name) {
		for(Command cmd : this.commands)
			if(cmd.getName().equalsIgnoreCase(name) || Arrays.asList(cmd.getAliases()).contains(name))
				return cmd;
		return null;
	}
	
	public boolean command(String[] in) {
		Command cmd = this.getCommand(in[0]);
		if(in[0].equalsIgnoreCase("help") || in[0].equalsIgnoreCase("?")) {
			out.println("== COMMAND LIST ==");
			for(Command c : this.commands) {
				out.println(c.getName() + " - " + c.getDescription());
			}
			out.println("quit - F�hrt alle Server herunter und so du weisst was ich mein");
			return true;
		}else if(cmd==null){
			err.println("Unbekannter Befehl. Benutze \"help\" oder \"?\" um alle Befehle aufzulisten.");
			return false;
		}else {
			String c;
			if(in.length > 1) {
				c = in[1];
				for (int i = 2; i < in.length; i++) {
					c = c + " "+ in[i];
				}
			}else {
				c = "";
			}
			boolean b = cmd.onCommand(c.split("\\s+"), this.out, this.err);
			this.out.println("");
			return b;
		}
	}
	
	/**
	 * Command zum Auflisten von Servern oder Templates.
	 * @author Dargen_
	 *
	 */
	public class CommandList implements Command {

		@Override
		public String getName() { return "list"; }

		@Override
		public String getUsage() { return "list [servers/templates]"; }

		@Override
		public String getDescription() { return "Listet alle regisrierten Server oder Templates auf."; }

		@Override
		public String[] getAliases() {
			return new String[] {"ls", "select"};
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args.length == 1) {
				if(args[0].equalsIgnoreCase("servers") || args[0].equalsIgnoreCase("s")) {
					out.println("== SERVER DIRECTORY ==");
					if(Main.servers.size() == 0)
						out.println("None.");
					else
					for(MCServer srv : Main.servers) {
						out.println("#" + srv.getID()+" - "+srv.getPort()+" - "+srv.getState().toString()+" - "+srv.getDesc());
					}
					return true;
				}else if(args[0].equalsIgnoreCase("templates") || args[0].equalsIgnoreCase("t")) {
					out.println("== TEMPLATE DIRECTORY ==");
					for(String s : MCServer.getTemplates()) {
						out.println(s);
					}
					return true;
				}else {
					err.println("Syntax: " + this.getUsage());
					return false;
				}
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum Starten eines Servers.
	 * @author Dargen_
	 *
	 */
	public class CommandStart implements Command {

		@Override
		public String getName() { return "start"; }

		@Override
		public String getUsage() { return "start <ID>[ID,ID,ID..]"; }

		@Override
		public String getDescription() { return "Startet Server mit gegebener/n ID/s"; }

		@Override
		public String[] getAliases() {
			return new String[] {"exec"};
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
			if(args.length == 1) {
				try {
					String[] ids = args[0].split(",");
					for(String sid : ids) {
						MCServer srvr = MCServer.getByID(Integer.parseInt(sid));
						if(srvr != null) {
							srvr.start();
						}
						else {
							err.println("404: Server #"+sid+" nicht gefunden.");
						}
					}
					return true;
					
				} catch (NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[0]+"\".");
				} catch (Throwable e) {
					err.println("[!] Fehler beim Starten von Server #"+args[0]);
					e.printStackTrace();
				}
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum starten eines Servers.
	 * @author Dargen_
	 *
	 */
	public class CommandStop implements Command {

		@Override
		public String getName() { return "stop"; }

		@Override
		public String getUsage() { return "stop <ID>[ID,ID,ID..]"; }

		@Override
		public String getDescription() { return "Weist gegebene/n Server an herunterzufahren."; }

		@Override
		public String[] getAliases() {
			return new String[] {};
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
			if(args.length == 1) {
				try {
					String[] ids = args[0].split(",");
					for(String sid : ids) {
						MCServer srvr = MCServer.getByID(Integer.parseInt(sid));
						if(srvr != null) {
							srvr.stop();
						}
						else {
							err.println("404: Server #"+sid+" nicht gefunden.");
						}
					}
					return true;
				} catch (NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[0]+"\".");
				} catch (Throwable e) {
					err.println("[!] Fehler beim Stoppen von Server #"+args[0]);
					e.printStackTrace();
				}
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum Zwangsherunterfahren eines Servers.
	 * @author Dargen_
	 *
	 */
	public class CommandKill implements Command {

		@Override
		public String getName() { return "kill"; }

		@Override
		public String getUsage() { return "kill <ID>[ID,ID,ID..]"; }

		@Override
		public String getDescription() { return "F�hrt Server mit gegebener/n ID/s zwanghaft herunter."; }

		@Override
		public String[] getAliases() {
			return new String[] {};
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
			if(args.length == 1) {
				try {
					String[] ids = args[0].split(",");
					for(String sid : ids) {
						MCServer srvr = MCServer.getByID(Integer.parseInt(sid));
						if(srvr != null) {
							srvr.kill();
						}
						else {
							err.println("404: Server #"+sid+" nicht gefunden.");
						}
					}
					return true;
				} catch (NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[0]+"\".");
				} catch (Throwable e) {
					err.println("[!] Fehler beim Killen von Server #"+args[0]);
					e.printStackTrace();
				}
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum Aktivieren und Deaktivieren des Webservers.
	 * @author Dargen_
	 *
	 */
	public class CommandHttpd implements Command {

		@Override
		public String getName() { return "httpd"; }

		@Override
		public String getUsage() { return "httpd [start/stop]"; }

		@Override
		public String getDescription() { return "Aktiviert oder deaktiviert die integrierte Webschnittstelle."; }

		@Override
		public String[] getAliases() {
			return new String[] { "wi" };
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
			if(args.length == 1) {
				if(args[0].equalsIgnoreCase("start")) {
					try {
						Main.httpd = new HTTPD(Main.config);
						Main.httpd.Start();
						return true;
					} catch (Throwable e) {
						err.println("[!] Fehler beim Starten der Webschnittstelle.");
						e.printStackTrace();
						return false;
					}
					
				}else if(args[0].equalsIgnoreCase("stop")) {
					Main.httpd.Stop();
					return true;
				}
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum Senden eines Befehls an einen laufenden Server.
	 * @author Dargen_
	 *
	 */
	public class CommandCommand implements Command {

		@Override
		public String getName() { return "cmd"; }

		@Override
		public String getUsage() { return "cmd <ID> <COMMAND..>"; }

		@Override
		public String getDescription() { return "Sendet gegebenen Befehl an laufenden Server mit gegebener ID."; }

		@Override
		public String[] getAliases() {
			return new String[] { "com", "c", "/" };
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
			if(args.length > 1) {
				try {
					MCServer srvr = MCServer.getByID(Integer.parseInt(args[0]));
					if(srvr != null) {
						String c = args[1];
						if(args.length >= 2)
						for (int i = 2; i < args.length; i++) {
							c = c + " "+ args[i];
						}
						srvr.sendCommand(c);
					}
					else
						err.println("[!] 404: Server #"+args[0]+" nicht gefunden.");
				} catch (NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[0]+"\".");
				} catch (Throwable e) {
					err.println("[!] Fehler beim senden des Befehls an Server #"+args[0]);
					e.printStackTrace();
				}
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Command zum anzeigen der Version.
	 * @author Dargen_
	 *
	 */
	public class CommandVersion implements Command {

		@Override
		public String getName() { return "version"; }

		@Override
		public String getUsage() { return "version"; }

		@Override
		public String getDescription() { return "Zeigt die Version dieses Systems an."; }

		@Override
		public String[] getAliases() {
			return new String[] { "ver" };
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			out.println("System-Version: " + Main.version);
			return true;
		}
		
	}
	
	/**
	 * Command zum erstellen eines Servers.
	 * @author Dargen_
	 *
	 */
	public class CommandCreate implements Command {

		@Override
		public String getName() { return "create"; }

		@Override
		public String getUsage() { return "create <PORT> <TEMPLATE> <RAM (MB)> <AUTOSTART (true/false)> <DESC..>"; }

		@Override
		public String getDescription() { return "Erstellt einen neuen Server."; }

		@Override
		public String[] getAliases() {
			return new String[] { "mk", "add" };
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
				
			if(args.length > 3) {
				int port;
				try {
					port = Integer.parseInt(args[0]);
				}catch(NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[0]+"\".");
					return false;
				}
				
				int memory;
				try {
					memory = Integer.parseInt(args[2]);
				}catch(NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[2]+"\".");
					return false;
				}
				
				String desc = args[4];
				if(args.length > 5)
				for (int i = 5; i < args.length; i++) {
					desc = desc + " "+ args[i];
				}
				
				boolean autostart = false;
				try {
					autostart = Boolean.parseBoolean(args[3]);
				}catch(Throwable e) {
					err.println("Ung�ltige Eingabe: \""+args[3]+"\".");
					e.printStackTrace();
					return false;
				}
				
				int id = MCServer.Create(desc, port, args[1], memory, autostart);
				
				if(id >= 0)
					return true;
				else {
					err.println("[!] Fehler beim erstellen des Server ("+id+")");
					return false;
				}
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum l�schen eines Servers.
	 * @author Dargen_
	 *
	 */
	public class CommandDelete implements Command {

		@Override
		public String getName() { return "delete"; }

		@Override
		public String getUsage() { return "delete <ID>[ID,ID,ID..]"; }

		@Override
		public String getDescription() { return "L�scht Server mit angegebener ID"; }

		@Override
		public String[] getAliases() {
			return new String[] { "del", "rm", "drop" };
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args[0]=="") {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
			if(args.length == 1) {
				String[] c = args[0].split(",");
				for(String s : c) {
					try {
						MCServer.Delete(Integer.parseInt(s));
					} catch (NumberFormatException e) {
						err.println("Ung�ltige Eingabe: \""+s+"\".");
					} catch (Throwable e) {
						err.println("[!] Fehler beim L�schen von Server #"+s);
						e.printStackTrace();
					}
				}
				
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * Befehl zum Editieren eines Servers (server.ini)
	 * @author Dargen_
	 *
	 */
	public class CommandConfig implements Command {

		@Override
		public String getName() { return "conf"; }

		@Override
		public String getUsage() { return "conf <ID> [SETTING] [VALUE..]"; }

		@Override
		public String getDescription() { return "Verwaltet Einstellungsdatei eines Servers (server.ini)."; }

		@Override
		public String[] getAliases() {
			return new String[] { "cfg", "ini", "c" };
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) {
			if(args.length >= 1) {
				try {
					int id = Integer.parseInt(args[0]);
					MCServer srv = MCServer.getByID(id);
					if(srv == null) {
						err.println("[!] 404: Server #"+id+" nicht gefunden.");
						return false;
					}else {
						Ini ini = srv.getIni();
						if(ini == null)
							return false;
						if(args.length == 1) {
							out.println("== SERVER CONFIG FILE #"+id+" ==");
							
							for(String val : ini.get("Server").keySet())
								out.println(val + ": " + ini.get("Server", val));
							return true;
							
						}else if(args.length == 2) {
							out.println("[Server #"+id+"] "+args[1]+": "+ini.get("Server", args[1]));
							return true;
						}else {
							String val = args[2];
							if(args.length > 3)
							for (int i = 3; i < args.length; i++) {
								val = val + " "+ args[i];
							}
							
							ini.put("Server", args[1], val);
							ini.store();
							
							out.println("[Server #"+id+"] "+args[1]+": "+ini.get("Server", args[1]));
							return true;
						}
					}
				} catch (NumberFormatException e) {
					err.println("Syntax: " + this.getUsage());
				} catch (Throwable e) {
					err.println("[!] Ein Fehler ist aufgetreten.");
					e.printStackTrace();
				}
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
	
	/**
	 * 
	 * @author Dargen_
	 *
	 */
	public class CommandBackup implements Command { 

		@Override
		public String getName() { return "backup"; }

		@Override
		public String getUsage() { return "backup <ID>[ID,ID,ID..] <list/create/delete/apply> [Beschreibung]"; }

		@Override
		public String getDescription() { return "Verwaltet Serverbackups."; }

		@Override
		public String[] getAliases() {
			return new String[] {"b", "backups"};
		}

		@Override
		public boolean onCommand(String[] args, PrintStream out, PrintStream err) { //TODO
			if(args.length >= 2) {
				
				try {
					String[] ids = args[0].split(",");
					for(String sid : ids) {
						MCServer srvr = MCServer.getByID(Integer.parseInt(sid));
						if(srvr != null) {
							if(args[1].equalsIgnoreCase("list")) {
								out.println("== BACKUP DIRECTORY #"+srvr.getID()+" ==");
								for(String s : srvr.getBackups())
									out.println(s);
								
							}else if(args[1].equalsIgnoreCase("create")) {
								String desc = "backup_"+Util.curDate().replace(" ", "_");
								if(args.length >= 3) {
									desc = args[2];
									if(args.length > 3)
									for (int i = 3; i < args.length; i++) {
										desc = desc + " "+ args[i];
									}
								}
								srvr.createBackup(desc);
								
							}else if(args[1].equalsIgnoreCase("delete")) {
								if(args.length >= 3) {
									String desc = args[2];
									if(args.length > 3)
									for (int i = 3; i < args.length; i++) {
										desc = desc + " "+ args[i];
									}
									srvr.deleteBackup(desc);
								}else {
									err.println("Syntax: " + this.getUsage());
									return false;
								}
							}else if(args[1].equalsIgnoreCase("apply")) {
								if(args.length >= 3) {
									String desc = args[2];
									if(args.length > 3)
									for (int i = 3; i < args.length; i++) {
										desc = desc + " "+ args[i];
									}
									srvr.applyBackup(desc);
								}else {
									err.println("Syntax: " + this.getUsage());
									return false;
								}
								
							}else {
								err.println("Syntax: " + this.getUsage());
								return false;
							}
						}
						else {
							err.println("404: Server #"+sid+" nicht gefunden.");
						}
					}
					return true;
					
				} catch (NumberFormatException e) {
					err.println("Ung�ltige Eingabe: \""+args[0]+"\".");
				} 
				return false;
			}else {
				err.println("Syntax: " + this.getUsage());
				return false;
			}
		}
		
	}
}