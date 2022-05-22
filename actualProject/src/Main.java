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

	private class LZSS {

		final int SLIDING_WINDOW_SIZE = 4096;
		final byte START_OF_TOKEN = 2;
		final byte END_OF_TOKEN = 3;

		LZSS() {
		}

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

			for (Byte ch : input) {
				if (ch.byteValue() == START_OF_TOKEN) {
					inToken = true;

					scanningLength = true;
				} else if ((char) ch.byteValue() == ',' && inToken) {
					scanningLength = false;
				} else if (ch.byteValue() == END_OF_TOKEN) {
					inToken = false;

					lengthVal = valFromList(length);
					offsetVal = valFromList(offset);

					referencedText = output.subList(output.size() - offsetVal, output.size() - offsetVal + lengthVal);
					output.addAll(referencedText);

					length = new ArrayList<Byte>();
					offset = new ArrayList<Byte>();

				} else if (inToken) {
					if (scanningLength) {
						length.add(ch);
					} else {
						offset.add(ch);
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

						token = String.format("%c%d,%d%c", START_OF_TOKEN, length, offset, END_OF_TOKEN);
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
					byte[] lengthCache = new byte[j];
					for (int k = 0; k < j; k++) {
						lengthCache[k] = byteArray[i - j + k];
					}
					lengthStr = new String(lengthCache, StandardCharsets.UTF_8);
					length = Integer.parseInt(lengthStr);
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
					j = 0;
					while (byteArray[i] != 3) {
						i++;
						j++;
					}
					j -= 1;
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
				} else {
					llFrequencies[Byte.toUnsignedInt(byteArray[i])] += 1;
				}
			}
			return llFrequencies;
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
			StringBuilder bitSequenceStr = new StringBuilder();
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
					byte[] lengthCache = new byte[j];
					for (int k = 0; k < j; k++) {
						lengthCache[k] = byteArray[i - j + k];
					}
					lengthStr = new String(lengthCache, StandardCharsets.UTF_8);
					length = Integer.parseInt(lengthStr);
					if (length < 11) {
						bitSequenceStr.append(llPrefixCodes[254 + length]);
					} else if (length < 13) {
						bitSequenceStr.append(llPrefixCodes[265]);
						bitSequenceStr
								.append(String.format("%1s", Integer.toBinaryString(length - 11)).replace(" ", "0"));
					} else if (length < 15) {
						bitSequenceStr.append(llPrefixCodes[266]);
						bitSequenceStr
								.append(String.format("%1s", Integer.toBinaryString(length - 13)).replace(" ", "0"));
					} else if (length < 17) {
						bitSequenceStr.append(llPrefixCodes[267]);
						bitSequenceStr
								.append(String.format("%1s", Integer.toBinaryString(length - 15)).replace(" ", "0"));
					} else if (length < 19) {
						bitSequenceStr.append(llPrefixCodes[268]);
						bitSequenceStr
								.append(String.format("%1s", Integer.toBinaryString(length - 17)).replace(" ", "0"));
					} else if (length < 23) {
						bitSequenceStr.append(llPrefixCodes[269]);
						bitSequenceStr
								.append(String.format("%2s", Integer.toBinaryString(length - 19)).replace(" ", "0"));
					} else if (length < 27) {
						bitSequenceStr.append(llPrefixCodes[270]);
						bitSequenceStr
								.append(String.format("%2s", Integer.toBinaryString(length - 23)).replace(" ", "0"));
					} else if (length < 31) {
						bitSequenceStr.append(llPrefixCodes[271]);
						bitSequenceStr
								.append(String.format("%2s", Integer.toBinaryString(length - 27)).replace(" ", "0"));
					} else if (length < 35) {
						bitSequenceStr.append(llPrefixCodes[272]);
						bitSequenceStr
								.append(String.format("%2s", Integer.toBinaryString(length - 31)).replace(" ", "0"));
					} else if (length < 43) {
						bitSequenceStr.append(llPrefixCodes[273]);
						bitSequenceStr
								.append(String.format("%3s", Integer.toBinaryString(length - 35)).replace(" ", "0"));
					} else if (length < 51) {
						bitSequenceStr.append(llPrefixCodes[274]);
						bitSequenceStr
								.append(String.format("%3s", Integer.toBinaryString(length - 43)).replace(" ", "0"));
					} else if (length < 59) {
						bitSequenceStr.append(llPrefixCodes[275]);
						bitSequenceStr
								.append(String.format("%3s", Integer.toBinaryString(length - 51)).replace(" ", "0"));
					} else if (length < 67) {
						bitSequenceStr.append(llPrefixCodes[276]);
						bitSequenceStr
								.append(String.format("%3s", Integer.toBinaryString(length - 59)).replace(" ", "0"));
					} else if (length < 83) {
						bitSequenceStr.append(llPrefixCodes[277]);
						bitSequenceStr
								.append(String.format("%4s", Integer.toBinaryString(length - 67)).replace(" ", "0"));
					} else if (length < 99) {
						bitSequenceStr.append(llPrefixCodes[278]);
						bitSequenceStr
								.append(String.format("%4s", Integer.toBinaryString(length - 83)).replace(" ", "0"));
					} else if (length < 115) {
						bitSequenceStr.append(llPrefixCodes[279]);
						bitSequenceStr
								.append(String.format("%4s", Integer.toBinaryString(length - 99)).replace(" ", "0"));
					} else if (length < 131) {
						bitSequenceStr.append(llPrefixCodes[280]);
						bitSequenceStr
								.append(String.format("%4s", Integer.toBinaryString(length - 115)).replace(" ", "0"));
					} else if (length < 163) {
						bitSequenceStr.append(llPrefixCodes[281]);
						bitSequenceStr
								.append(String.format("%5s", Integer.toBinaryString(length - 131)).replace(" ", "0"));
					} else if (length < 195) {
						bitSequenceStr.append(llPrefixCodes[282]);
						bitSequenceStr
								.append(String.format("%5s", Integer.toBinaryString(length - 163)).replace(" ", "0"));
					} else if (length < 227) {
						bitSequenceStr.append(llPrefixCodes[283]);
						bitSequenceStr
								.append(String.format("%5s", Integer.toBinaryString(length - 195)).replace(" ", "0"));
					} else if (length < 258) {
						bitSequenceStr.append(llPrefixCodes[284]);
						bitSequenceStr
								.append(String.format("%5s", Integer.toBinaryString(length - 227)).replace(" ", "0"));
					} else if (length == 258) {
						bitSequenceStr.append(distancePrefixCodes[285]);
					}
					j = 0;
					while (byteArray[i] != 3) {
						i++;
						j++;
					}
					j -= 1;
					byte[] distanceCache = new byte[j];
					for (int k = 0; k < j; k++) {
						distanceCache[k] = byteArray[i - j + k];
					}
					distanceStr = new String(distanceCache, StandardCharsets.UTF_8);
					distance = Integer.parseInt(distanceStr);
					if (distance < 5) {
						bitSequenceStr.append(distancePrefixCodes[distance - 1]);
					} else if (distance < 7) {
						bitSequenceStr.append(distancePrefixCodes[4]);
						bitSequenceStr
								.append(String.format("%1s", Integer.toBinaryString(distance - 5)).replace(" ", "0"));
					} else if (distance < 9) {
						bitSequenceStr.append(distancePrefixCodes[5]);
						bitSequenceStr
								.append(String.format("%1s", Integer.toBinaryString(distance - 7)).replace(" ", "0"));
					} else if (distance < 13) {
						bitSequenceStr.append(distancePrefixCodes[6]);
						bitSequenceStr
								.append(String.format("%2s", Integer.toBinaryString(distance - 9)).replace(" ", "0"));
					} else if (distance < 17) {
						bitSequenceStr.append(distancePrefixCodes[7]);
						bitSequenceStr
								.append(String.format("%2s", Integer.toBinaryString(distance - 13)).replace(" ", "0"));
					} else if (distance < 25) {
						bitSequenceStr.append(distancePrefixCodes[8]);
						bitSequenceStr
								.append(String.format("%3s", Integer.toBinaryString(distance - 17)).replace(" ", "0"));
					} else if (distance < 33) {
						bitSequenceStr.append(distancePrefixCodes[9]);
						bitSequenceStr
								.append(String.format("%3s", Integer.toBinaryString(distance - 25)).replace(" ", "0"));
					} else if (distance < 49) {
						bitSequenceStr.append(distancePrefixCodes[10]);
						bitSequenceStr
								.append(String.format("%4s", Integer.toBinaryString(distance - 33)).replace(" ", "0"));
					} else if (distance < 65) {
						bitSequenceStr.append(distancePrefixCodes[11]);
						bitSequenceStr
								.append(String.format("%4s", Integer.toBinaryString(distance - 49)).replace(" ", "0"));
					} else if (distance < 97) {
						bitSequenceStr.append(distancePrefixCodes[12]);
						bitSequenceStr
								.append(String.format("%5s", Integer.toBinaryString(distance - 65)).replace(" ", "0"));
					} else if (distance < 129) {
						bitSequenceStr.append(distancePrefixCodes[13]);
						bitSequenceStr
								.append(String.format("%5s", Integer.toBinaryString(distance - 97)).replace(" ", "0"));
					} else if (distance < 193) {
						bitSequenceStr.append(distancePrefixCodes[14]);
						bitSequenceStr
								.append(String.format("%6s", Integer.toBinaryString(distance - 129)).replace(" ", "0"));
					} else if (distance < 257) {
						bitSequenceStr.append(distancePrefixCodes[15]);
						bitSequenceStr
								.append(String.format("%6s", Integer.toBinaryString(distance - 193)).replace(" ", "0"));
					} else if (distance < 385) {
						bitSequenceStr.append(distancePrefixCodes[16]);
						bitSequenceStr
								.append(String.format("%7s", Integer.toBinaryString(distance - 257)).replace(" ", "0"));
					} else if (distance < 513) {
						bitSequenceStr.append(distancePrefixCodes[17]);
						bitSequenceStr
								.append(String.format("%7s", Integer.toBinaryString(distance - 385)).replace(" ", "0"));
					} else if (distance < 769) {
						bitSequenceStr.append(distancePrefixCodes[18]);
						bitSequenceStr
								.append(String.format("%8s", Integer.toBinaryString(distance - 513)).replace(" ", "0"));
					} else if (distance < 1025) {
						bitSequenceStr.append(distancePrefixCodes[19]);
						bitSequenceStr
								.append(String.format("%8s", Integer.toBinaryString(distance - 769)).replace(" ", "0"));
					} else if (distance < 1537) {
						bitSequenceStr.append(distancePrefixCodes[20]);
						bitSequenceStr.append(
								String.format("%9s", Integer.toBinaryString(distance - 1025)).replace(" ", "0"));
					} else if (distance < 2049) {
						bitSequenceStr.append(distancePrefixCodes[21]);
						bitSequenceStr.append(
								String.format("%9s", Integer.toBinaryString(distance - 1537)).replace(" ", "0"));
					} else if (distance < 3073) {
						bitSequenceStr.append(distancePrefixCodes[22]);
						bitSequenceStr.append(
								String.format("%10s", Integer.toBinaryString(distance - 2049)).replace(" ", "0"));
					} else if (distance < 4097) {
						bitSequenceStr.append(distancePrefixCodes[23]);
						bitSequenceStr.append(
								String.format("%10s", Integer.toBinaryString(distance - 3073)).replace(" ", "0"));
					} else if (distance < 6145) {
						bitSequenceStr.append(distancePrefixCodes[24]);
						bitSequenceStr.append(
								String.format("%11s", Integer.toBinaryString(distance - 4097)).replace(" ", "0"));
					} else if (distance < 8193) {
						bitSequenceStr.append(distancePrefixCodes[25]);
						bitSequenceStr.append(
								String.format("%11s", Integer.toBinaryString(distance - 6145)).replace(" ", "0"));
					} else if (distance < 12289) {
						bitSequenceStr.append(distancePrefixCodes[26]);
						bitSequenceStr.append(
								String.format("%12s", Integer.toBinaryString(distance - 8193)).replace(" ", "0"));
					} else if (distance < 16385) {
						bitSequenceStr.append(distancePrefixCodes[27]);
						bitSequenceStr.append(
								String.format("%12s", Integer.toBinaryString(distance - 12289)).replace(" ", "0"));
					} else if (distance < 24577) {
						bitSequenceStr.append(distancePrefixCodes[28]);
						bitSequenceStr.append(
								String.format("%13s", Integer.toBinaryString(distance - 16385)).replace(" ", "0"));
					} else if (distance >= 24577) {
						bitSequenceStr.append(distancePrefixCodes[29]);
						bitSequenceStr.append(
								String.format("%13s", Integer.toBinaryString(distance - 24577)).replace(" ", "0"));
					}
				} else {
					bitSequenceStr.append(llPrefixCodes[Byte.toUnsignedInt(byteArray[i])]);
				}
			}
			// System.out.print(bitSequenceStr);
			return bitSequenceStr.toString();
		}

	}

	public String compressFile(String filePath, String archivePath) {
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

	public static void main(String[] args) {
		compressor = new Compressor();
		compressor.compressFile("test222.txt", "archive1");
		String lzss = compressor.compressFile("test222.txt", "archive1");
		// System.out.println(lzss);

		String fileName = "archive1";
		byte[] byteArray = FileManipulator.fileToBytes(fileName);
		// System.out.println(byteArray.length);
		for (int i = 0; i < byteArray.length; i++) {
			// System.out.println(" " + Byte.toUnsignedInt(byteArray[i]));
		}
		// System.out.println();
		int frequencies[] = Compressor.Huffman.frequencyCounter(byteArray);
		// System.out.println("frequency");
		for (int i = 0; i < frequencies.length; i++) {
			// System.out.println((char) i + " " + frequencies[i]);
		}

		// System.out.println("Jānis lika");
		Compressor.Huffman.bitStreamMaker(byteArray);
		String chuska;
		chuska = Compressor.Huffman.bitStreamMaker(byteArray);
		System.out.println(chuska.length()/8);
		System.out.println(chuska.substring(37500));
		FileManipulator.bytesToFile(
				FileManipulator.binStrToBytes(chuska),
				"archive200"
			);
		

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
