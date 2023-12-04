package message_files;

import java.util.Arrays;

import input_processing.GeneralConfig;

public class Content {

    public int typeID;
    public int pieceIndex;
    public byte[] payload;

    public static final int CHOKE = 0;
    public static final int UNCHOKE = 1;
    public static final int INTERESTED = 2;
    public static final int NOT_INTERESTED = 3;
    public static final int HAVE = 4;
    public static final int BITFIELD = 5;
    public static final int REQUEST = 6;
    public static final int PIECE = 7;

    public static byte[] make(int type, byte[] payload, int pieceIndex) {
        byte[] mess;

        switch (type) {
            case CHOKE:
            case UNCHOKE:
            case INTERESTED:
            case NOT_INTERESTED:
                mess = new byte[5];
                GeneralConfig.insertIntIntoByteArray(mess, 1, 0);
                mess[4] = (byte) type;
                return mess;
            case HAVE:
            case REQUEST:
                mess = new byte[9];
                GeneralConfig.insertIntIntoByteArray(mess, 5, 0);
                mess[4] = (byte) type;
                GeneralConfig.insertIntIntoByteArray(mess, pieceIndex, 5);

                return mess;
            case BITFIELD:

                mess = new byte[(5 + payload.length)];
                GeneralConfig.insertIntIntoByteArray(mess, 1 + payload.length, 0);
                mess[4] = (byte) type;
                for (int i = 0; i < payload.length; i++) {
                    mess[5 + i] = payload[i];
                }
                return mess;

            case PIECE:

                mess = new byte[(9 + payload.length)];

                GeneralConfig.insertIntIntoByteArray(mess, 1 + payload.length, 0);

                mess[4] = (byte) type;

                GeneralConfig.insertIntIntoByteArray(mess, pieceIndex, 5);

                for (int i = 0; i < payload.length; i++) {
                    mess[9 + i] = payload[i];
                }
                return mess;
            default:
                return (new byte[0]);
        }
    }

    public void read(byte[] message) {
        int len = message.length;
        if (message.length == 0) {
            this.typeID = -1;
            return;
        }
        int type = message[0];
        if (type >= 0 && type <= 7) {
            this.typeID = type;

            if (len == 5 && (type == HAVE || type == REQUEST)) {
                pieceIndex = GeneralConfig.byteArrayToInt(Arrays.copyOfRange(message, 1, 5));
            } else if (type == PIECE) {
                pieceIndex = GeneralConfig.byteArrayToInt(Arrays.copyOfRange(message, 1, 5));
                this.payload = new byte[len - 5];
                for (int i = 5; i < len; i++) {
                    this.payload[i - 5] = message[i];
                }
            } else if (type == BITFIELD) {
                this.payload = new byte[len - 1];
                for (int i = 1; i < len; i++) {
                    this.payload[i - 1] = message[i];
                }
            }
        }
    }

}