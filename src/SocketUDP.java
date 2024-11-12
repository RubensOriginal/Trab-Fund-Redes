import java.io.IOException;
import java.net.*;

public class SocketUDP{
    private DatagramSocket socket;
    private int porta;

    public SocketUDP(int porta) throws SocketException {
        this.porta = porta;
        this.socket = new DatagramSocket(porta);
    }

    public class Pacote{
        public String ip;
        public String mensagem;

        public Pacote(String ip, String mensagem) {
            this.ip = ip;
            this.mensagem = mensagem;
        }
    }

    //envio de pacotes
    public void enviar(String mensagem, String ipDestino) throws Exception {
        byte[] buffer = mensagem.getBytes();
        InetAddress ip = InetAddress.getByName(ipDestino);
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, ip, porta);
        socket.send(pacote);
    }

    //recebimento de pacotes
    public Pacote receber() throws IOException{
        byte[] buffer = new byte[1024];
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
        socket.receive(pacote);

        String mensagem = new String(pacote.getData(), 0, pacote.getLength());

        String ip = pacote.getAddress().toString();

        return new Pacote(ip.substring(1), mensagem); 
    }

    //fecha o socket
    public void fechaSocket(){
        socket.close();
    }
}