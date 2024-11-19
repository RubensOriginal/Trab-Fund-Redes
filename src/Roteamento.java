import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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

    public void roteiaMensagem(String mensagem) {

        String[] retorno = mensagem.split("%");

        if (retorno[1].equals(Main.localIp)) {
            System.out.printf("----\nMensagem chegou ao Destino:\nIP Origem: %s\nIP Destino: %s\nMensagem: %s\n----", retorno[0], retorno[1], retorno[2]);
            // Mensagem para esse roteador
        } else {
            // Mensagem para outro roteador
            System.out.printf("----\nRepassando a mensagem:\nIP Origem: %s\nIP Destino: %s\nMensagem: %s\n----", retorno[0], retorno[1], retorno[2]);
            List<TabelaRoteamento> roteadorList = tabelaRoteamento.stream().filter(e -> e.getIp() == retorno[1]).toList();

            if (roteadorList.size() > 0) {
                TabelaRoteamento roteador = roteadorList.get(0);
                try {
                    socket.enviar(mensagem, roteador.getSaida());
                } catch (Exception e1) {
                    System.out.println("Ocorreu um erro ao repassar a mensagem.");
                }
            }
        }

    }

    public void enviaMensagem(String mensagem) {
        String[] partes = mensagem.split("/");

        List<TabelaRoteamento> roteadores = tabelaRoteamento.stream().filter(e -> e.getIp().equals(partes[0])).toList();

        System.out.println(roteadores.size());

        if (roteadores.size() > 0) {

            TabelaRoteamento roteador = roteadores.get(0);
            try {
                socket.enviar("&" + Main.localIp + "%" + partes[0] + "%" + partes[1], roteador.getSaida());
                System.out.println("Mensagem enviada.");
            } catch (Exception e1) {
                System.out.println("Não foi possível enviar a mensagem.");
            }
        }
    }

    public void modificaRoteamento(String origem, String mensagem) {
        boolean modificou = false;

        String[] tabela = mensagem.substring(1, mensagem.length()).split("!");

        List<TabelaRoteamento> roteadores = tabelaRoteamento.stream().filter(e -> e.getIp().equals(origem)).toList();
        if (roteadores.size() == 1) {
            TabelaRoteamento roteador = roteadores.get(0);
            if (roteador.getMetrica() == 1)
                roteador.resetTTL();
            else
                roteador.set(origem, 1, origem);
        } else {
            tabelaRoteamento.add(new TabelaRoteamento(origem, 1, origem));
        }

        for (String valor : tabela) {
            String[] split = valor.split(":");

            if (split[0].equals(Main.localIp))
                continue;

            if (split[1].length() > 3) {
                System.out.println("Valor inválido: " + valor);
                continue;
            }

            List<TabelaRoteamento> roteamento = tabelaRoteamento.stream().filter(e -> e.getIp().equals(split[0])).toList();

            if (roteamento.size() == 0) {
                tabelaRoteamento.add(new TabelaRoteamento(split[0], Integer.parseInt(split[1]) + 1, origem));
                System.out.println("Adiciona: " + split[0] + " - Métrica " + (Integer.parseInt(split[1]) + 1) + " - Saída " + origem);
                modificou = true;
            } else {
                TabelaRoteamento roteador = roteamento.get(0);

                if (roteador.getMetrica() > Integer.parseInt(split[1]) + 1) {
                    roteador.set(split[0], Integer.parseInt(split[1]) + 1, origem);
                    modificou = true;
                }

                if (roteador.getSaida().equals(origem))

                roteador.resetTTL();
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
        } else {
            TabelaRoteamento roteador = roteamento.get(0);

            roteador.set(vizinho, 1, vizinho);
        }

        if (modificou)
            propagaRoteamento();
    }

    public void gerenciaRoteamento() throws InterruptedException {
        while(true) {

            List<String> removidos = new ArrayList<>();

            for (TabelaRoteamento rota : tabelaRoteamento) {
                if (rota.decreaseTTL()) {
                    System.out.println("Removeu a rota " + rota.toString());
                }
            }

            for (int i = 0; i < tabelaRoteamento.size(); i++) {
                if (tabelaRoteamento.get(i).getTTL() <= 0) {
                    removidos.add(tabelaRoteamento.get(i).getIp());
                    tabelaRoteamento.remove(i);
                    i--;
                }
            }
            
            for (int i = 0; i < tabelaRoteamento.size(); i++) {
                for (String removido : removidos) {
                    if (tabelaRoteamento.get(i).getSaida().equals(removido)) {
                        tabelaRoteamento.remove(i);
                        i--;
                    }
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
