import java.io.IOError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Listener {
	public static final int PORT = 8080;
	private Server server = new Server();
	private ServerSocketChannel ssc;
	private Selector sel;

	public Listener() throws IOException {
		ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(PORT));
		// sel = Selector.open();
		new Thread(server).start();
		try {
			blockAccept();
		} finally {
			ssc.close();
			sel.close();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		new Listener();
	}

	private void blockAccept() {
		try {
			ssc.configureBlocking(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				server.addClient(ssc.accept());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void selectAccept() {
		try {
			ssc.configureBlocking(false);
			ssc.register(sel, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				sel.select();
				Iterator<SelectionKey> it = sel.selectedKeys().iterator();
				while (it.hasNext()) {
					var selectionKey = it.next();
					it.remove();
					if (selectionKey.isAcceptable()) {
						server.addClient(ssc.accept());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}