//Oto Ģigulis 211RDB013 9.grupa
//Jānis Aveniņš 211RDB020 9.grupa
//Līva Puķīte 211RDB036 9.grupa
//Anastasija Šarakova 211RDB093 9.grupa

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;


public class Main {


	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		String choiseStr;
		String sourceFile, resultFile, firstFile, secondFile;
		
		loop: while (true) {
			
			System.out.println("Enter choice:");
			choiseStr = sc.next();
								
			switch (choiseStr) {
			case "comp":
				System.out.print("source file name: ");
				sourceFile = sc.next();
				System.out.print("archive name: ");
				resultFile = sc.next();
				comp(sourceFile, resultFile); // TODO
				break;
			case "decomp":
				System.out.print("archive name: ");
				sourceFile = sc.next();
				System.out.print("file name: ");
				resultFile = sc.next();
				decomp(sourceFile, resultFile); // TODO
				break;
			case "size":
				System.out.print("file name: ");
				sourceFile = sc.next();
				size(sourceFile);
				break;
			case "equal":
				System.out.print("first file name: ");
				firstFile = sc.next();
				System.out.print("second file name: ");
				secondFile = sc.next();
				System.out.println(equal(firstFile, secondFile)); 
				break;
			case "about":
				about();
				break;
			case "exit":
				break loop;
			}
		}

		sc.close();
	}

	public static void comp(String sourceFile, String resultFile) {
		byte[] bytes = fileToBytes(sourceFile);
		System.out.println(Arrays.toString(bytes));
		// TODO insert logic func here
	}
	
	public static byte[] fileToBytes(String filePath) {
		
		File file = new File(filePath);
		FileInputStream fileIn = null;
		FileChannel ch = null;
		StringBuilder output = new StringBuilder();
		int size;
		MappedByteBuffer buffer;
		byte[] bytes;
		
		try {
			fileIn = new FileInputStream(file);
			ch = fileIn.getChannel();
			size = (int) ch.size();
			buffer = ch.map(MapMode.READ_ONLY, 0, size);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {	
				if (fileIn != null) fileIn.close();
				if (ch != null) ch.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		bytes = new byte[size];
		buffer.get(bytes);
		
		return bytes;
	}

	public static String binToFile(String filePath) {
		
		File file = new File(filePath);
		FileInputStream fileIn = null;
		FileChannel ch = null;
		StringBuilder output = new StringBuilder();
		int size;
		MappedByteBuffer buffer;
		byte[] bytes;
		
		try {
			fileIn = new FileInputStream(file);
			ch = fileIn.getChannel();
			size = (int) ch.size();
			buffer = ch.map(MapMode.READ_ONLY, 0, size);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {	
				if (fileIn != null) fileIn.close();
				if (ch != null) ch.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		bytes = new byte[size];
		buffer.get(bytes);
		for(byte bt: bytes) output.append(Integer.toBinaryString(bt));
		
		return output.toString();
	}

	
	public static void decomp(String sourceFile, String resultFile) {
		// TODO: implement this method
	}
	
	public static void size(String sourceFile) {
		try {
			FileInputStream f = new FileInputStream(sourceFile);
			System.out.println("size: " + f.available());
			f.close();
		}
		catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		
	}
	
	public static boolean equal(String firstFile, String secondFile) {
		try {
			FileInputStream f1 = new FileInputStream(firstFile);
			FileInputStream f2 = new FileInputStream(secondFile);
			int k1, k2;
			byte[] buf1 = new byte[1000];
			byte[] buf2 = new byte[1000];
			do {
				k1 = f1.read(buf1);
				k2 = f2.read(buf2);
				if (k1 != k2) {
					f1.close();
					f2.close();
					return false;
				}
				for (int i=0; i<k1; i++) {
					if (buf1[i] != buf2[i]) {
						f1.close();
						f2.close();
						return false;
					}
						
				}
			} while (k1 == 0 && k2 == 0);
			f1.close();
			f2.close();
			return true;
		}
		catch (IOException ex) {
			System.out.println(ex.getMessage());
			return false;
		}
	}
	
	public static void about() {
		System.out.println("Oto Ģigulis 211RDB013 9.grupa");
		System.out.println("Jānis Aveniņš 211RDB020 9.grupa");
	    System.out.println("Līva Puķīte 211RDB036 9.grupa");
	    System.out.println("Anastasija Šarakova 211RDB093 9.grupa");
	}
}
