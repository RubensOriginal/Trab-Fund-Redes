public class TabelaRoteamento {

    private String ip;
    private int metrica;
    private String saida;
    private int ttl;

    public TabelaRoteamento(String ip, int metrica, String saida) {
        this.ip = ip;
        this.metrica = metrica;
        this.saida = saida;
        ttl = 35;
    }

    public boolean decreaseTTL() {
        ttl--;

        return ttl <= 0;
    }

    public void set(String ip, int metrica, String saida) {

    }

    public String getIp() {
        return ip;
    }

    public int getMetrica() {
        return metrica;
    }

    @Override
    public String toString() {
        return "TabelaRoteamento [ip=" + ip + ", metrica=" + metrica + ", saida=" + saida + ", ttl=" + ttl + "]";
    }

    
}
