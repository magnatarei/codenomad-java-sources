/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  45. Monitor de alterações numa pasta
 * ------------------------------------------------------------
 * O que faz: observa uma pasta por consulta periódica (tira uma "foto" dos
 *            arquivos a cada intervalo e compara com a anterior) e registra,
 *            com hora, cada arquivo criado, alterado ou removido. Opcionalmente
 *            desce nas subpastas e para sozinho depois de N consultas.
 * Como usar: java MonitorArquivos.java ./downloads
 *            java MonitorArquivos.java ./projeto 500 --recursivo --rodadas 20
 *            java MonitorArquivos.java --demo
 * O que você aprende: Files.walk e Files.list com streams, atributos de
 *            arquivo (tamanho e data de modificação), records como chave de
 *            comparação, diferença entre dois mapas, threads daemon e pastas
 *            temporárias que se limpam sozinhas.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class MonitorArquivos {

    /** O que guardamos de cada arquivo entre uma consulta e outra. Records comparam por valor. */
    record Estado(long tamanho, FileTime modificado) {}

    /** Um evento detectado: tipo (criado, alterado, removido), arquivo e detalhe. */
    record Evento(String tipo, Path arquivo, String detalhe) {}

    final Path pasta;
    final boolean recursivo;
    Map<Path, Estado> anterior;

    MonitorArquivos(Path pasta, boolean recursivo) throws IOException {
        this.pasta = pasta;
        this.recursivo = recursivo;
        this.anterior = fotografar();                       // a primeira foto é a referência
    }

    /** Lê a pasta inteira e devolve um mapa ordenado de caminho relativo para estado. */
    Map<Path, Estado> fotografar() throws IOException {
        var foto = new TreeMap<Path, Estado>();
        // Files.walk visita subpastas; Files.list fica só no primeiro nível. Os dois devolvem Stream.
        try (Stream<Path> fluxo = recursivo ? Files.walk(pasta) : Files.list(pasta)) {
            for (Path p : fluxo.filter(Files::isRegularFile).toList()) {
                try {
                    foto.put(pasta.relativize(p), new Estado(Files.size(p), Files.getLastModifiedTime(p)));
                } catch (IOException e) {
                    // O arquivo pode sumir entre o listar e o medir; na próxima rodada acertamos.
                }
            }
        }
        return foto;
    }

    /** Compara a foto nova com a anterior e devolve o que mudou. */
    List<Evento> verificar() throws IOException {
        Map<Path, Estado> atual = fotografar();
        var eventos = new ArrayList<Evento>();
        for (var e : atual.entrySet()) {
            Estado antes = anterior.get(e.getKey());
            if (antes == null) {
                eventos.add(new Evento("criado", e.getKey(), bytes(e.getValue().tamanho())));
            } else if (!antes.equals(e.getValue())) {
                eventos.add(new Evento("alterado", e.getKey(), bytes(antes.tamanho()) + " -> " + bytes(e.getValue().tamanho())));
            }
        }
        for (Path p : anterior.keySet()) {
            if (!atual.containsKey(p)) eventos.add(new Evento("removido", p, ""));
        }
        anterior = atual;
        return eventos;
    }

    /** Laço de consulta: dorme, verifica, imprime. Com rodadas = 0 roda até ser interrompido. */
    void observar(int intervaloMs, int rodadas) throws IOException, InterruptedException {
        System.out.printf("Observando %s a cada %d ms%s, %d arquivos no início.%n", pasta, intervaloMs,
                recursivo ? " (com subpastas)" : "", anterior.size());
        for (int rodada = 1; rodadas == 0 || rodada <= rodadas; rodada++) {
            Thread.sleep(intervaloMs);
            for (Evento ev : verificar()) {
                System.out.printf("[%s] %-8s %s %s%n", hora(), ev.tipo(), ev.arquivo(), ev.detalhe());
            }
        }
        System.out.println("Encerrado após " + rodadas + " consultas.");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        Path pasta = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isDirectory(pasta)) falhar("'" + args[0] + "' não existe ou não é uma pasta.");
        int intervalo = 1000, rodadas = 0;
        boolean recursivo = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--recursivo" -> recursivo = true;
                case "--rodadas" -> rodadas = inteiro(i + 1 < args.length ? args[i + 1] : "", "--rodadas", 1);
                default -> {
                    if (i == 1) intervalo = inteiro(args[i], "intervalo", 100);
                    else if (!args[i - 1].equals("--rodadas")) falhar("não conheço a opção '" + args[i] + "'.");
                }
            }
        }
        new MonitorArquivos(pasta, recursivo).observar(intervalo, rodadas);
    }

    static int inteiro(String texto, String nome, int minimo) {
        try {
            int n = Integer.parseInt(texto);
            if (n < minimo) falhar(nome + " precisa ser pelo menos " + minimo + "; recebi " + n + ".");
            return n;
        } catch (NumberFormatException e) {
            falhar(nome + " precisa ser um número inteiro; recebi '" + texto + "'.");
            return 0;
        }
    }

    /** Cria uma pasta temporária, provoca os três eventos numa thread paralela e observa por 3 segundos. */
    static void demo() throws IOException, InterruptedException {
        System.out.println("=== Demonstração: criar, alterar e remover um arquivo ===");
        Path pasta = Files.createTempDirectory("codenomad-monitor");
        Path existente = pasta.resolve("notas.txt");
        Files.writeString(existente, "arquivo que já estava lá\n");
        System.out.println("Pasta temporária: " + pasta + " (já contém notas.txt, que não vai mudar)");
        Path novo = pasta.resolve("relatorio.csv");
        Thread provocador = new Thread(() -> {
            try {
                Thread.sleep(500);
                Files.writeString(novo, "produto,quantidade\n");
                System.out.println("  (provocador) criei relatorio.csv");
                Thread.sleep(900);
                Files.writeString(novo, "produto,quantidade\ncaderno,12\ncaneta,30\n");
                System.out.println("  (provocador) acrescentei duas linhas a relatorio.csv");
                Thread.sleep(900);
                Files.delete(novo);
                System.out.println("  (provocador) apaguei relatorio.csv");
            } catch (IOException | InterruptedException e) {
                System.out.println("  (provocador) falhou: " + e.getMessage());
            }
        });
        provocador.setDaemon(true);                         // não impede o programa de terminar
        provocador.start();
        new MonitorArquivos(pasta, false).observar(300, 10);
        provocador.join();
        Files.deleteIfExists(existente);
        Files.deleteIfExists(pasta);
        System.out.println("Pasta temporária removida: " + pasta);
    }

    static String bytes(long n) {
        return n < 1024 ? n + " B" : String.format("%.1f KB", n / 1024.0);
    }

    static String hora() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo);
        System.exit(1);
    }

    static void uso() {
        System.out.println("""
            Uso: java MonitorArquivos.java <pasta> [intervalo_ms] [--recursivo] [--rodadas N]
              intervalo_ms   tempo entre consultas, padrão 1000 (mínimo 100)
              --recursivo    observa também as subpastas
              --rodadas N    para depois de N consultas (sem isso, roda até Ctrl+C)
            Exemplo: java MonitorArquivos.java ./downloads 500 --rodadas 60
            Demonstração: java MonitorArquivos.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Ignore padrões como *.tmp e a pasta .git, lidos de um arquivo .monitorignore.
 *  2. Detecte renomeações: um "removido" e um "criado" na mesma rodada com o mesmo tamanho.
 *  3. Compare com java.nio.file.WatchService, que avisa sem consultar; meça o uso de CPU dos dois.
 */
