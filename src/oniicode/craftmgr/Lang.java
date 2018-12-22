package oniicode.craftmgr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * UI Language Loader
 * @author Oniicode
 *
 */
public class Lang {
	
	public HashMap<String, String> hashie;
	
	
	private String primary = "en_GB";
	private String secondary = "en_US";
	
	/**
	 * @param primary Desired language. Entries from here override those from secondary language file.
	 * @param secondary Secondary (Fallback) language file. Entries will be read from here, if not found in Primary.
	 */
	public Lang(String primary, String secondary) {
		this.primary = primary;
		this.secondary = secondary;
	}
	
	public Lang(String primary) {
		this.primary = primary;
	}
	
	/**
	 * Constructor for default Language
	 */
	public Lang() {}
	
	
	/**
	 * Builds Hashmap
	 * @throws IOException on Error
	 */
	public void load() throws Exception {
		this.hashie = new HashMap<String, String>();
		// Load Secondary "Fallback" language file first
		ArrayList<String> secondary = this.readLangfile(this.secondary);
		for(String s : secondary) {
			if(s.contains("=")) {
				String[] d = s.split("=");
				this.hashie.put(d[0], d[1]);
			}
		}
		
		ArrayList<String> primary = this.readLangfile(this.primary);
		for(String s : primary) {
			if(s.contains("=")) {
				String[] d = s.split("=", 2);
				this.hashie.put(d[0], d[1]);
			}
		}
	}
	
	private ArrayList<String> readLangfile(String lang) throws Exception {
		URL res = Util.getResource(Main.class, "lang/"+this.secondary+".lang");
		BufferedReader r = new BufferedReader(new InputStreamReader(res.openStream()));
		ArrayList<String> l = new ArrayList<String>();
		String s;
		while ((s = r.readLine()) != null) {
			l.add(s);
		}
		return l;
	}
	
}
