import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import input_processing.GeneralConfig;
import message_files.Content;
import message_files.ContentManager;

public class Peer {

    // -------- Common Resources Shared Among Peers ----------------//
    // number of peers that have the full file, including this peer
    static int numberOfCompleteDownloads = 0;
    // list of ids of currently interested peers
    static HashSet<Integer> interestedNeighbors = new HashSet<>();
    static HashSet<Integer> preferredNeighbors = new HashSet<>();
    // id of current optimally unchoked peer
    static int optimallyUnchokedPeer;
    // download speeds for neighbors with given id , in last unchoking interval
    static HashMap<Integer, Double> lastIntervalDownloadSpeeds = new HashMap<>();
    static HashMap<Integer, BitSet> bitFieldMap = new HashMap<>();
    static HashMap<Integer, Boolean> chokingMap = new HashMap<>();
    static HashMap<Integer, Boolean> fileWritten = new HashMap<>();
    static BitSet requestedPieces;
    // static boolean fileWritten;

    // ---------- Local Members ------------//

    int peerID;
    String hostName;
    int port;
    int hasFile;
    Socket socket;
    boolean hasItBeenChoked;
    static HashMap<Integer, byte[]> fileData;

    public Peer(int peerID, String hostName, int port, int hasFile) throws Exception {
        this.peerID = peerID;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
        bitFieldMap.put(peerID, new BitSet(GeneralConfig.overallPieces));
        if (peerID != ParentThread.peerID) {
            chokingMap.put(peerID, true);
            lastIntervalDownloadSpeeds.put(peerID, 0.0);
        }

        if (this.hasFile == 1 && (peerID == ParentThread.peerID)) {
            bitFieldMap.get(peerID).set(0, GeneralConfig.overallPieces);
            numberOfCompleteDownloads = 1;
            fileData = FileUpdation.chunkFileData(GeneralConfig.sizeOfFile, GeneralConfig.sizeOfPiece,
                    GeneralConfig.nameOfFile);
            fileWritten.put(ParentThread.peerID, true);
        } else if (this.hasFile != 1 && (peerID == ParentThread.peerID)) {
            fileData = new HashMap<Integer, byte[]>();
            fileWritten.put(ParentThread.peerID, false);

        }

    }

    public void startMessageExchange(Socket socket) {
        this.socket = socket;

        new Thread(new MessageExchange(socket)).start();
    }

    public void setBitFields(int peerID, byte[] bytes) {
        bitFieldMap.put(peerID, BitSet.valueOf(bytes));
    }

    public void setBitField(int peerID, int pieceIndex) {
        bitFieldMap.get(peerID).set(pieceIndex);
    }

    class MessageExchange implements Runnable {
        Socket socket;

        public MessageExchange(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            synchronized (this) {
                try {

                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                    // send bitfield message if current peer has any pieces
                    if (!bitFieldMap.get(ParentThread.peerID).isEmpty()) {
                        outputStream.write(ContentManager.constructMessage(Content.BITFIELD,
                                bitFieldMap.get(ParentThread.peerID).toByteArray(), -1));
                        outputStream.flush();
                    }

                    while (numberOfCompleteDownloads < ParentThread.peers.size()) {

                        int messageSize = 0;
                        try {
                            messageSize = inputStream.readInt();

                        } catch (EOFException e) {

                            continue;
                        }

                        byte[] messageArray = new byte[messageSize];
                        double start = System.currentTimeMillis();
                        inputStream.read(messageArray);
                        double time = System.currentTimeMillis() - start;

                        Content message = ContentManager.parseMessage(messageArray);
                        lastIntervalDownloadSpeeds.put(peerID, messageSize / time);
                        lastIntervalDownloadSpeeds = sortDownloadSpeeds(lastIntervalDownloadSpeeds);
                        switch (message.typeID) {
                            case Content.BITFIELD:
                                bitFieldMap.put(peerID, BitSet.valueOf(message.payload));

                                // find pieces neighbour has but peer does not

                                BitSet piecesRequired = (BitSet) bitFieldMap.get(ParentThread.peerID).clone();
                                piecesRequired.xor(bitFieldMap.get(peerID));
                                piecesRequired.andNot(bitFieldMap.get(ParentThread.peerID));

                                if (!(piecesRequired.length() == 0)) {
                                    outputStream.write(ContentManager.constructMessage(Content.INTERESTED, null, -1));
                                    outputStream.flush();
                                } else {
                                    outputStream
                                            .write(ContentManager.constructMessage(Content.NOT_INTERESTED, null, -1));
                                    outputStream.flush();

                                }

                                if (message.payload.length * 8 >= GeneralConfig.overallPieces) {
                                    numberOfCompleteDownloads += 1;
                                    fileWritten.put(peerID, true);
                                }

                                break;
                            case Content.INTERESTED:
                                ParentThread.log.logInfo(
                                        "Peer " + ParentThread.peerID + " received interested message from " + peerID);
                                interestedNeighbors.add(peerID);
                                break;
                            case Content.NOT_INTERESTED:

                                ParentThread.log.logInfo("Peer " + ParentThread.peerID
                                        + " received not interested message from " + peerID);
                                interestedNeighbors.remove(peerID);
                                break;
                            case Content.CHOKE:
                                ParentThread.log.logInfo("Peer " + ParentThread.peerID + " is choked by " + peerID);

                                // this neighbor has choked current peer
                                hasItBeenChoked = true;
                                break;
                            case Content.UNCHOKE:
                                ParentThread.log.logInfo("Peer " + ParentThread.peerID + " is unchoked by " + peerID);

                                // this neighbor has unchoked current peer
                                hasItBeenChoked = false;
                                // request piece required

                                piecesRequired = (BitSet) bitFieldMap.get(ParentThread.peerID).clone();
                                piecesRequired.xor(bitFieldMap.get(peerID));
                                piecesRequired.andNot(bitFieldMap.get(ParentThread.peerID));

                                if (requestedPieces == null) {
                                    requestedPieces = new BitSet(GeneralConfig.overallPieces);
                                }

                                if (!(piecesRequired.size() == 0)) {

                                    int pieceIndex = piecesRequired
                                            .nextSetBit(new Random().nextInt(piecesRequired.size()));
                                    if (pieceIndex < 0) {
                                        pieceIndex = piecesRequired.nextSetBit(0);
                                    }
                                    if (pieceIndex >= 0) {
                                        requestedPieces.set(pieceIndex);
                                        outputStream
                                                .write(ContentManager.constructMessage(Content.REQUEST, null,
                                                        pieceIndex));
                                        outputStream.flush();
                                    }
                                }

                                break;
                            case Content.REQUEST:
                                if (peerID == optimallyUnchokedPeer || preferredNeighbors.contains(peerID)) {
                                    int pieceIndex = message.pieceIndex;
                                    byte[] piece = new byte[GeneralConfig.sizeOfPiece];

                                    piece = getChunkData(pieceIndex);

                                    // send piece
                                    if (piece != null) {
                                        outputStream
                                                .write(ContentManager.constructMessage(Content.PIECE, piece,
                                                        pieceIndex));
                                        outputStream.flush();
                                    } else {
                                        System.out.println("null piece, index is " + pieceIndex);
                                    }

                                }
                                break;
                            case Content.PIECE:
                                fileData.put(message.pieceIndex, message.payload);
                                // set bitfield
                                setBitField(ParentThread.peerID, message.pieceIndex);

                                // send have message to all
                                for (Peer peer : ParentThread.peers.values()) {
                                    if (peer.socket != null) {
                                        DataOutputStream oStream = new DataOutputStream(peer.socket.getOutputStream());
                                        if (fileData.size() == GeneralConfig.overallPieces) {
                                            oStream.write(ContentManager.constructMessage(Content.HAVE, null,
                                                    GeneralConfig.overallPieces));
                                            oStream.write(
                                                    ContentManager.constructMessage(Content.HAVE, null,
                                                            message.pieceIndex));
                                        } else {
                                            oStream.write(
                                                    ContentManager.constructMessage(Content.HAVE, null,
                                                            message.pieceIndex));
                                        }
                                        oStream.flush();
                                    }

                                }

                                ParentThread.log
                                        .logInfo("Peer " + ParentThread.peerID + " has downloaded the piece from "
                                                + peerID + ". Now the number of pieces it has is " + fileData.size());

                                // keep asking more
                                piecesRequired = (BitSet) bitFieldMap.get(ParentThread.peerID).clone();
                                piecesRequired.xor(bitFieldMap.get(peerID));
                                piecesRequired.andNot(bitFieldMap.get(ParentThread.peerID));

                                requestedPieces = new BitSet(GeneralConfig.overallPieces);

                                if (!(piecesRequired.length() == 0) && !hasItBeenChoked) {
                                    int pieceIndex = piecesRequired.nextSetBit(0);
                                    requestedPieces.set(pieceIndex);
                                    outputStream
                                            .write(ContentManager.constructMessage(Content.REQUEST, null, pieceIndex));
                                    outputStream.flush();
                                    piecesRequired.andNot(requestedPieces);

                                }

                                if (fileData.size() == GeneralConfig.overallPieces) {

                                    ParentThread.log.logInfo(
                                            "Peer " + ParentThread.peerID + " has downloaded the complete file.");
                                    if (!fileWritten.get(ParentThread.peerID)) {
                                        numberOfCompleteDownloads += 1;
                                        writeFile();
                                        fileWritten.put(ParentThread.peerID, true);
                                    }
                                }

                                break;
                            case Content.HAVE:

                                if (message.pieceIndex == GeneralConfig.overallPieces) {
                                    numberOfCompleteDownloads += 1;
                                    fileWritten.put(peerID, true);
                                    break;
                                }
                                ParentThread.log.logInfo(
                                        "Peer " + ParentThread.peerID + " received have message from " + peerID);

                                setBitField(peerID, message.pieceIndex);
                                piecesRequired = (BitSet) bitFieldMap.get(ParentThread.peerID).clone();
                                piecesRequired.xor(bitFieldMap.get(peerID));
                                piecesRequired.andNot(bitFieldMap.get(ParentThread.peerID));

                                if (!(piecesRequired.length() == 0)
                                        && !(fileData.size() == GeneralConfig.overallPieces)) {
                                    outputStream.write(ContentManager.constructMessage(Content.INTERESTED, null, -1));
                                    outputStream.flush();
                                } else {
                                    outputStream
                                            .write(ContentManager.constructMessage(Content.NOT_INTERESTED, null, -1));
                                    outputStream.flush();
                                }

                                break;
                            default:
                                break;

                        }

                    }

                    Thread.sleep(5000);
                    System.exit(0);

                } catch (SocketException s) {
                    System.out.println("Socket connection closed with " + peerID);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }

    public static byte[] getChunkData(int chunkIndex) {

        return fileData.get(chunkIndex);
    }

    public static void writeChunkData(int chunkIndex, byte[] data) {

        fileData.put(chunkIndex, data);
    }

    public void writeFile() throws Exception {

        fileData = FileUpdation.sortChunkedData(fileData);
        File file = new File("./input_files/" + ParentThread.peerID + "/thefile");
        if (file.createNewFile()) {
            FileWriter fileWriter = new FileWriter(
                    "./input_files/" + ParentThread.peerID + "/" + GeneralConfig.nameOfFile,
                    true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (HashMap.Entry<Integer, byte[]> entry : fileData.entrySet()) {
                bufferedWriter.write(new String(entry.getValue(), StandardCharsets.UTF_8));
            }
            bufferedWriter.close();
            fileWriter.close();
        }

    }

    public static HashMap<Integer, Double> sortDownloadSpeeds(HashMap<Integer, Double> map) throws Exception {
        List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(map.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return -1 * (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<Integer, Double> temp = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> sorted : list) {
            temp.put(sorted.getKey(), sorted.getValue());
        }
        return temp;

    }

}
