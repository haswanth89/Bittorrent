package input_processing;

import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class GeneralConfig {

    public static int noOfPreferredNeighbours;
    public static int unchokingRange;
    public static int optimisticUnchokingRange;
    public static String nameOfFile;
    public static int sizeOfFile;
    public static int sizeOfPiece;
    public static int overallPieces;

    public static void readFile(String filePath) throws IOException {
        ArrayList<String> readLines = GeneralConfig.readLinesFromFile(filePath);
        noOfPreferredNeighbours = Integer.parseInt(readLines.get(0).split(" ")[1]);
        unchokingRange = Integer.parseInt(readLines.get(1).split(" ")[1]);
        optimisticUnchokingRange = Integer.parseInt(readLines.get(2).split(" ")[1]);
        nameOfFile = readLines.get(3).split(" ")[1];
        sizeOfFile = Integer.parseInt(readLines.get(4).split(" ")[1]);
        sizeOfPiece = Integer.parseInt(readLines.get(5).split(" ")[1]);
        overallPieces = (int) Math.ceil((double) sizeOfFile / sizeOfPiece);
    }

    public static int byteArrayToInt(byte[] byteArray) {
        return ByteBuffer.wrap(byteArray).getInt();
    }

    public static byte[] intToByteArray(int intValue) {
        return ByteBuffer.allocate(4).putInt(intValue).array();
    }

    public static void insertIntIntoByteArray(byte[] byteArray, int intValue, int startIndex) {
        byte[] convertedArray = GeneralConfig.intToByteArray(intValue);
        for (int i = 0; i < 4; i++) {
            byteArray[startIndex + i] = convertedArray[i];
        }
    }

    public static String bytesToString(byte[] byteArray, int start, int end) {
        int length = end - start + 1;
        if (length <= 0 || end >= byteArray.length) {
            return "";
        }

        byte[] stringBytes = new byte[length];
        System.arraycopy(byteArray, start, stringBytes, 0, length);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    public static ArrayList<String> readLinesFromFile(String filePath) throws IOException {
        ArrayList<String> fileLines = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
        String currentLine = bufferedReader.readLine();
        while (currentLine != null) {
            fileLines.add(currentLine);
            currentLine = bufferedReader.readLine();
        }
        bufferedReader.close();
        return fileLines;
    }

}
