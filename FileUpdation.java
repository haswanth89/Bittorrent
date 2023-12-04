
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

// public class FileUpdation {
//     public static boolean check(String peerId) throws IOException {
//         File file = new File("./input_files" + peerId + "/thefile");
//         return file.exists();
//     }

//     public static byte[] returnChunks(byte[] original, int low, int high) {
//         byte[] result = new byte[high - low];
//         System.arraycopy(original, low, result, 0, Math.min(original.length - low, high - low));
//         return result;
//     }

//     public static HashMap<Integer, byte[]> getDataInChunks(int fileSize, int chunkSize, String fileName)
//             throws Exception {
//         // byte[][] fileChunks = new byte[(int) Math.ceil(fileSize /
//         // chunkSize)][chunkSize];
//         // fileChunks = new byte[(int) Math.ceil(fileSize / chunkSize) + 1][];
//         // System.out.println(fileChunks == null);
//         HashMap<Integer, byte[]> fileData = new HashMap<Integer, byte[]>();
//         BufferedInputStream file = new BufferedInputStream(
//                 new FileInputStream("./input_files/" + ParentThread.peerID + "/" + fileName));
//         byte[] byteArray = new byte[fileSize];

//         file.read(byteArray);
//         file.close();
//         int chunkIndex = 0, cnt = 0;

//         while (chunkIndex < fileSize) {

//             if (chunkIndex + chunkSize <= fileSize) {
//                 fileData.put(cnt, returnChunks(byteArray, chunkIndex, chunkIndex + chunkSize));
//                 cnt++;
//             } else {
//                 fileData.put(cnt, returnChunks(byteArray, chunkIndex, fileSize));
//                 cnt++;
//             }
//             chunkIndex += chunkSize;

//         }

//         return fileData;

//     }

//     public static void createDirectories(int peerID, String file) throws IOException {

//         Path p = Paths.get("./input_files/" + String.valueOf(peerID));
//         System.out.println(p.toString());
//         if (Files.exists(p)) {
//             clean(p, file);
//         } else {
//             Files.createDirectory(p);

//         }
//         System.out.println("Here");
//         new File("./input_files/" + String.valueOf(peerID) + "/logs_" + String.valueOf(peerID) + ".log");
//     }

//     public static void clean(Path path, String file) throws IOException {

//         Stream<Path> filesList = Files.list(path);

//         for (Object o : filesList.toArray()) {

//             Path current_file = (Path) o;
//             if (!current_file.getFileName().toString().equals(file)) {
//                 Files.delete(current_file);
//             }

//         }
//         filesList.close();

//     }

//     public static HashMap<Integer, byte[]> sortFileData(HashMap<Integer, byte[]> map) throws Exception {
//         List<Map.Entry<Integer, byte[]>> list = new LinkedList<Map.Entry<Integer, byte[]>>(map.entrySet());

//         // Sort the list
//         Collections.sort(list, new Comparator<Map.Entry<Integer, byte[]>>() {
//             public int compare(Map.Entry<Integer, byte[]> o1,
//                     Map.Entry<Integer, byte[]> o2) {
//                 return (o1.getKey()).compareTo(o2.getKey());
//             }
//         });

//         // put data from sorted list to hashmap
//         HashMap<Integer, byte[]> temp = new LinkedHashMap<Integer, byte[]>();
//         for (Map.Entry<Integer, byte[]> sorted : list) {
//             temp.put(sorted.getKey(), sorted.getValue());
//         }
//         return temp;

//     }

// }

public class FileUpdation {
    public static boolean checkFileExists(String peerId) throws IOException {
        File file = new File("./input_files" + peerId + "/thefile");
        return file.exists();
    }

    public static byte[] extractChunk(byte[] original, int start, int end) {
        byte[] chunk = new byte[end - start];
        System.arraycopy(original, start, chunk, 0, Math.min(original.length - start,
                end - start));
        return chunk;
    }

    public static HashMap<Integer, byte[]> chunkFileData(int fileSize, int chunkSize, String fileName)
            throws Exception {
        HashMap<Integer, byte[]> chunkedData = new HashMap<>();
        BufferedInputStream inputStream = new BufferedInputStream(
                new FileInputStream("./input_files/" + ParentThread.peerID + "/" +
                        fileName));
        byte[] fileBuffer = new byte[fileSize];

        inputStream.read(fileBuffer);
        inputStream.close();
        int currentChunkIndex = 0, chunkCounter = 0;

        while (currentChunkIndex < fileSize) {
            if (currentChunkIndex + chunkSize <= fileSize) {
                chunkedData.put(chunkCounter,
                        extractChunk(fileBuffer, currentChunkIndex, currentChunkIndex + chunkSize));
                chunkCounter++;
            } else {
                chunkedData.put(chunkCounter, extractChunk(fileBuffer, currentChunkIndex,
                        fileSize));
                chunkCounter++;
            }
            currentChunkIndex += chunkSize;
        }

        return chunkedData;
    }

    public static void initializeDirectories(int peerID, String fileName) throws IOException {
        Path directoryPath = Paths.get("./input_files/" + peerID);
        System.out.println(directoryPath.toString());
        if (Files.exists(directoryPath)) {
            cleanDirectory(directoryPath, fileName);
        } else {
            Files.createDirectory(directoryPath);
        }
        System.out.println("Directory initialized");
        new File("./input_files/" + peerID + "/logs_" + peerID + ".log");
    }

    public static void cleanDirectory(Path directory, String fileName) throws IOException {
        Stream<Path> filesStream = Files.list(directory);

        for (Object pathObject : filesStream.toArray()) {
            Path currentPath = (Path) pathObject;
            if (!currentPath.getFileName().toString().equals(fileName)) {
                Files.delete(currentPath);
            }
        }
        filesStream.close();
    }

    public static HashMap<Integer, byte[]> sortChunkedData(HashMap<Integer, byte[]> dataMap) throws Exception {
        List<Map.Entry<Integer, byte[]>> entryList = new LinkedList<>(dataMap.entrySet());

        // Sort the list
        Collections.sort(entryList, new Comparator<Map.Entry<Integer, byte[]>>() {
            public int compare(Map.Entry<Integer, byte[]> firstEntry,
                    Map.Entry<Integer, byte[]> secondEntry) {
                return firstEntry.getKey().compareTo(secondEntry.getKey());
            }
        });

        // Put data from sorted list to hashmap
        HashMap<Integer, byte[]> sortedData = new LinkedHashMap<>();
        for (Map.Entry<Integer, byte[]> sortedEntry : entryList) {
            sortedData.put(sortedEntry.getKey(), sortedEntry.getValue());
        }
        return sortedData;
    }
}
