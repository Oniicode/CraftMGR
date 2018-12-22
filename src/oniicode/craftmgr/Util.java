package oniicode.craftmgr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Util {
	
	public static <T> T firstNonNull(T first, T second) {
	    if (first != null) {
	      return first;
	    }
	    if (second != null) {
	      return second;
	    }
	    throw new NullPointerException("Both parameters are null");
	  }
	
	public static URL getResource(Class<?> contextClass, String resourceName) {
	    URL url = contextClass.getResource(resourceName);
	    if(url==null)
	    	throw new IllegalArgumentException("resource "+resourceName+" relative to "+contextClass.getName()+" not found.");
	    return url;
	  }
	
	
	/**
	 * L�scht Datei oder Ordner samt Inhalt.
	 * @param path
	 * @throws IOException
	 */
	public static void deleteFileOrFolder(final Path path) throws IOException {
		System.out.println("L�sche: "+path.toString()+" ..");
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(final Path file, final IOException e) {
				return handleException(e);
			}

			private FileVisitResult handleException(final IOException e) {
				e.printStackTrace(); // replace with more robust error handling
				return FileVisitResult.TERMINATE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
				if (e != null)
					return handleException(e);
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	};
	
	/**
	 * Datei oder Ordner kopieren.
	 * @param sourceLocation von
	 * @param targetLocation nach
	 * @throws IOException Bei Fehler
	 */
	public static void copy(File sourceLocation, File targetLocation) throws IOException {
	    if (sourceLocation.isDirectory()) {
	        copyDirectory(sourceLocation, targetLocation);
	    } else {
	        copyFile(sourceLocation, targetLocation);
	    }
	}

	/**
	 * Ordner samt Inhalten kopieren
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	private static void copyDirectory(File source, File target) throws IOException {
	    if (!target.exists()) {
	        target.mkdir();
	    }

	    for (String f : source.list()) {
	        copy(new File(source, f), new File(target, f));
	    }
	}

	/**
	 * Datei Kopieren
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	private static void copyFile(File source, File target) throws IOException {        
	    try (
	            InputStream in = new FileInputStream(source);
	            OutputStream out = new FileOutputStream(target)
	    ) {
	        byte[] buf = new byte[1024];
	        int length;
	        while ((length = in.read(buf)) > 0) {
	            out.write(buf, 0, length);
	        }
	    }
	}
	
	/**
	 * Entpackt ZIP-Archiv
	 * @param zip Zu entpackende ZIP-Datei
	 * @param outputfolder Ordner in den die ZIP entpackt werden soll
	 * @return
	 */
	public static boolean unzip(File zip, File outputfolder) {
		int BUFFER = 512;
		
		if(!outputfolder.exists() && !outputfolder.mkdirs() || !outputfolder.isDirectory()) {
			System.err.println("[!] Fehler beim Entpacken: Ung�ltiger Ausgabepfad.");
			return false;
		}
		
		FileInputStream fis;
		try {
			fis = new FileInputStream(zip);
		} catch (FileNotFoundException e) {
			System.err.println("[!] Fehler beim Entpacken: ZIP-Datei \""+zip.getPath()+"\" nicht gefunden.");
			e.printStackTrace();
			return false;
		}
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
		try {
			while((entry = zis.getNextEntry()) != null) {
				
				System.out.println("Dekomprimiere: " + entry.getName() + " ..");
				int count; //U
				byte data[] = new byte[BUFFER];
				File target_f = new File(outputfolder.getPath() + File.separator + entry.getName());
				if(entry.isDirectory())
					continue;
				else if(!target_f.getParentFile().exists() && !target_f.getParentFile().mkdirs()) {
					System.err.println("[!] Fehler beim Entpacken: Konnte Ordnerpfad \""+target_f.getParentFile()+"\" nicht erstellen");
					return false;
				}
				FileOutputStream fos = new FileOutputStream(target_f);
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
				zis.closeEntry();
				fos.close();
			}
			zis.close();
			return true;
		} catch (IOException e) {
			System.err.println("[!] Fehler beim Dekomprimieren.");
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Gegenteil von unzip()
	 * @param folder Ordner dessem Inhalt in die ZIP soll
	 * @param zipfile Zu erstellende ZIP-Datei
	 * @return true bei Erfolg
	 */
	public static boolean mkzip(File folder, File zipfile) {
		try {
			FileOutputStream fos = new FileOutputStream(zipfile);
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
			
			writeDirToZip(folder, folder, zos);
			
			zos.close();
			fos.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("[!] Fehler beim Komprimieren. (Zipfile not found)");
			return false;
		} catch (IOException e) {
			System.err.println("[!] Fehler beim Komprimieren. (IOException)");
			e.printStackTrace();
			return false;
		}
	}
	
	private static void writeDirToZip(File basedir, File dir, ZipOutputStream zos) throws IOException {
		int BUFFER = 512;
		for(File f : dir.listFiles()) {
			if(!f.isDirectory()) {
				System.out.println("Komprimiere: " + f.getPath() + " ..");
				FileInputStream fis = new FileInputStream(f);
		        ZipEntry zipEntry = new ZipEntry(basedir.toURI().relativize(f.toURI()).getPath());
		        zos.putNextEntry(zipEntry);

		        byte[] bytes = new byte[BUFFER];
		        int length;
		        while ((length = fis.read(bytes)) >= 0) {
		            zos.write(bytes, 0, length);
		        }

		        zos.closeEntry();
		        fis.close();
			}else {
				writeDirToZip(basedir, f, zos);
			}
		}
	}
	
	public static String curDate() {
		return getDate(new Date());
	}
	
	public static String getDate(Date date) {
		return new SimpleDateFormat("dd-MM-yyyy HH-mm-ss z").format(date);
	}
	
	public static Date getDate(String s) {
		try {
			return new SimpleDateFormat("dd-MM-yyyy HH-mm-ss z").parse(s);
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static String readableFileSize(long size) {
	    if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
