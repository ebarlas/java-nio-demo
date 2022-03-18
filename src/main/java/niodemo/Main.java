package niodemo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress("localhost", 8000));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(1_024 * 64);
        while (true) {
            int numSelectedKey = selector.select();
            System.out.println("op=select, numSelectedKey=" + numSelectedKey);
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectionKeys.iterator();
            while (it.hasNext()) {
                SelectionKey selectionKey = it.next();
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel socketChannel = channel.accept();
                    System.out.println("op=accept, socket=" + socketChannel.socket());
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (selectionKey.isWritable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer writeBuffer = (ByteBuffer) selectionKey.attachment();
                    int numBytesWritten = socketChannel.write(writeBuffer);
                    System.out.println("op=write, numBytesWritten=" + numBytesWritten);
                    if (!writeBuffer.hasRemaining()) {
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    }
                } else if (selectionKey.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    int numBytesRead = socketChannel.read(readBuffer);
                    System.out.println("op=read, numBytesRead=" + numBytesRead);
                    readBuffer.flip();
                    ByteBuffer writeBuffer = ByteBuffer.allocate(readBuffer.remaining());
                    writeBuffer.put(readBuffer);
                    writeBuffer.flip();
                    readBuffer.clear();
                    socketChannel.register(selector, SelectionKey.OP_WRITE, writeBuffer);
                }
                it.remove();
            }
        }
    }

}
