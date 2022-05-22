//Oto Ģigulis 211RDB013 9.grupa
//Jānis Aveniņš 211RDB020 9.grupa
//Līva Puķīte 211RDB036 9.grupa
//Anastasija Šarakova 211RDB093 9.grupa

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class FileManipulator {
	
	public static byte[] fileToBytes(String filePath) {

		File file = new File(filePath);
		FileInputStream fileIn = null;
		FileChannel ch = null;
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
				if (fileIn != null)	fileIn.close();
				if (ch != null) ch.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		bytes = new byte[size];
		buffer.get(bytes);

		return bytes;
	}

	public static byte[] binStrToBytes(String binStr) {
		try {
			return (new BigInteger(binStr, 2)).toByteArray();
		} catch (Exception e) {
			System.out.println("Binary string contains invalid values, returning null...");
			return null;
		}		
	}
	
	public static void bytesToFile(byte[] bytes, String filePath) {
		try {
			Files.write(Paths.get(filePath), bytes, StandardOpenOption.CREATE);
			System.out.println(Paths.get(filePath).toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}

class Compressor {
	
	private static FileManipulator fm = new FileManipulator();
	
	private class LZSS {
		
		final int SLIDING_WINDOW_SIZE = 4096;
		final byte START_OF_TOKEN = 2;
		final byte END_OF_TOKEN = 3;
		LZSS(){}
		
		private String decode(String input) {
			return decode(input.getBytes());
		}
				
		private String decode(byte[] input) {
			
			boolean inToken = false, scanningLength = true;
			
			ArrayList<Byte> length = new ArrayList<Byte>();
			ArrayList<Byte> offset = new ArrayList<Byte>();			
			
			ArrayList<Byte> output = new ArrayList<Byte>();
			int lengthVal, offsetVal;
			List<Byte> referencedText;
			
			for(Byte ch: input) {
				if(ch.byteValue() == START_OF_TOKEN) {
					inToken = true;
					scanningLength = true;
				}else if((char) ch.byteValue() == ',' && inToken) {
					scanningLength = false;
				}else if(ch.byteValue() == END_OF_TOKEN) {
					inToken = false;
					
					lengthVal = valFromList(length);
					offsetVal = valFromList(offset);
					
					referencedText = output.subList(output.size()-offsetVal, output.size()-offsetVal + lengthVal);
					output.addAll(referencedText);
					
					length = new ArrayList<Byte>();
					offset = new ArrayList<Byte>();
				}else if(inToken) {
					if(scanningLength) {
						length.add(ch);
					}else {
						offset.add(ch);
					}
				}else {
					output.add(ch.byteValue());
				}			
			}
			
			String fin = "";
			for(Byte b: output) fin += (char) b.byteValue();
			
			return fin;
		}
		
		private int valFromList(ArrayList<Byte> ls) {
			String sum = "";
			for(Byte bt: ls) {
				sum += (char) bt.byteValue();
			}
			return Integer.parseInt(sum);
		}
		
		private int elementsInArray(List<Byte> checkElements, List<Byte> elements) {
			int i = 0, offset = 0;
			
			for(Byte element: elements) {
				if(checkElements.size() <= offset) return i - checkElements.size();
				
				if(checkElements.get(offset) == element) offset++;
				else {
					offset = 0;
				}
				
				i++;
			}
						
			return -1;
		}
		
		private String encode(String input) {
			return encode(input.getBytes());
		}
		
		private String encode(byte[] input) {
			
			List<Byte> searchBuffer = new ArrayList<Byte>();
			ArrayList<Byte> checkedChars = new ArrayList<Byte>();
			ArrayList<Byte> checkedCharsPlusCurr = new ArrayList<Byte>();
			List<Byte> output = new ArrayList<Byte>();
			String token;
			int i = 0, index, offset, length; 
			
			
			for(Byte bt: input) {
				index = elementsInArray(checkedChars, searchBuffer);
				
				checkedCharsPlusCurr = (ArrayList<Byte>) checkedChars.clone();
				checkedCharsPlusCurr.add(bt);
				
				if(elementsInArray(checkedCharsPlusCurr, searchBuffer) == -1 || i == input.length - 1) {
					if(i == input.length - 1 && elementsInArray(checkedCharsPlusCurr, searchBuffer) != -1) {
						checkedChars.add(bt);
					}
					
					if(checkedChars.size() > 1) {
						index = elementsInArray(checkedChars, searchBuffer);
						offset = i - index - checkedChars.size();
						length = checkedChars.size();
						
						token = String.format("%c%d,%d%c",
								START_OF_TOKEN,
								length,
								offset,
								END_OF_TOKEN);
						if(token.length() > length) {
							output.addAll(checkedChars);
						}else {
							for(byte b: token.getBytes()) {
								output.add(b);
							}
						}
						searchBuffer.addAll(checkedChars);
					}else {
						output.addAll(checkedChars);
						searchBuffer.addAll(checkedChars);
					}
					
					checkedChars = new ArrayList<Byte>();
				}
				
				checkedChars.add(bt);
				
				if(searchBuffer.size() > SLIDING_WINDOW_SIZE) {
					searchBuffer = searchBuffer.subList(1, searchBuffer.size());
				}
				
				i++;
			}
			
			StringBuilder sb = new StringBuilder();
			for(Byte bt: output) {
				sb.append((char) bt.byteValue());
			}

			sb.append((char) input[input.length-1]);
			
			return sb.toString();
		}
		
 	}
	
	public void compressFile(String filePath, String archivePath) {
		LZSS lzss = this.new LZSS();
		byte[] input = fm.fileToBytes(filePath);
		
		String lzssEncodedText = lzss.encode(input);
		byte[] lzssEncodedTextAsBytes = lzssEncodedText.getBytes(); 
		
		fm.bytesToFile(lzssEncodedTextAsBytes, archivePath);
	}
	
	public void decompressFile(String archivePath, String filePath) {
		LZSS lzss = this.new LZSS();
		byte[] input = fm.fileToBytes(archivePath);
		
		String lzssDecodedText = lzss.decode(input);
		byte[] lzssDecodedTextAsBytes = lzssDecodedText.getBytes(); 
		
		fm.bytesToFile(lzssDecodedTextAsBytes, filePath);
	}
	
}

public class Main {
	
	static Compressor compressor;
	
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		compressor = new Compressor();
		String choiceStr, sourceFile, resultFile, firstFile, secondFile;
		boolean runFlag = true;
		
		
		while (runFlag) {

			System.out.println("Enter choice (comp, decomp, size, equal, about, exit): ");
			choiceStr = sc.next();

			switch (choiceStr) {
				case "comp":
					System.out.print("source file name: ");
					sourceFile = sc.next();
					System.out.print("archive name: ");
					resultFile = sc.next();
					comp(sourceFile, resultFile);
					break;
				case "decomp":
					System.out.print("archive name: ");
					sourceFile = sc.next();
					System.out.print("file name: ");
					resultFile = sc.next();
					decomp(sourceFile, resultFile);
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
					System.out.println(equals(firstFile, secondFile));
					break;
				case "about":
					about();
					break;
				case "exit":
					runFlag = false;
			}
		}

		sc.close();
	}

	public static void comp(String sourceFile, String resultFile) {
		System.out.printf("Compressing file of path '%s' to archive path '%s'%n", sourceFile, resultFile);
		compressor.compressFile(sourceFile, resultFile);
	}

	public static void decomp(String sourceFile, String resultFile) {
		System.out.printf("Decompressing archive of path '%s' to file path '%s'%n", sourceFile, resultFile);
		compressor.decompressFile(sourceFile, resultFile);
	}

	public static void size(String sourceFile) {
		try {
			FileInputStream f = new FileInputStream(sourceFile);
			System.out.println("size: " + f.available());
			f.close();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}

	}

	public static boolean equals(String firstFile, String secondFile) {
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
				for (int i = 0; i < k1; i++) {
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
		} catch (IOException ex) {
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
