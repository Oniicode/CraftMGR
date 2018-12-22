package oniicode.craftmgr.interfaces;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.ini4j.Ini;

import oniicode.craftmgr.Main;

public class SQ {

	private ServerSocket sock;
	private int port;
	private Thread run;
	
	public SQ(Ini config) {
		this.port = Integer.parseInt(config.get("ServerQuery", "port"));
	}

	public void Start() throws IOException {
		this.sock = new ServerSocket(this.port);
		this.run = new Thread(runner);
		this.run.start();
		System.out.println("Query-Port: "+this.port);
	}
	
	public void Stop() throws IOException {
		this.run.interrupt();
		this.sock.close();
		this.sock = null;
		System.out.println("ServerQuery deaktiviert.");
	}
	
	private Runnable runner = new Runnable() {
		public void run() {
			while (true) {
				try {
					new Thread(new Controller(sock.accept())).start();
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	};
	
	class Controller implements Runnable{

		private Socket csock;
		
		BufferedReader in; //Received from Client
		PrintStream out; //Out to Client
		
		private CLI cli;
		
		public Controller(Socket csock) throws IOException {
			this.csock = csock;
			System.out.println("[i] "+csock.getInetAddress().toString()+" verbindet sich mit ServerQuery.");
			this.in = new BufferedReader(new InputStreamReader(this.csock.getInputStream()));
			this.out = new PrintStream(this.csock.getOutputStream());
			this.cli = new CLI(this.out, this.out);
		}
		
		private boolean hasGreeted = false;
		private boolean isAuthed = false;
		
		@Override
		public void run() {
			while(csock.isConnected()) {
				try {
					String s = in.readLine();
					if(s != null)
						this.read(s);
					else{
						System.out.println("[i] Client von ServerQuery getrennt.");
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
			return;
		}
		
		
		
		private void read(String s) throws IOException {
			if(!hasGreeted) {
				if(s.equalsIgnoreCase("hail satan")) {
					hasGreeted = true;
					this.out.println("hail satan, passwd pls");
				}
				return;
			}
			if(!isAuthed) {
				if(s.equals(Main.config.get("ServerQuery", "authkey"))) {
					isAuthed = true;
					System.out.println("[i] "+csock.getInetAddress().toString()+" am ServerQuery authentifiziert.");
					this.out.println();
					this.out.println(Main.textheader);
					this.out.println("Version: " + Main.version+"\n");
				}else {
					this.out.println("piss off");
				}
			} else {
				String[] cmd = s.trim().split("\\s+");
				if(cmd[0].equalsIgnoreCase("quit"))
					this.out.println("Quit-Befehl per ServerQuery nicht verf�gbar.");
				else
					this.cli.command(cmd);
			}
		}
		
	}
}
