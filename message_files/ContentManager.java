package message_files;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ContentManager {

    // public static byte[] makeMessage(int type, byte[] payload, int pieceIndex) {

    // return Content.make(type, payload, pieceIndex);
    // }

    // public static Content receiveMessage(byte[] messageArray) {

    // Content message = new Content();
    // message.read(messageArray);

    // return message;
    // }

    // public static byte[] createHandShake(int peerID) {
    // byte[] handShakePacket = new byte[32];

    // byte[] handShakeHeader = "P2PFILESHARINGPROJ".getBytes();
    // byte[] zeroPadding = "0000000000".getBytes();
    // byte[] peerIDInBytes =
    // ByteBuffer.allocate(4).put(String.valueOf(peerID).getBytes()).array();

    // int idx = 0;

    // for (int i = 0; i < handShakeHeader.length; i += 1) {
    // handShakePacket[idx] = handShakeHeader[i];
    // idx += 1;
    // }

    // for (int i = 0; i < zeroPadding.length; i += 1) {
    // handShakePacket[idx] = zeroPadding[i];
    // idx += 1;
    // }

    // for (int i = 0; i < peerIDInBytes.length; i += 1) {
    // handShakePacket[idx] = peerIDInBytes[i];
    // idx += 1;
    // }

    // System.out.println("Hand Shake Packet --- " + new String(handShakePacket,
    // StandardCharsets.UTF_8));
    // return handShakePacket;

    // }

    public static byte[] constructMessage(int messageType, byte[] messagePayload, int pieceIndex) {
        return Content.make(messageType, messagePayload, pieceIndex);
    }

    public static Content parseMessage(byte[] messageBytes) {
        Content parsedMessage = new Content();
        parsedMessage.read(messageBytes);
        return parsedMessage;
    }

    public static byte[] generateHandshake(int peerIdentifier) {
        byte[] handshakePacket = new byte[32];

        byte[] handshakeHeader = "P2PFILESHARINGPROJ".getBytes();
        byte[] zeroBytesPadding = "0000000000".getBytes();
        byte[] peerIdBytes = ByteBuffer.allocate(4).put(String.valueOf(peerIdentifier).getBytes()).array();

        int packetIndex = 0;

        for (int i = 0; i < handshakeHeader.length; i++) {
            handshakePacket[packetIndex++] = handshakeHeader[i];
        }

        for (int i = 0; i < zeroBytesPadding.length; i++) {
            handshakePacket[packetIndex++] = zeroBytesPadding[i];
        }

        for (int i = 0; i < peerIdBytes.length; i++) {
            handshakePacket[packetIndex++] = peerIdBytes[i];
        }

        System.out.println("Handshake Packet --- " + new String(handshakePacket, StandardCharsets.UTF_8));
        return handshakePacket;
    }

}