package oniicode.craftmgr.interfaces;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.ini4j.Ini;
import org.mindrot.jbcrypt.BCrypt;

import fi.iki.elonen.NanoHTTPD;
import oniicode.craftmgr.MCServer;
import oniicode.craftmgr.Main;
import oniicode.craftmgr.Util;

/**
 * Web INTERFACE - Bedient das System von einem Webserver aus.
 * @author Dargen_
 *
 */
public class HTTPD extends NanoHTTPD {
	
	private Ini config;
	
	private HashMap<UUID, HashMap<String, String>> sessions;
	
	public HTTPD(Ini config) {
		super(Integer.parseInt(config.get("HTTPD", "port")));
		this.config = config;
		this.sessions = new HashMap<UUID, HashMap<String, String>>();
	}
	
	public void Start() throws IOException {
		this.start(SOCKET_READ_TIMEOUT, true);
		System.out.println("Webinterface-Port: " + this.getListeningPort());
	}
	
	public void Stop() {
		this.stop();
		System.out.println("Webinterface deaktiviert.");
	}
	
	private int wrong_login_attempts = 0;
	
	@Override
    public Response serve(IHTTPSession session) {
		@SuppressWarnings("deprecation")
		Map<String, String> parms = session.getParms();
		UUID sessionid;
		
		String sessioncookie = new CookieHandler(session.getHeaders()).read("session");
		boolean isNewSession = false;
		if(sessioncookie == null) {
			isNewSession = true;
			sessioncookie = UUID.randomUUID().toString();
		}
		try {
			sessionid = UUID.fromString(sessioncookie);
		}catch(IllegalArgumentException e) {
			System.out.println("/!\\_ Sicherheitswarnung: Anfrage an Webschnittstelle mit fehlerhaftem Session-Cookie von "+session.getHeaders().get("http-client-ip")+" am "+new java.util.Date().toString()+" festgestellt. Wahrscheinlich wurde dieses Cookie manipuliert.");
			return newFixedLengthResponse("<html><body style=\"font-family: Arial;\">Denkst du ich merk das nicht? Dein Cookie welches die Session-UUID beinhalten soll, tr�gt etwas was wahrscheinlich nichtmal eine UUID ist.</body></html>");
		}
		if(!this.sessions.containsKey(sessionid))
			this.sessions.put(sessionid, new HashMap<String, String>());
		
		String body = Response.Status.INTERNAL_ERROR+"..";
		
		if(Method.GET.equals(session.getMethod()) && this.isAssetUrl(session.getUri())) { //REQUEST (ASSET)
			try {
				String name = session.getUri().substring(8);
				URL resource;
				if(name.contains(".."))
					return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, Response.Status.FORBIDDEN.getDescription());
				
				try {
					
					//resource = Resources.getResource("assets/"+name); REMOVED TO AVOID USAGE OF GUAVA -.-
					resource = Util.getResource(Main.class, "html/assets/"+name);
				}catch(IllegalArgumentException e) {
					return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, Response.Status.NOT_FOUND.getDescription());
				}
				
				BufferedReader r = new BufferedReader(new InputStreamReader(resource.openStream()));
				String s2 = "";
				String s;
				while ((s = r.readLine()) != null) {
					s2 += s + "\n";
				}
				if(name.endsWith(".js")) {
					s2 = this.parseLangPlaceholders(s2);
				}
				return newFixedLengthResponse(Response.Status.OK, this.getMimetype(resource.getFile()), s2);
			} catch (IOException e) {
				e.printStackTrace();
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, Response.Status.INTERNAL_ERROR.getDescription());
			}
			
		}else if(!this.isLoggedIn(sessionid)) { //REQUEST (NICHT EINGELOGGT)
			if(this.wrong_login_attempts >= 50)
				return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, Response.Status.FORBIDDEN.getDescription()+"\nZu viele falsche Anmeldeversuche.\n Bitte wende dich an den Systemadministrator.");
			if(Method.POST.equals(session.getMethod())) {
				Map<String, String> files = new HashMap<String, String>();
				try {
					session.parseBody(files);
				} catch (Throwable t) {
					t.printStackTrace();
					return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, Response.Status.INTERNAL_ERROR.getDescription());
				}
				String received_passwd = null;
				try {
					received_passwd = session.getQueryParameterString().split("=")[1].split("&")[0];
				}catch(ArrayIndexOutOfBoundsException e) {
					return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
				}
				Response res = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
				if(BCrypt.checkpw(received_passwd, this.config.get("HTTPD", "passwd"))) {
					this.setSessionData(sessionid, "logged_in", "true");
					res.addHeader("Location", "/");
					return res;
				}else {
					this.wrong_login_attempts++;
					res.addHeader("Location", "/?wrongpasswd");
					return res;
				}
			}else{
				body = this.getHtml("login");
				if(parms.containsKey("wrongpasswd")) 
					body = body.replace("{wrongpasswd}", Main.lang.hashie.get("passwd_worng")); 
				else
					body = body.replace("{wrongpasswd}", ""); 
			}
		}else { //REQUEST (EINGELOGGT)
			if(Method.GET.equals(session.getMethod())) {
				if(parms.containsKey("logout")) {
					Response res = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
					this.sessions.remove(sessionid);
					res.addHeader("Location", "/?loggedout");
					res.addHeader("Set-Cookie", "session=deleted; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT");
					return res;
				}else if(parms.containsKey("start")) {
					try {
						int id = Integer.parseInt(parms.get("start"));
						MCServer srv = MCServer.getByID(id);
						if(srv != null && srv.start()) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "start "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "start "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("stop")) {
					try {
						int id = Integer.parseInt(parms.get("stop"));
						MCServer srv = MCServer.getByID(id);
						if(srv != null && srv.stop()) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "stop "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "stop "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("kill")) {
					try {
						int id = Integer.parseInt(parms.get("kill"));
						MCServer srv = MCServer.getByID(id);
						if(srv != null && srv.kill()) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "kill "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "kill "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("cmd")) {
					try {
						int id = Integer.parseInt(parms.get("cmd"));
						MCServer srv = MCServer.getByID(id);
						if(parms.get("com") != null && srv != null && srv.sendCommand(parms.get("com"))) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "cmd "+id+" "+parms.get("com")+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "cmd "+id+" "+parms.get("com")+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("delete")) {
					try {
						int id = Integer.parseInt(parms.get("delete"));
						if(MCServer.Delete(id)) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "delete "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "delete "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("status")) {
					try {
						int id = Integer.parseInt(parms.get("status"));
						MCServer srv = MCServer.getByID(id);
						if(srv == null) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "NOT FOUND");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, srv.getState()+"");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("list")) {
					String s = "error";
					if(parms.get("list").equalsIgnoreCase("servers")) {
						s = "== REGISTERED SERVERS ==\n";
						for(MCServer srv : Main.servers) {
							s += srv.getID()+"_"+srv.getPort()+"_"+srv.getState()+"_"+srv.getDesc()+"\n";
						}
					}else if(parms.get("list").equalsIgnoreCase("templates")) {
						File f = new File("templates");
						
						if(!f.exists() || !f.isDirectory()) {
							s = "ERROR: Templateverzeichnis nicht gefunden.";
						}else {
							s = "== TEMPLATE LIST ==\n";
							for(String ss : f.list()) {
								s += ss + "\n";
							}
						}
					}
					return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, s);
				}else if(parms.containsKey("console")) {
					try {
						int id = Integer.parseInt(parms.get("console"));
						MCServer srv = MCServer.getByID(id);
						if(srv != null) {
							if(srv.getConsole() == null)
								return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "- "+Main.lang.hashie.get("c_empty")+" -");
							else
								return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, srv.getConsole());
						}
							
					}catch(NumberFormatException e) {}
					return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
				}else if(parms.containsKey("create")) {
					try {
						int port = Integer.parseInt(parms.get("create"));
						if(parms.get("template") != null && parms.get("memory") != null && parms.get("autostart") != null && parms.get("desc") != null) {
							int memory = Integer.parseInt(parms.get("memory"));
							int i = MCServer.Create(parms.get("desc"), port, parms.get("template"), memory, Boolean.parseBoolean(parms.get("autostart")));
							if(i >= 0) {
								return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "create SUCCESS "+i);
							}else {
								return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "create FAILURE "+i);
							}
						}else {
							return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("conf")) {
					try {
						int id = Integer.parseInt(parms.get("conf"));
						MCServer srv = MCServer.getByID(id);
						if(srv == null)
							body = "<p>404: Server #"+id+" nicht gefunden.</p><hr><a class=\"btn btn-primary\" href=\"/\">[Zur�ck]</a>";
						else {
							Ini ini = srv.getIni();
							for(Entry<String,String> e : parms.entrySet()) {
								if(parms.containsKey(e.getKey()) && parms.get(e.getKey()) != null && !e.getKey().equalsIgnoreCase("conf") && !e.getKey().equalsIgnoreCase("noscript"))
									ini.put("Server", e.getKey(), e.getValue());
							}
							try {
								ini.store();
								Response res = newFixedLengthResponse(Response.Status.REDIRECT, MIME_PLAINTEXT, "conf "+id+"\nSUCCESS");
								res.addHeader("Location", "/?server="+id+"&config=0&success");
								return res;
							} catch (IOException e) {
								e.printStackTrace();
								return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "conf "+id+"\nFAILURE");
							}
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("backup-create")) {
					try {
						int id = Integer.parseInt(parms.get("backup-create"));
						MCServer srv = MCServer.getByID(id);
						String desc = "backup_"+Util.curDate().replace(" ", "_");
						if(parms.containsKey("desc"))
							desc = parms.get("desc");
						
						if(srv != null && srv.createBackup(desc)) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "backup-create "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "backup-create "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("backup-apply")) {
					try {
						int id = Integer.parseInt(parms.get("backup-apply"));
						MCServer srv = MCServer.getByID(id);
						if(srv != null && parms.containsKey("desc") && srv.applyBackup(parms.get("desc"))) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "backup-apply "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "backup-apply "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				}else if(parms.containsKey("backup-delete")) {
					try {
						int id = Integer.parseInt(parms.get("backup-delete"));
						MCServer srv = MCServer.getByID(id);
						if(srv != null && parms.containsKey("desc") && srv.deleteBackup(parms.get("desc"))) {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "backup-delete "+id+"\nSUCCESS");
						}else {
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "backup-delete "+id+"\nFAILURE");
						}
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
					
				}else if(parms.containsKey("server")) {
					try {
						int id = Integer.parseInt(parms.get("server"));
						MCServer srv = MCServer.getByID(id);
						if(srv == null) {
							body = "<p>404: Server #"+id+" nicht gefunden.</p><hr><a class=\"btn btn-primary\" href=\"/\">[Zur�ck]</a>";
						}else if(parms.containsKey("config")){
							Ini ini = srv.getIni();
							String s = "";
							for(String key : ini.get("Server").keySet()) {
								if(key.equalsIgnoreCase("conf") || key.equalsIgnoreCase("set-conf"))
									continue;
								String ss = this.getHtml("server_config_entry").replace("{KEY}", key).replace("{VAL}", ini.get("Server", key));
								if(key.equalsIgnoreCase("Time-Created") || key.equalsIgnoreCase("Recently-Started"))
									ss = ss.replace("{RW}", "readonly");
								else
									ss = ss.replace("{RW}", "");
								s += ss;
							}
							body = this.parseServerPlaceholders(this.getHtml("server_config"), srv).replace("{CONF}", s);
							if(parms.containsKey("success"))
								body = body.replace("{SUCCESS}", "<p id=\"success\">�bernommen!</p>");
							else
								body = body.replace("{SUCCESS}", "");
						}else if(parms.containsKey("files")){
							body = this.parseServerPlaceholders(this.getHtml("server_files"), srv);
							String path = "";
							if(parms.get("files") != null && StringUtils.countMatches(parms.get("files"), "..")<=0) {
								path = parms.get("files");
							}
							if(path.startsWith("/") || path.startsWith("\\")) {
								path = path.substring(1);
							}
							if(path.endsWith("/") || path.endsWith("\\")) {
								path = path.substring(0, path.length() -1);
							}
							File p = srv.getFile(path);
							if(p.exists()) {
								if(p.isDirectory()) {
									String s = "";
									for(File f : p.listFiles()) {
										String st;
										if(f.isDirectory()) {
											st = this.parseServerPlaceholders(
													this.getHtml("server_files_entry")
													.replace("{I}", this.getHtml("server_files_entry_icon_folder"))
													.replace("{NAME}", "<a href=\"?server="+srv.getID()+"&files="+path+"/"+f.getName()+"\">"+f.getName()+"</a>")
													.replace("{DATE}", "")
													.replace("{SIZE}", f.listFiles().length + " Elemente")
													.replace("{TYPE}", "Ordner")
													, srv
												);  
										}else {
											st = this.parseServerPlaceholders(
													this.getHtml("server_files_entry")
													.replace("{I}", this.getHtml("server_files_entry_icon_file"))
													.replace("{NAME}", f.getName())
													.replace("{DATE}", Util.getDate(new Date(f.lastModified())))
													.replace("{SIZE}", Util.readableFileSize(f.length()))
													, srv
												); 
											try {
												String t = Files.probeContentType(f.toPath());
												if(t == null)
													t = "N/A";
												st = st.replace("{TYPE}", t);
											} catch (IOException e) {
												st = st.replace("{TYPE}", "<i>Fehler: "+e.getMessage()+"</i>");
												e.printStackTrace();
												continue;
											}
										}
										s+=st;
									}
									body = body.replace("{S_FILES}", s);
								}else {
									body = body.replace("{S_FILES}", "File Editor noch in Arbeit");
								}
								String pp = "";
								pp +="<li class=\"breadcrumb-item\"><a href=\"?server="+srv.getID()+"&files\">Server #"+srv.getID()+"</a></li>";
								String[] crumbs = path.split("/");
								for(int i = 0; i < crumbs.length; i++) {
									String s = crumbs[i];
									if(s.equals(p.getName()))
										pp +="<li class=\"breadcrumb-item active\">"+s+"</li>";
									else
										pp +="<li class=\"breadcrumb-item\"><a href=\"?server="+srv.getID()+"&files="+getPathToIndex(crumbs, i)+"\">"+s+"</a></li>";
									
								}
								body = body.replace("{S_FILES_BREADCRUMB}", pp);
							}else {
								body = body.replace("{S_FILES}", "<p>404: Not found. <a href=\"?server="+srv.getID()+"&files\">Zurück</a></p>");
							}
							
							
							
						}else if(parms.containsKey("backups")){
							String s = "";
							for(String ss : srv.getBackups()) {
								File f = new File(srv.getDir().getPath() + File.separator + "backups");
								s += this.parseServerPlaceholders(this.getHtml("server_backups_entry").replace("{DATE}", Util.getDate(new Date(f.lastModified()))).replace("{NAME}", ss.replace(".zip", "")).replace("{SIZE}", Util.readableFileSize(f.length())), srv);
							}
							body = this.parseServerPlaceholders(this.getHtml("server_backups"), srv).replace("{S_BACKUPS}", s);
						}else {
							body = this.parseServerPlaceholders(this.getHtml("server_console"), srv);
						}
						
					}catch(NumberFormatException e) {
						return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, Response.Status.BAD_REQUEST.getDescription());
					}
				
				}else {
					String s = "";
					File f = new File("templates");
					for(String l : f.list())
						s += "<option>" + l + "</option>";
					
					body = this.getHtml("serverlist").replaceAll("<TEMPLATES/>", s);
					//body = "<h2>CraftMGR Backend v1.0</h2><small>(c) Dargen_ 2018</small><hr><b>HTTP-Interface Reference</b><p>/?start=[ID]<br>/?=stop=[ID]<br>/?=kill=[ID]<br>/?=delete=[ID]<br>/?=cmd=[ID]&com=[COMMAND]<br>/?=console=[ID]<br>/?=create=[PORT]&template=[TEMPLATE]&memory=[RAM]&autostart=[true/false]&desc=[BESCHREIBUNG..]</p>";
				}
			
			}
			
		}
		String htmlresponse = this.getHtml("wi_root").replace("{0}", body);
		htmlresponse = this.parsePlaceholders(htmlresponse).replaceAll("\n", "").replaceAll("\t", "");
		htmlresponse = this.parseLangPlaceholders(htmlresponse);
		Response res = newFixedLengthResponse(htmlresponse);
		
        if(isNewSession && this.sessions.containsKey(sessionid)) {
        	res.addHeader("Set-Cookie", "session="+sessioncookie);
        }
        return res;
    }

	private String getPathToIndex(String[] crumbs, int i) {
		String ret = "";
		for(int c = 0; c <= i; c++) {
			ret += crumbs[c];
			if(!(c==i))
				ret+="/";
		}
		return ret;
	}

	/**
	 * �berpr�ft ob Benutzer von Session mit gegebener ID eingeloggt ist.
	 * @param session Session ID
	 * @return Username. NULL wenn Session nicht eingeloggt
	 */
	private boolean isLoggedIn(UUID session) {
		if(this.config.get("HTTPD", "passwd").equalsIgnoreCase("none"))
			return true;
		return this.sessions.get(session).get("logged_in") != null && this.sessions.get(session).get("logged_in") == "true";
	}
	
	/**
	 * �berpr�ft ob die angegebene URL einen g�ltigen Pfad eines Assets wiedergibt.
	 * @param url zu pr�fende URL
	 */
	private boolean isAssetUrl(String url) {
		int i = 0;
		for(char c : url.toCharArray())
			if(c=='/')
				i++;
		return i == 2 && url.startsWith("/assets/");
	}
	
	/**
	 * Ruft einer Session zugewiesenen Informationen ab
	 * @param session Session-ID
	 * @param key Wie bei 'ner HashMap
	 * @return Den von der bestimmten session dem bestimmten key zugeteilten wert, NULL wenn key oder session nicht gefunden.
	 */
	@SuppressWarnings("unused")
	private String getSessionData(UUID session, String key) {
		try {
			return this.sessions.get(session).get(key);
		}catch(NullPointerException npe) {
			return null;
		}
		
	}
	
	private void setSessionData(UUID session, String key, String value) {
		HashMap<String, String> map = this.sessions.get(session);
		if(map == null)
			map = new HashMap<String, String>();
		map.put(key, value);
		this.sessions.put(session, map);
	}
	
	private String getMimetype(String url) {
		String ex = "";
		int i = url.lastIndexOf('.');
		if (i > 0) {
		    ex = url.substring(i+1);
		}
		
		switch(ex) {
		case "txt":
			return MIME_PLAINTEXT;
		case "css":
			return "text/css";
		case "js":
			return "text/javascript";
		case "eot":
			return "application/vnd.ms-fontobject";
		case "woff":
			return "font/woff";
		case "woff2":
			return "font/woff2";
		case "ttf":
			return "font/ttf";
		case "svf":
			return "image/svg+xml";
		}
		
		return MIME_PLAINTEXT;
	}
	
	/**
	 * Besorgt HTML-Vorlage
	 * @param name Name der Vorlage
	 * @return NULL wenn nicht gefunden.
	 */
	private String getHtml(String name) {
		URL resource;
		try {
			resource = Util.getResource(Main.class, "html/"+name+".html");
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(resource.openStream()));
			String s2 = "";
			String s;
			while ((s = r.readLine()) != null) {
				s2 += s + "\n";
			}
			return s2;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("[!] Fehler beim Lesen von HTML-Vorlage \""+name+"\".");
			return null;
		}
	}
	
	/**
	 * Parst alle Datenplatzhalter in einem String
	 * 
	 * <table>
	 *  <tr>
	 *   <td>{ver}</td>
	 *   <td>Systemversion</td>
	 *  </tr>
	 *  <tr>
	 *   <td>{app}</td>
	 *   <td>Name dieses Systems</td>
	 *  </tr>
	 *  <tr>
	 *   <td>{loginformid}</td>
	 *   <td>Name/ID des INPUT-Tags wie er im HTML-Template sein soll</td>
	 *  </tr>
	 *  
	 * </table>
	 * @param html Eingabestring der geparst werden soll.
	 * @return String mit ersetzten Platzhaltern.
	 */
	private String parsePlaceholders(String html) {
		String s = html;
		s = s.replace("{ver}", Main.version);
		s = s.replace("{app}", Main.appname);
		s = s.replace("{loginformid}", "x");
		s = s.replace("{curdate}", Util.curDate());
		return s;
	}
	
	private String parseLangPlaceholders(String html) {
		String s = html;
		for(Map.Entry<String, String> m : Main.lang.hashie.entrySet()) {
			s = s.replace("{lang:"+m.getKey()+"}", m.getValue());
		}
		return s;
	}
	
	private String parseServerPlaceholders(String html, MCServer srv) {
		String s = html;
		s = s.replace("{S_ID}", srv.getID()+"");
		s = s.replace("{S_DESC}", srv.getDesc());
		s = s.replace("{S_PORT}", srv.getPort()+"");
		s = s.replace("{S_STATE}", srv.getState().toString());
		s = s.replace("{S_MEMORY}", srv.getMemory()+"");
		s = s.replace("{S_DATE_CREATED}", srv.getIni().get("Server", "Time-Created")+"");
		return s;
	}
}

