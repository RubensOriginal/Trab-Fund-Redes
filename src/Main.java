import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import SocketUDP.Pacote;

public class Main {

    private static List<TabelaRoteamento> tabelaRoteamento = new ArrayList<>();
    public static String localIp;
    public static SocketUDP socket;
    public static Roteamento roteamento;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Digite o IP da máquina:");
        localIp = sc.next();

        System.out.println("A rede já existe? [0 = Não, 1 = Sim]");
        boolean redeExiste = sc.next() == "1" ? true : false;
        try {

            socket = new SocketUDP(19000);
            roteamento = new Roteamento(tabelaRoteamento, localIp, socket, redeExiste);

            startRoteamento(roteamento);
            startRecebimento(socket);

        } catch (Exception e) {

        }
    }

    public static void gerenciaMensagem(SocketUDP socket) {
        while(true) {
            try {
                SocketUDP.Pacote pacote = socket.receber();

                if (mensagem.startsWith("!")) {
                    roteamento.modificaRoteamento(mensagem, mensagem);
                }
            } catch (IOException e) {

            }
        }
    }

    public static void startRoteamento(Roteamento roteamento) {
        Thread t1 = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    roteamento.gerenciaRoteamento();
                } catch (Exception e) {

                }
            }

        });

        t1.start();
    }

    public static void startRecebimento(SocketUDP socket) {
        Thread t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                gerenciaMensagem(socket);
            }
        });
    }
}