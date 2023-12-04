
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.io.DataOutputStream;
import java.util.Random;
import java.util.logging.Formatter;

import input_processing.GeneralConfig;
import message_files.Content;
import message_files.ContentManager;

import java.util.logging.*;
import java.util.Date;

class LogRecords {

    Logger log;
    FileHandler file;

    LogRecords(String peerId) throws IOException {
        log = Logger.getLogger(peerId);
        file = new FileHandler("./input_files/" + peerId + "/logs_" + peerId + ".log");
        file.setFormatter(new MyNewFormatter());
        log.addHandler(file);

    }

    public void logInfo(String message) {
        log.log(new LogRecord(Level.INFO, message));
    }

    public void logError(String message) {
        log.log(new LogRecord(Level.SEVERE, message));
    }

    class MyNewFormatter extends Formatter {

        @Override
        public String format(LogRecord logRecord) {
            return new Date(logRecord.getMillis()) + " : " + logRecord.getMessage() + "\n";
        }

    }

}

class ParentThread implements Runnable {

    static final String PEERINFO_PATH = "input_files/PeerInfo.cfg";

    static int peerID;
    static TreeMap<Integer, Peer> peers;
    static LogRecords log;
    static HashMap<Integer, byte[]> fileData;

    public ParentThread(int peerID) throws Exception {
        ParentThread.peerID = peerID;
        peers = readPeerInfo();

        // Creating Peer directories
        try {

            FileUpdation.initializeDirectories(peerID, GeneralConfig.nameOfFile);
            log = new LogRecords(String.valueOf(ParentThread.peerID));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        new Thread(new StartupClient()).start();
        new Thread(new StartupServer()).start();
        new Thread(new DeterminePreferredNeighbors()).start();
        new Thread(new OmptimallyUnChokeInterestedNeighbors()).start();

    }

    private class StartupServer implements Runnable {

        byte[] handshakePacket = new byte[32];

        @Override
        public void run() {
            try {
                // Wait for new connections at designated port
                int port = peers.get(peerID).port;
                ServerSocket serverSocket = new ServerSocket(port);
                log.logInfo("Server: " + ParentThread.peerID + " Started on Port Number :" + port);
                boolean newPeers = false;
                for (Map.Entry<Integer, Peer> neighbor : peers.entrySet()) {
                    if (newPeers) {
                        Socket socket = serverSocket.accept();
                        ObjectInputStream serverInputStream = new ObjectInputStream(socket.getInputStream());
                        ObjectOutputStream serverOutputStream = new ObjectOutputStream(socket.getOutputStream());

                        serverInputStream.read(handshakePacket);

                        serverOutputStream.write(ContentManager.generateHandshake(peerID));
                        serverOutputStream.flush();

                        log.logInfo("Peer :" + peerID + " makes a connection to" + neighbor.getKey());
                        neighbor.getValue().startMessageExchange(socket);
                    }
                    if (peerID == neighbor.getKey())
                        newPeers = true;

                }

                serverSocket.close();

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private class StartupClient implements Runnable {

        @Override
        public void run() {
            try {
                for (Map.Entry<Integer, Peer> peer : peers.entrySet()) {

                    if (peer.getKey() == peerID)
                        break;
                    Peer neighbor = peer.getValue();
                    Socket socket = new Socket(neighbor.hostName, neighbor.port);
                    // Input and output Streams
                    ObjectOutputStream clientOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream clientInputStream = new ObjectInputStream(socket.getInputStream());

                    // creating the handshake header and writing it into output stream
                    byte[] handshakePacket = ContentManager.generateHandshake(peerID);
                    clientOutputStream.write(handshakePacket);
                    clientOutputStream.flush();

                    // Reading handshake packet from the server and authenticating it
                    clientInputStream.readFully(handshakePacket);
                    String messageHeader = GeneralConfig.bytesToString(handshakePacket, 0, 17);
                    String messagePeerID = GeneralConfig.bytesToString(handshakePacket, 28, 31);

                    if (messageHeader.equals("P2PFILESHARINGPROJ")
                            && Integer.parseInt(messagePeerID) == peer.getKey()) {
                        // log.logInfo("Client received back handshake from the " + peer.getKey());
                        neighbor.startMessageExchange(socket);
                    } else {
                        socket.close();
                    }

                }

            } catch (IOException exception) {

                exception.printStackTrace();

            }
        }
    }

    public static TreeMap<Integer, Peer> readPeerInfo() throws Exception {
        ArrayList<String> lines = GeneralConfig.readLinesFromFile(PEERINFO_PATH);
        TreeMap<Integer, Peer> peerInfo = new TreeMap<>();
        for (String line : lines) {
            String[] words = line.split(" ");
            peerInfo.put(Integer.valueOf(words[0]), new Peer(Integer.parseInt(words[0]), words[1],
                    Integer.valueOf(words[2]), Integer.parseInt(words[3])));
        }
        return peerInfo;
    }

}

public class PeerProcess {

    static final String COMMON_CONFIG_PATH = "input_files/Common.cfg";

    public static void main(String[] args) throws Exception {

        int peerID = Integer.parseInt(args[0]);
        GeneralConfig.readFile(COMMON_CONFIG_PATH);

        new Thread(new ParentThread(peerID)).start();
    }

}

class OmptimallyUnChokeInterestedNeighbors implements Runnable {

    public OmptimallyUnChokeInterestedNeighbors() {
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                while (Peer.numberOfCompleteDownloads < ParentThread.peers.size()) {

                    HashSet<Integer> preferredCandidates = new HashSet<Integer>(Peer.interestedNeighbors);
                    HashSet<Integer> clonePreferred = new HashSet<Integer>(Peer.preferredNeighbors);
                    preferredCandidates.removeAll(clonePreferred);

                    Random rand = new Random();
                    if (preferredCandidates.size() > 0) {
                        int chosenInterestedNeighbor = rand.nextInt(preferredCandidates.size());
                        Peer.optimallyUnchokedPeer = (int) preferredCandidates.toArray()[chosenInterestedNeighbor];

                        Peer.chokingMap.put(Peer.optimallyUnchokedPeer, false);
                        Socket socket = ParentThread.peers.get(Peer.optimallyUnchokedPeer).socket;
                        if (socket == null) {
                            break;
                        }
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        dataOutputStream.write(ContentManager.constructMessage(Content.UNCHOKE, null, -1));
                        dataOutputStream.flush();
                    }
                    ParentThread.log.logInfo("Peer " + ParentThread.peerID
                            + " has the optimistically unchoked neighbor " + Peer.optimallyUnchokedPeer);

                    Thread.sleep(GeneralConfig.optimisticUnchokingRange * 1000);

                }

            } catch (SocketException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

class DeterminePreferredNeighbors implements Runnable {

    public DeterminePreferredNeighbors() {
    }

    @Override
    public void run() {
        synchronized (this) {

            try {
                while (Peer.numberOfCompleteDownloads < ParentThread.peers.size()) {

                    int KNeighbors = GeneralConfig.noOfPreferredNeighbours;
                    Peer.preferredNeighbors.clear();

                    if (Peer.interestedNeighbors.size() > KNeighbors) {
                        int i = 0;
                        for (HashMap.Entry<Integer, Double> e : Peer.lastIntervalDownloadSpeeds.entrySet()) {
                            Peer.preferredNeighbors.add(e.getKey());
                            i++;
                            if (i >= KNeighbors) {
                                break;
                            }
                        }
                    } else {
                        for (Integer peerID : Peer.interestedNeighbors) {
                            Peer.preferredNeighbors.add(peerID);
                        }
                    }

                    Peer.lastIntervalDownloadSpeeds.replaceAll((key, value) -> 0.0);

                    for (HashMap.Entry<Integer, Boolean> pair : Peer.chokingMap.entrySet()) {
                        Socket socket = ParentThread.peers.get(pair.getKey()).socket;
                        if (socket == null) {
                            continue;
                        }
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        if (Peer.preferredNeighbors.contains(pair.getKey())) {
                            dataOutputStream.write(ContentManager.constructMessage(Content.UNCHOKE, null, -1));
                            dataOutputStream.flush();
                            Peer.chokingMap.put(pair.getKey(), false);

                        } else {

                            dataOutputStream.write(ContentManager.constructMessage(Content.CHOKE, null, -1));
                            dataOutputStream.flush();
                            Peer.chokingMap.put(pair.getKey(), true);

                        }
                    }
                    ParentThread.log.logInfo("Peer " + ParentThread.peerID + " has the preferred neighbors "
                            + Peer.preferredNeighbors.toString());

                    Thread.sleep(GeneralConfig.unchokingRange * 1000);

                }

            } catch (SocketException e) {
            } catch (Exception e) {

                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}
