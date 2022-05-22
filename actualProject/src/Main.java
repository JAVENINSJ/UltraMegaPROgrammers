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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
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
				if (fileIn != null)
					fileIn.close();
				if (ch != null)
					ch.close();
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

	public class LZSS {

		final int SLIDING_WINDOW_SIZE = 4096;
		final byte START_OF_TOKEN = 2;
		final byte END_OF_TOKEN = 3;

		private LZSS() {
		}

		protected String decode(byte[] input) {

			boolean inToken = false, scanningOffset = true;

			ArrayList<Byte> length = new ArrayList<Byte>();
			ArrayList<Byte> offset = new ArrayList<Byte>();

			ArrayList<Byte> output = new ArrayList<Byte>();
			int lengthVal, offsetVal;
			List<Byte> referencedText;

			for (Byte ch : input) {
				if (ch.byteValue() == START_OF_TOKEN) {
					inToken = true;
					scanningOffset = true;
				} else if ((char) ch.byteValue() == ',' && inToken) {
					scanningOffset = false;
				} else if (ch.byteValue() == END_OF_TOKEN) {
					inToken = false;

					lengthVal = valFromList(length);
					offsetVal = valFromList(offset);

					referencedText = output.subList(output.size() - offsetVal, output.size() - offsetVal + lengthVal);
					output.addAll(referencedText);

					length = new ArrayList<Byte>();
					offset = new ArrayList<Byte>();
				} else if (inToken) {
					if (scanningOffset) {
						offset.add(ch);
					} else {
						length.add(ch);
					}
				} else {
					output.add(ch.byteValue());
				}
			}

			String fin = "";
			for (Byte b : output)
				fin += (char) b.byteValue();

			return fin;
		}

		private int valFromList(ArrayList<Byte> ls) {
			String sum = "";
			for (Byte bt : ls) {
				sum += (char) bt.byteValue();
			}
			return Integer.parseInt(sum);
		}

		private int elementsInArray(List<Byte> checkElements, List<Byte> elements) {
			int i = 0, offset = 0;

			for (Byte element : elements) {
				if (checkElements.size() <= offset)
					return i - checkElements.size();

				if (checkElements.get(offset) == element)
					offset++;
				else {
					offset = 0;
				}

				i++;
			}

			return -1;
		}

		private String encode(byte[] input) {

			List<Byte> searchBuffer = new ArrayList<Byte>();
			ArrayList<Byte> checkedChars = new ArrayList<Byte>();
			ArrayList<Byte> checkedCharsPlusCurr = new ArrayList<Byte>();
			List<Byte> output = new ArrayList<Byte>();
			String token;
			int i = 0, index, offset, length;

			for (Byte bt : input) {
				index = elementsInArray(checkedChars, searchBuffer);

				checkedCharsPlusCurr = (ArrayList<Byte>) checkedChars.clone();
				checkedCharsPlusCurr.add(bt);

				if (elementsInArray(checkedCharsPlusCurr, searchBuffer) == -1 || i == input.length - 1) {
					if (i == input.length - 1 && elementsInArray(checkedCharsPlusCurr, searchBuffer) != -1) {
						checkedChars.add(bt);
					}

					if (checkedChars.size() > 1) {
						index = elementsInArray(checkedChars, searchBuffer);
						offset = i - index - checkedChars.size();
						length = checkedChars.size();
						token = String.format("%c%d,%d%c", START_OF_TOKEN, offset, length, END_OF_TOKEN);

						if (token.length() > length) {
							output.addAll(checkedChars);
						} else {
							for (byte b : token.getBytes()) {
								output.add(b);
							}
						}
						searchBuffer.addAll(checkedChars);
					} else {
						output.addAll(checkedChars);
						searchBuffer.addAll(checkedChars);
					}

					checkedChars = new ArrayList<Byte>();
				}

				checkedChars.add(bt);

				if (searchBuffer.size() > SLIDING_WINDOW_SIZE) {
					searchBuffer = searchBuffer.subList(1, searchBuffer.size());
				}

				i++;
			}

			StringBuilder sb = new StringBuilder();
			for (Byte bt : output) {
				sb.append((char) bt.byteValue());
			}

			return sb.toString();
		}

	}

	public class Huffman {
		static int[] frequencyCounter(byte[] byteArray) {
			int[] llFrequencies = new int[286]; // 0-285 simboli hz vai vajag EOB
			int[] distFrequencies = new int[30]; // 0-29 hz kāpēc saka ka 0-31
			int distance, length, j = 0;
			String distanceStr, lengthStr;
			for (int i = 0; i < byteArray.length; i++) {
				if (byteArray[i] == 2) { // 2 sākums 44 komats 3 beigas
					System.out.println(i);
					j = 0;
					while (byteArray[i] != 44) {
						i++;
						j++;
					}
					j -= 1;
					System.out.println(i);
					System.out.println(j);
					byte[] distanceCache = new byte[j];
					for (int k = 0; k < j; k++) {
						distanceCache[k] = byteArray[i - j + k];
					}
					distanceStr = new String(distanceCache, StandardCharsets.UTF_8);
					distance = Integer.parseInt(distanceStr);
					if (distance < 5) {
						distFrequencies[distance - 1] += 1;
					} else if (distance < 7) {
						distFrequencies[4] += 1;
					} else if (distance < 9) {
						distFrequencies[5] += 1;
					} else if (distance < 13) {
						distFrequencies[6] += 1;
					} else if (distance < 17) {
						distFrequencies[7] += 1;
					} else if (distance < 25) {
						distFrequencies[8] += 1;
					} else if (distance < 33) {
						distFrequencies[9] += 1;
					} else if (distance < 49) {
						distFrequencies[10] += 1;
					} else if (distance < 65) {
						distFrequencies[11] += 1;
					} else if (distance < 97) {
						distFrequencies[12] += 1;
					} else if (distance < 129) {
						distFrequencies[13] += 1;
					} else if (distance < 193) {
						distFrequencies[14] += 1;
					} else if (distance < 257) {
						distFrequencies[15] += 1;
					} else if (distance < 385) {
						distFrequencies[16] += 1;
					} else if (distance < 513) {
						distFrequencies[17] += 1;
					} else if (distance < 769) {
						distFrequencies[18] += 1;
					} else if (distance < 1025) {
						distFrequencies[19] += 1;
					} else if (distance < 1537) {
						distFrequencies[20] += 1;
					} else if (distance < 2049) {
						distFrequencies[21] += 1;
					} else if (distance < 3073) {
						distFrequencies[22] += 1;
					} else if (distance < 4097) {
						distFrequencies[23] += 1;
					} else if (distance < 6145) {
						distFrequencies[24] += 1;
					} else if (distance < 8193) {
						distFrequencies[25] += 1;
					} else if (distance < 12289) {
						distFrequencies[26] += 1;
					} else if (distance < 16385) {
						distFrequencies[27] += 1;
					} else if (distance < 24577) {
						distFrequencies[28] += 1;
					} else if (distance >= 24577) {
						distFrequencies[29] += 1;
					}
					j = 0;
					while (byteArray[i] != 3) {
						i++;
						j++;
					}
					j -= 1;
					byte[] lengthCache = new byte[j];
					for (int k = 0; k < j; k++) {
						lengthCache[k] = byteArray[i - j + k];
					}
					lengthStr = new String(lengthCache, StandardCharsets.UTF_8);
					length = Integer.parseInt(lengthStr);
					System.out.println("Length " + length);
					if (length < 11) {
						llFrequencies[255 + length - 1] += 1;
					} else if (length < 13) {
						llFrequencies[265] += 1;
					} else if (length < 15) {
						llFrequencies[266] += 1;
					} else if (length < 17) {
						llFrequencies[267] += 1;
					} else if (length < 19) {
						llFrequencies[268] += 1;
					} else if (length < 23) {
						llFrequencies[269] += 1;
					} else if (length < 27) {
						llFrequencies[270] += 1;
					} else if (length < 31) {
						llFrequencies[271] += 1;
					} else if (length < 35) {
						llFrequencies[272] += 1;
					} else if (length < 43) {
						llFrequencies[273] += 1;
					} else if (length < 51) {
						llFrequencies[274] += 1;
					} else if (length < 59) {
						llFrequencies[275] += 1;
					} else if (length < 67) {
						llFrequencies[276] += 1;
					} else if (length < 83) {
						llFrequencies[277] += 1;
					} else if (length < 99) {
						llFrequencies[278] += 1;
					} else if (length < 115) {
						llFrequencies[279] += 1;
					} else if (length < 131) {
						llFrequencies[280] += 1;
					} else if (length < 163) {
						llFrequencies[281] += 1;
					} else if (length < 195) {
						llFrequencies[282] += 1;
					} else if (length < 227) {
						llFrequencies[283] += 1;
					} else if (length < 258) {
						llFrequencies[284] += 1;
					} else if (length == 258) {
						llFrequencies[285] += 1;
					}
					i++;
				} else {
					llFrequencies[Byte.toUnsignedInt(byteArray[i])] += 1;
				}
			}
			return llFrequencies; // kā atgriezt abus masīvus?
		}

		static String bitStreamMaker(byte[] byteArray) {
			String[] llPrefixCodes = new String[288];
			String[] distancePrefixCodes = new String[32];
			for (int i = 48; i <= 191; i++) {
				llPrefixCodes[i - 48] = String.format("%8s", Integer.toBinaryString(i)).replace(" ", "0");
			}
			for (int i = 400; i <= 511; i++) {
				llPrefixCodes[i - 256] = String.format("%9s", Integer.toBinaryString(i)).replace(" ", "0");
			}
			for (int i = 0; i <= 23; i++) {
				llPrefixCodes[i + 256] = String.format("%7s", Integer.toBinaryString(i)).replace(" ", "0");
			}
			for (int i = 384; i <= 391; i++) {
				llPrefixCodes[i - 104] = String.format("%9s", Integer.toBinaryString(i)).replace(" ", "0");
			}
			for (int i = 0; i <= 31; i++) {
				distancePrefixCodes[i] = String.format("%5s", Integer.toBinaryString(i)).replace(" ", "0");
			}
			for (int i = 0; i < llPrefixCodes.length; i++) {
				System.out.println(llPrefixCodes[i]);
			}
			for (int i = 0; i < distancePrefixCodes.length; i++) {
				System.out.println(distancePrefixCodes[i]);
			}
			String bitSequence = "";
			int[] llFrequencies = new int[286];
			int[] distFrequencies = new int[30];
			int distance, length, j = 0;
			String distanceStr, lengthStr;
			for (int i = 0; i < byteArray.length; i++) {
				if (byteArray[i] == 2) {
					j = 0;
					while (byteArray[i] != 44) {
						i++;
						j++;
					}
					j -= 1;
					System.out.println(i);
					System.out.println(j);
					byte[] distanceCache = new byte[j];
					for (int k = 0; k < j; k++) {
						distanceCache[k] = byteArray[i - j + k];
					}
					distanceStr = new String(distanceCache, StandardCharsets.UTF_8);
					distance = Integer.parseInt(distanceStr);
					if (distance < 5) {
						distFrequencies[distance - 1] += 1;
					} else if (distance < 7) {
						distFrequencies[4] += 1;
					} else if (distance < 9) {
						distFrequencies[5] += 1;
					} else if (distance < 13) {
						distFrequencies[6] += 1;
					} else if (distance < 17) {
						distFrequencies[7] += 1;
					} else if (distance < 25) {
						distFrequencies[8] += 1;
					} else if (distance < 33) {
						distFrequencies[9] += 1;
					} else if (distance < 49) {
						distFrequencies[10] += 1;
					} else if (distance < 65) {
						distFrequencies[11] += 1;
					} else if (distance < 97) {
						distFrequencies[12] += 1;
					} else if (distance < 129) {
						distFrequencies[13] += 1;
					} else if (distance < 193) {
						distFrequencies[14] += 1;
					} else if (distance < 257) {
						distFrequencies[15] += 1;
					} else if (distance < 385) {
						distFrequencies[16] += 1;
					} else if (distance < 513) {
						distFrequencies[17] += 1;
					} else if (distance < 769) {
						distFrequencies[18] += 1;
					} else if (distance < 1025) {
						distFrequencies[19] += 1;
					} else if (distance < 1537) {
						distFrequencies[20] += 1;
					} else if (distance < 2049) {
						distFrequencies[21] += 1;
					} else if (distance < 3073) {
						distFrequencies[22] += 1;
					} else if (distance < 4097) {
						distFrequencies[23] += 1;
					} else if (distance < 6145) {
						distFrequencies[24] += 1;
					} else if (distance < 8193) {
						distFrequencies[25] += 1;
					} else if (distance < 12289) {
						distFrequencies[26] += 1;
					} else if (distance < 16385) {
						distFrequencies[27] += 1;
					} else if (distance < 24577) {
						distFrequencies[28] += 1;
					} else if (distance >= 24577) {
						distFrequencies[29] += 1;
					}
					j = 0;
					while (byteArray[i] != 3) {
						i++;
						j++;
					}
					j -= 1;
					byte[] lengthCache = new byte[j];
					for (int k = 0; k < j; k++) {
						lengthCache[k] = byteArray[i - j + k];
					}
					lengthStr = new String(lengthCache, StandardCharsets.UTF_8);
					length = Integer.parseInt(lengthStr);
					System.out.println("Length " + length);
					if (length < 11) {
						llFrequencies[255 + length - 1] += 1;
					} else if (length < 13) {
						llFrequencies[265] += 1;
					} else if (length < 15) {
						llFrequencies[266] += 1;
					} else if (length < 17) {
						llFrequencies[267] += 1;
					} else if (length < 19) {
						llFrequencies[268] += 1;
					} else if (length < 23) {
						llFrequencies[269] += 1;
					} else if (length < 27) {
						llFrequencies[270] += 1;
					} else if (length < 31) {
						llFrequencies[271] += 1;
					} else if (length < 35) {
						llFrequencies[272] += 1;
					} else if (length < 43) {
						llFrequencies[273] += 1;
					} else if (length < 51) {
						llFrequencies[274] += 1;
					} else if (length < 59) {
						llFrequencies[275] += 1;
					} else if (length < 67) {
						llFrequencies[276] += 1;
					} else if (length < 83) {
						llFrequencies[277] += 1;
					} else if (length < 99) {
						llFrequencies[278] += 1;
					} else if (length < 115) {
						llFrequencies[279] += 1;
					} else if (length < 131) {
						llFrequencies[280] += 1;
					} else if (length < 163) {
						llFrequencies[281] += 1;
					} else if (length < 195) {
						llFrequencies[282] += 1;
					} else if (length < 227) {
						llFrequencies[283] += 1;
					} else if (length < 258) {
						llFrequencies[284] += 1;
					} else if (length == 258) {
						llFrequencies[285] += 1;
					}
					i++;

				} else {
					bitSequence += llPrefixCodes[Byte.toUnsignedInt(byteArray[i])];
				}
			}
			return bitSequence;
		}

	}

	public String compressFile(String filePath, String archivePath) { // void
		LZSS lzss = this.new LZSS();
		byte[] input = fm.fileToBytes(filePath);

		String lzssEncodedText = lzss.encode(input);
		byte[] lzssEncodedTextAsBytes = lzssEncodedText.getBytes();

		fm.bytesToFile(lzssEncodedTextAsBytes, archivePath);
		return lzssEncodedText;
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

	// programmēšana mani padara agresīvu
	public static void main(String[] args) {
		compressor = new Compressor();
		compressor.compressFile("test222.txt", "archive1");
		String lzss = compressor.compressFile("test222.txt", "archive1");
		System.out.println(lzss);

		String fileName = "archive1";
		byte[] byteArray = FileManipulator.fileToBytes(fileName);
		System.out.println(byteArray.length);
		for (int i = 0; i < byteArray.length; i++) {
			System.out.println(" " + Byte.toUnsignedInt(byteArray[i])); // Byte.toUnsignedInt
		}
		System.out.println();
		int frequencies[] = Compressor.Huffman.frequencyCounter(byteArray);
		System.out.println("frequency");
		for (int i = 0; i < frequencies.length; i++) {
			System.out.println((char) i + " " + frequencies[i]);
		}

		String[] llPrefixCodes = new String[288];
		String[] distancePrefixCodes = new String[32];
		for (int i = 48; i <= 191; i++) {
			llPrefixCodes[i - 48] = String.format("%8s", Integer.toBinaryString(i)).replace(" ", "0");
		}
		for (int i = 400; i <= 511; i++) {
			llPrefixCodes[i - 256] = String.format("%9s", Integer.toBinaryString(i)).replace(" ", "0");
		}
		for (int i = 0; i <= 23; i++) {
			llPrefixCodes[i + 256] = String.format("%7s", Integer.toBinaryString(i)).replace(" ", "0");
		}
		for (int i = 384; i <= 391; i++) {
			llPrefixCodes[i - 104] = String.format("%9s", Integer.toBinaryString(i)).replace(" ", "0");
		}
		for (int i = 0; i <= 31; i++) {
			distancePrefixCodes[i] = String.format("%5s", Integer.toBinaryString(i)).replace(" ", "0");
		}
		for (int i = 0; i < llPrefixCodes.length; i++) {
			System.out.println(llPrefixCodes[i]);
		}
		for (int i = 0; i < distancePrefixCodes.length; i++) {
			System.out.println(distancePrefixCodes[i]);
		}

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
