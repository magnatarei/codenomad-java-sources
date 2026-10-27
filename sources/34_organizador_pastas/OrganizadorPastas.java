/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  34. Organizador de pasta por tipo
 * ------------------------------------------------------------
 * O que faz: olha a extensão de cada arquivo de uma pasta e o move para a
 *            subpasta da sua categoria (imagens, documentos, planilhas,
 *            áudio, vídeo, código ou outros; as pastas são criadas sem acento).
 *            Mostra a prévia, aplica com --aplicar, imprime o relatório e
 *            desfaz tudo com --desfazer.
 * Como usar: java OrganizadorPastas.java ~/Downloads
 *            java OrganizadorPastas.java ~/Downloads --aplicar
 *            java OrganizadorPastas.java ~/Downloads --desfazer
 *            java OrganizadorPastas.java --demo
 * O que você aprende: Map de extensão para categoria, Files.list, move,
 *            createDirectories e size, um arquivo de log que torna a
 *            operação reversível, Files.walk para desenhar a árvore e
 *            groupingBy com contagem e soma ao mesmo tempo.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public class OrganizadorPastas {

    static final String LOG = ".organizador_desfazer.txt";
    static final Map<String, String> CATEGORIA = new HashMap<>();
    static {
        // Cada extensão aponta para a sua categoria; o que não estiver aqui vai para "outros".
        Map.of("imagens", "jpg jpeg png gif webp svg bmp heic",
               "documentos", "pdf doc docx txt md odt rtf epub",
               "planilhas", "xls xlsx csv ods",
               "audio", "mp3 wav flac ogg m4a aac",
               "video", "mp4 mkv avi mov webm",
               "codigo", "java py js ts html css c cpp go rs sh json xml")
                .forEach((cat, exts) -> { for (String e : exts.split(" ")) CATEGORIA.put(e, cat); });
    }

    record Movimento(Path origem, Path destino) {}

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try { System.out.print(executar(args)); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
    }

    static void demo() throws IOException {
        Path pasta = Files.createTempDirectory("codenomad_organizar_");
        List<String> nomes = List.of("foto_praia.jpg", "captura.PNG", "logo.svg", "contrato.pdf", "notas de aula.txt", "orcamento.xlsx",
                "vendas.csv", "musica.mp3", "aula_01.mp4", "Main.java", "script.py", "backup.xyz", "sem_extensao", "notas de aula (2).txt");
        for (String nome : nomes) Files.writeString(pasta.resolve(nome), "x".repeat(nome.length() * 100), StandardCharsets.UTF_8);
        Files.createDirectories(pasta.resolve("documentos")); // subpastas nunca são movidas...
        Files.writeString(pasta.resolve("documentos/contrato.pdf"), "versão antiga", StandardCharsets.UTF_8); // ...e nada é sobrescrito
        System.out.println("=== Demonstração em " + pasta + " ===");
        for (String modo : new String[]{"", "--aplicar", "--desfazer"}) {
            System.out.println("\n> OrganizadorPastas <pasta> " + modo);
            System.out.print(executar(modo.isEmpty() ? new String[]{pasta.toString()} : new String[]{pasta.toString(), modo}));
            if (modo.equals("--aplicar")) System.out.print(arvore(pasta));
        }
        System.out.println("\nDepois de desfazer:");
        System.out.print(arvore(pasta));
        System.out.println("\n> OrganizadorPastas <pasta> --desfazer (de novo)");
        try { executar(new String[]{pasta.toString(), "--desfazer"}); }
        catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        try (Stream<Path> tudo = Files.walk(pasta)) {
            for (Path p : tudo.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    static String executar(String[] args) throws IOException {
        Path pasta = Path.of(args[0]);
        if (!Files.isDirectory(pasta)) throw new IllegalArgumentException("a pasta '" + args[0] + "' não existe");
        String modo = args.length > 1 ? args[1] : "";
        if (!List.of("", "--aplicar", "--desfazer").contains(modo)) throw new IllegalArgumentException("opção desconhecida '" + modo + "'; use --aplicar ou --desfazer");
        if (modo.equals("--desfazer")) return desfazer(pasta);
        List<Movimento> plano = planejar(pasta);
        if (plano.isEmpty()) return "Nada para organizar em " + pasta + "\n";
        StringBuilder sb = new StringBuilder(modo.isEmpty() ? "Prévia (use --aplicar para mover de verdade):\n" : "Movendo:\n");
        for (Movimento m : plano) {
            sb.append(String.format("  %-24s -> %s%n", m.origem().getFileName(), pasta.relativize(m.destino())));
        }
        if (modo.equals("--aplicar")) {
            List<String> log = new ArrayList<>();
            for (Movimento m : plano) {
                Files.createDirectories(m.destino().getParent());
                Files.move(m.origem(), m.destino());
                // Guardamos o caminho novo e o nome original, separados por tabulação, para poder desfazer.
                log.add(pasta.relativize(m.destino()) + "\t" + m.origem().getFileName());
            }
            Files.write(pasta.resolve(LOG), log, StandardCharsets.UTF_8);
            sb.append("Registro gravado em ").append(LOG).append(" (use --desfazer para voltar atrás).\n");
        }
        return sb.append(relatorio(plano)).toString();
    }

    /** Decide o destino de cada arquivo sem tocar em nada; é a prévia e também o plano do --aplicar. */
    static List<Movimento> planejar(Path pasta) throws IOException {
        List<Movimento> plano = new ArrayList<>();
        Set<Path> reservados = new HashSet<>();
        try (Stream<Path> fluxo = Files.list(pasta)) {
            for (Path arquivo : fluxo.filter(Files::isRegularFile).sorted().toList()) {
                String nome = arquivo.getFileName().toString();
                if (nome.equals(LOG)) continue;
                Path destino = pasta.resolve(categoria(nome)).resolve(nome);
                // Nunca sobrescrever: se já houver um arquivo igual na categoria (ou outro do plano for
                // para lá), o nome ganha um sufixo (2), (3)...
                int ponto = nome.lastIndexOf('.');
                String base = ponto > 0 ? nome.substring(0, ponto) : nome, ext = ponto > 0 ? nome.substring(ponto) : "";
                for (int n = 2; Files.exists(destino) || reservados.contains(destino); n++) {
                    destino = destino.resolveSibling(base + " (" + n + ")" + ext);
                }
                reservados.add(destino);
                plano.add(new Movimento(arquivo, destino));
            }
        }
        return plano;
    }

    static String categoria(String nome) {
        int ponto = nome.lastIndexOf('.');
        String ext = ponto <= 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
        return CATEGORIA.getOrDefault(ext, "outros");
    }

    /** Lê o registro e move cada arquivo de volta para a raiz, depois apaga as pastas que ficaram vazias. */
    static String desfazer(Path pasta) throws IOException {
        Path log = pasta.resolve(LOG);
        if (!Files.exists(log)) throw new IllegalArgumentException("não há registro " + LOG + " em " + pasta + "; nada para desfazer");
        StringBuilder sb = new StringBuilder("Desfazendo:\n");
        int voltaram = 0;
        for (String linha : Files.readAllLines(log, StandardCharsets.UTF_8)) {
            String[] partes = linha.split("\t", 2);
            Path atual = pasta.resolve(partes[0]), original = pasta.resolve(partes[1]);
            if (!Files.exists(atual)) { sb.append("  (sumiu) ").append(partes[0]).append('\n'); continue; }
            if (Files.exists(original)) { sb.append("  (já existe na raiz, mantido) ").append(partes[0]).append('\n'); continue; }
            Files.move(atual, original);
            voltaram++;
            sb.append(String.format("  %-32s -> %s%n", partes[0], partes[1]));
        }
        Files.delete(log);
        try (Stream<Path> fluxo = Files.list(pasta)) {
            for (Path sub : fluxo.filter(Files::isDirectory).toList()) {
                try (Stream<Path> dentro = Files.list(sub)) {
                    if (dentro.findAny().isEmpty()) Files.delete(sub); // só apaga pasta vazia
                }
            }
        }
        return sb.append(voltaram).append(" arquivo(s) de volta na raiz.\n").toString();
    }

    /** Conta arquivos e soma bytes por categoria. TreeMap deixa as categorias em ordem alfabética. */
    static String relatorio(List<Movimento> plano) throws IOException {
        Map<String, long[]> porCategoria = new TreeMap<>();
        for (Movimento m : plano) {
            long[] acumulado = porCategoria.computeIfAbsent(m.destino().getParent().getFileName().toString(), k -> new long[2]);
            acumulado[0]++;
            acumulado[1] += Files.size(Files.exists(m.destino()) ? m.destino() : m.origem());
        }
        StringBuilder sb = new StringBuilder("Relatório por categoria:\n");
        porCategoria.forEach((cat, v) -> sb.append(String.format("  %-12s %2d arquivo(s)  %s%n", cat, v[0], tamanho(v[1]))));
        return sb.toString();
    }

    static String tamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.forLanguageTag("pt-BR"), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / (1024.0 * 1024));
    }

    /** Desenha a pasta como árvore; a profundidade vem da quantidade de partes do caminho relativo. */
    static String arvore(Path pasta) throws IOException {
        StringBuilder sb = new StringBuilder(pasta.getFileName() + "/\n");
        try (Stream<Path> fluxo = Files.walk(pasta)) {
            for (Path p : fluxo.skip(1).sorted().toList()) {
                Path rel = pasta.relativize(p);
                sb.append("  ".repeat(rel.getNameCount())).append(rel.getFileName()).append(Files.isDirectory(p) ? "/" : "").append('\n');
            }
        }
        return sb.toString();
    }

    static void uso() {
        System.out.println("""
            Uso: java OrganizadorPastas.java <pasta> [--aplicar | --desfazer]
              sem opção     mostra a prévia: para onde cada arquivo iria
              --aplicar     move os arquivos para imagens/, documentos/, planilhas/, audio/, video/, codigo/ ou outros/
              --desfazer    devolve cada arquivo ao lugar de origem usando o registro gravado
            Subpastas que já existem não são tocadas. Nenhum arquivo é sobrescrito.
            Demonstração: java OrganizadorPastas.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Permita configurar as categorias por um arquivo de texto "extensão=categoria" na pasta do usuário.
 *  2. Organize por ano e mês da data de modificação (Files.getLastModifiedTime) em vez de por tipo.
 *  3. Aceite --recursivo para organizar também o que está dentro das subpastas, sem reorganizar as categorias.
 */
