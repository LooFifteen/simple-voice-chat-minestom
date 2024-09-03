package dev.lu15.voicechat.network.voice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VoiceSocket {

    private final byte @NotNull[] buffer = new byte[4096];

    private @Nullable DatagramSocket socket;

    public void open(@NotNull InetAddress address, int port) throws SocketException {
        if (this.socket != null) throw new IllegalStateException("socket already open");

        this.socket = new DatagramSocket(port, address);

        // https://datatracker.ietf.org/doc/html/rfc1349
        // setting this will allow the socket to prioritize reliability over speed
        this.socket.setTrafficClass(0x04);
    }

    public @NotNull RawPacket read() throws IOException {
        if (this.socket == null || this.socket.isClosed()) throw new IllegalStateException("socket not open");

        DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
        this.socket.receive(packet);

        long timestamp = System.currentTimeMillis();
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

        return new RawPacket(data, packet.getSocketAddress(), timestamp);
    }

    public void write(byte @NotNull[] data, @NotNull SocketAddress address) throws IOException {
        if (this.socket == null || this.socket.isClosed()) throw new IllegalStateException("socket not open");
        socket.send(new DatagramPacket(data, data.length, address));
    }

    public void close() {
        if (this.socket == null || this.socket.isClosed()) return;
        this.socket.close();
        this.socket = null;
    }

    public boolean closed() {
        return this.socket == null || this.socket.isClosed();
    }

}
