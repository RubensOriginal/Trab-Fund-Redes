import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

    private static List<TabelaRoteamento> tabelaRoteamento = new CopyOnWriteArrayList<>();
    public static String localIp;
    public static SocketUDP socket;
    public static Roteamento roteamento;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Digite o IP da máquina:");
        localIp = sc.next();

        System.out.println("A rede já existe? [0 = Não, 1 = Sim]");
        boolean redeExiste = sc.next().equals("1") ? true : false;
        System.out.println(redeExiste);
        try {

            socket = new SocketUDP(19000);
            roteamento = new Roteamento(tabelaRoteamento, "./Roteamento.txt", socket, redeExiste);

            startRoteamento(roteamento);
            startRecebimento(socket);
            while(true) {
                String mensagem = sc.next();

                roteamento.enviaMensagem(mensagem);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void gerenciaMensagem(SocketUDP socket) {
        while(true) {
            try {
                SocketUDP.Pacote pacote = socket.receber();

                if (pacote.mensagem.startsWith("!")) {
                    roteamento.modificaRoteamento(pacote.ip, pacote.mensagem);
                } else if (pacote.mensagem.startsWith("@")) {
                    roteamento.adicionaRoteadorVizinho(pacote.mensagem);
                } else if (pacote.mensagem.startsWith("&")) {
                    roteamento.roteiaMensagem(pacote.mensagem);
                }
            } catch (IOException e) {
                System.out.println("Ocorreu um erro ao receber uma mensagem.");
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
                    System.out.println("Ocorreu um erro ao executar a thread T1.");
                    System.out.println(e);
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
    
        t2.start();
    }
}
