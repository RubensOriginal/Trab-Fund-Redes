import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Roteamento {

    private List<TabelaRoteamento> tabelaRoteamento;
    private int timer;
    private SocketUDP socket;

    public Roteamento(List<TabelaRoteamento> tabelaRoteamento, String file, SocketUDP socket, boolean redeExiste) throws FileNotFoundException, IOException {
        //TODO: Adicionar o Socket UDP
        this.tabelaRoteamento = tabelaRoteamento;
        this.socket = socket;
        timer = 15;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                tabelaRoteamento.add(new TabelaRoteamento(line, 1, line));
                try {
                    if (redeExiste)
                        socket.enviar("@" + Main.localIp, line);
                } catch (Exception e) {
                    System.out.println("Não foi possível anunciar para o IP" + line);
                }
            }
        }
    }

    public void modificaRoteamento(String origem, String mensagem) {
        boolean modificou = false;

        String[] tabela = mensagem.substring(1, mensagem.length()).split("!");

        for (String valor : tabela) {
            String[] split = valor.split(":");

            List<TabelaRoteamento> roteamento = tabelaRoteamento.stream().filter(e -> e.getIp() == split[0]).toList();

            if (roteamento.size() == 0) {
                tabelaRoteamento.add(new TabelaRoteamento(split[0], Integer.parseInt(split[1]) + 1, origem));
                System.out.println("Adiciona: " + split[0] + " - Métrica " + (Integer.parseInt(split[1]) + 1) + " - Saída " + origem);
                modificou = true;
            } else {
                TabelaRoteamento roteador = roteamento.get(0);

                if (roteador.getMetrica() + 1 > Integer.parseInt(split[1])) {
                    roteador.set(split[0], Integer.parseInt(split[1]) + 1, origem);
                    modificou = true;
                }
            }

        }

        if (modificou) {
            propagaRoteamento();
        }

    }

    public void adicionaRoteadorVizinho(String mensagem) {
        boolean modificou = false;

        String vizinho = mensagem.substring(1, mensagem.length());

        List<TabelaRoteamento> roteamento = tabelaRoteamento.stream().filter(e -> e.getIp() == vizinho).toList();

        if (roteamento.size() == 0) {
            System.out.println("Adiciona vizinho: " + vizinho + " - Métrica " + 1);
            tabelaRoteamento.add(new TabelaRoteamento(vizinho, 1, vizinho));
            modificou = true;
        }

        if (modificou)
            propagaRoteamento();
    }

    public void gerenciaRoteamento() throws InterruptedException {
        while(true) {
            for (TabelaRoteamento rota : tabelaRoteamento) {
                if (rota.decreaseTTL()) {
                    System.out.println("Removeu a rota " + rota.toString());
                    tabelaRoteamento.remove(rota);
                }
            }
         
            if (timer >= 15) {
                propagaRoteamento();
            } else {
                timer++;
            }
            Thread.sleep(1000);
        }
    }

    public void propagaRoteamento() {
        System.out.println("Propaga Tabela");
        printTabelaRoteamento();
    
        List<TabelaRoteamento> vizinhos = tabelaRoteamento.stream().filter(e -> e.getMetrica() == 1).toList();

        StringBuilder messagem = new StringBuilder();

        tabelaRoteamento.stream().forEach(e -> messagem.append("!" + e.getIp() + ":" + e.getMetrica()));

        for (TabelaRoteamento vizinho : vizinhos) {
            try {
            socket.enviar(messagem.toString(), vizinho.getIp());
            } catch (Exception e) {
                System.out.println("Não foi possível propagar para o IP " + vizinho.getIp());
            }
        }

        timer = 0;
    }

    private void printTabelaRoteamento() {
        System.out.println("-----------------------------------------------------");
        System.out.println("| IP              | Métrica | Saída           | TTL |");
        tabelaRoteamento.forEach(e -> System.out.printf("| %15s | %7s | %15s | %3s |\n", e.getIp(), e.getMetrica(), e.getSaida(), e.getTTL()));
        System.out.println("-----------------------------------------------------");
    }
}
