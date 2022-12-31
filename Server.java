import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
	private Selector readSelector;
	volatile private boolean hasClients;
	public static final int BUFFER_SIZE = 32;
	public static final int POOL_SIZE = 20;
	private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
	// private static String encoding = System.getProperty("file.encoding");
	// public static final Charset CS = Charset.forName(encoding);
	// private ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

	public Server() {
		try {
			readSelector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addClient(SocketChannel sc) {
		try {
			sc.configureBlocking(false);
			// System.out.println("wake up");
			sc.register(readSelector, SelectionKey.OP_READ);
			readSelector.wakeup();
			// synchronized (readSelector) {
			// 	readSelector.notify();
			// }
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println("add client : " + clients.size());
		hasClients = true;
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (hasClients) {
					// System.out.println("blocking");
					int keys = readSelector.select();
					if (keys == 0)
						continue;
						// synchronized (readSelector) {
						// 	readSelector.wait(32);
						// }
					// System.out.println("select : ");
					// System.out.println("read sel : " + readSelector.selectedKeys().size());
					Iterator<SelectionKey> it = readSelector.selectedKeys().iterator();
					while (it.hasNext()) {
						var selectionKey = it.next();
						it.remove();
						if (selectionKey.isValid() && selectionKey.isReadable()) {
							SocketChannel sc = (SocketChannel) selectionKey.channel();
							int byteRead = -1;
							// sync the buffer if need deep copy for writing
							// synchronized (buffer) {
							try {
								byteRead = sc.read(buffer);
							} catch (IOException e) {
								e.printStackTrace();
								selectionKey.cancel();
							}
							// }

							if (byteRead != -1) {
								// String msg = CS.decode(buffer.flip()).toString();
								// System.out.println("read from " + sc.getRemoteAddress() + ": " + msg);
								buffer.flip();
								writeToEachClient();
							} else {
								selectionKey.cancel();
							}
							buffer.clear();
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}
	}

	public void writeToEachClient() {
		var it = readSelector.keys().iterator();
		while(it.hasNext()) {
			var sc = (SocketChannel) it.next().channel();
			try {
				// A socket channel in non-blocking mode, for example,
				// cannot write any more bytes than are free in the socket's output buffer.
				// the write method is thread-safe
				sc.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
				it.remove();
			}
			buffer.rewind();
		}
	}

	public void closeAll() {
		for(var key : readSelector.keys()) {
			key.cancel();
		}
	}
}
