/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  43. Informações do sistema
 * ------------------------------------------------------------
 * O que faz: imprime um retrato da máquina onde está rodando: sistema
 *            operacional, versão do Java, usuário, pasta atual, CPUs, memória
 *            da JVM, variáveis de ambiente (filtradas, com segredos ocultos),
 *            espaço em disco da pasta atual, fuso horário e idioma.
 * Como usar: java InfoSistema.java --tudo
 *            java InfoSistema.java --secao disco --secao memoria
 *            java InfoSistema.java --ambiente JAVA
 *            java InfoSistema.java --demo
 * O que você aprende: System.getProperty e System.getenv, Runtime, os beans
 *            de java.lang.management, FileStore para disco, formatação de
 *            bytes, TreeMap para listar em ordem e switch com várias opções.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class InfoSistema {

    static final List<String> SECOES = List.of("sistema", "java", "usuario", "cpu", "memoria", "ambiente", "disco", "hora");
    /** Pedaços de nome que indicam segredo: o valor sai mascarado, nunca na tela. */
    static final List<String> SENSIVEIS = List.of("KEY", "TOKEN", "SECRET", "SENHA", "PASSWORD", "PASS", "CREDENTIAL");

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        var pedidas = new ArrayList<String>();
        String filtroAmbiente = "";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--demo", "--tudo" -> pedidas.addAll(SECOES);
                case "--secao" -> {
                    if (i + 1 >= args.length || !SECOES.contains(args[i + 1])) {
                        falhar("--secao precisa de um destes nomes: " + String.join(", ", SECOES));
                    }
                    pedidas.add(args[++i]);
                }
                case "--ambiente" -> {
                    if (i + 1 >= args.length) falhar("--ambiente precisa de um texto para filtrar, ex.: --ambiente JAVA");
                    filtroAmbiente = args[++i].toUpperCase();
                    pedidas.add("ambiente");
                }
                default -> falhar("não conheço a opção '" + args[i] + "'");
            }
        }
        System.out.println("=== Informações do sistema (" + agora() + ") ===");
        for (String secao : SECOES) {
            if (!pedidas.contains(secao)) continue;
            System.out.println();
            System.out.println("[" + secao + "]");
            // Cada seção é independente: se uma falhar, as outras continuam.
            try {
                mostrar(secao, filtroAmbiente);
            } catch (Exception e) {
                System.out.println("  não consegui obter: " + e.getMessage());
            }
        }
    }

    static void mostrar(String secao, String filtroAmbiente) throws IOException {
        Runtime rt = Runtime.getRuntime();
        switch (secao) {
            case "sistema" -> {
                linha("Sistema", prop("os.name") + " " + prop("os.version") + " (" + prop("os.arch") + ")");
                linha("Nome da máquina", nomeDaMaquina());
                linha("Separador de pastas", prop("file.separator") + "   fim de linha: " + prop("line.separator").replace("\r", "\\r").replace("\n", "\\n"));
            }
            case "java" -> {
                linha("Versão", prop("java.version") + " (" + prop("java.vendor") + ")");
                linha("Runtime", Runtime.version().toString());
                linha("Instalado em", prop("java.home"));
                linha("Charset padrão", Charset.defaultCharset().name());
                var jvm = ManagementFactory.getRuntimeMXBean();
                linha("JVM ligada há", duracao(Duration.ofMillis(jvm.getUptime())) + " (pid " + jvm.getPid() + ")");
            }
            case "usuario" -> {
                linha("Usuário", prop("user.name"));
                linha("Pasta pessoal", prop("user.home"));
                linha("Pasta atual", Path.of("").toAbsolutePath().toString());
                linha("Idioma", Locale.getDefault().getDisplayName() + " (" + Locale.getDefault().toLanguageTag() + ")");
            }
            case "cpu" -> {
                linha("Processadores disponíveis", String.valueOf(rt.availableProcessors()));
                var so = ManagementFactory.getOperatingSystemMXBean();
                // Em alguns sistemas (Windows, por exemplo) a carga média não é exposta e vem -1.
                double carga = so.getSystemLoadAverage();
                linha("Carga média (1 min)", carga < 0 ? "não exposta neste sistema" : String.format("%.2f", carga));
                linha("Threads vivas na JVM", String.valueOf(ManagementFactory.getThreadMXBean().getThreadCount()));
            }
            case "memoria" -> {
                // A biblioteca padrão só expõe a memória da própria JVM, não a RAM física inteira.
                long total = rt.totalMemory(), livre = rt.freeMemory(), maxima = rt.maxMemory();
                linha("Heap em uso", bytes(total - livre));
                linha("Heap reservado", bytes(total) + " (máximo " + bytes(maxima) + ")");
                var naoHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
                linha("Fora do heap", bytes(naoHeap.getUsed()) + " (classes, código compilado)");
                linha("Observação", "a RAM física não é exposta por java.*; use ferramentas do sistema para isso");
            }
            case "ambiente" -> ambiente(filtroAmbiente);
            case "disco" -> {
                Path atual = Path.of("").toAbsolutePath();
                FileStore disco = Files.getFileStore(atual);
                long totalD = disco.getTotalSpace(), usavel = disco.getUsableSpace();
                linha("Volume", disco.name().isEmpty() ? disco.toString() : disco.name() + " (" + disco.type() + ")");
                linha("Total", bytes(totalD));
                linha("Livre para você", bytes(usavel) + String.format(" (%.1f%% livre)", 100.0 * usavel / totalD));
                linha("Em uso", bytes(totalD - disco.getUnallocatedSpace()));
            }
            case "hora" -> {
                ZoneId fuso = ZoneId.systemDefault();
                linha("Fuso horário", fuso.getId() + " (deslocamento " + ZonedDateTime.now(fuso).getOffset() + ")");
                linha("Agora", agora());
                linha("Em UTC", ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            }
            default -> throw new IllegalArgumentException("seção desconhecida " + secao);
        }
    }

    /** Lista as variáveis em ordem alfabética; segredos aparecem como ******. */
    static void ambiente(String filtro) {
        var ordenadas = new TreeMap<String, String>(System.getenv());    // TreeMap ordena pela chave
        int mostradas = 0;
        for (var entrada : ordenadas.entrySet()) {
            String nome = entrada.getKey();
            if (!nome.toUpperCase().contains(filtro)) continue;
            String valor = entrada.getValue();
            boolean segredo = SENSIVEIS.stream().anyMatch(s -> nome.toUpperCase().contains(s));
            if (segredo) valor = "******";
            else if (valor.length() > 70) valor = valor.substring(0, 67) + "...";
            linha(nome, valor);
            mostradas++;
        }
        linha("Resumo", mostradas + " de " + ordenadas.size() + " variáveis" + (filtro.isEmpty() ? "" : " contendo '" + filtro + "'"));
    }

    static String nomeDaMaquina() {
        // Primeiro as variáveis de ambiente (instantâneas); só então a consulta ao sistema.
        String nome = System.getenv().getOrDefault("COMPUTERNAME", System.getenv().getOrDefault("HOSTNAME", ""));
        if (!nome.isEmpty()) return nome;
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            return "desconhecido";
        }
    }

    static String prop(String chave) {
        return System.getProperty(chave, "desconhecido");
    }

    static void linha(String rotulo, String valor) {
        System.out.printf("  %-26s %s%n", rotulo + ":", valor);
    }

    static String agora() {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /** 1536 vira "1,5 KB"; usa Locale pt-BR para sair com vírgula. */
    static String bytes(long n) {
        String[] unidades = {"B", "KB", "MB", "GB", "TB"};
        double valor = n;
        int i = 0;
        while (valor >= 1024 && i < unidades.length - 1) { valor /= 1024; i++; }
        return String.format(Locale.forLanguageTag("pt-BR"), i == 0 ? "%.0f %s" : "%.1f %s", valor, unidades[i]);
    }

    static String duracao(Duration d) {
        return String.format("%dh %02dmin %02ds", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo);
        System.exit(1);
    }

    static void uso() {
        System.out.println("""
            Uso: java InfoSistema.java --tudo
                 java InfoSistema.java --secao <nome> [--secao <nome> ...]
                 java InfoSistema.java --ambiente <texto>      (variáveis cujo nome contém o texto)
            Seções: sistema, java, usuario, cpu, memoria, ambiente, disco, hora
            Demonstração: java InfoSistema.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Adicione --json para imprimir as mesmas informações num objeto JSON.
 *  2. Mostre o espaço de todos os volumes com FileSystems.getDefault().getFileStores().
 *  3. Grave o relatório num arquivo com a data no nome e compare dois relatórios de dias diferentes.
 */
