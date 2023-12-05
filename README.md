# Bit Torrent Peer to Peer File sharing
# Bittorrent protocol implementation

In this project, we are writing a P2P file sharing software similar to BitTorrent in Java.

BitTorrent is a popular P2P protocol for file distribution. Among its interesting features,
you are asked to implement the choking-unchoking mechanism which is one of the most
important features of BitTorrent. In the following
Protocol Description section, you can
read the protocol description, which has been modified a little bit from the original
BitTorrent protocol.

## Instructions to run the files.

```bash
make 
```

## Note : (To clean the class files)

```bash
make clean
```
## Usage
Now we need to manually connect the peers to share the files using the below command.

```bash
java PeerProcess 1001
``` 
```bash
java PeerProcess 1002
```
```bash
java PeerProcess 1003
```
```bash
java PeerProcess 1004
```
