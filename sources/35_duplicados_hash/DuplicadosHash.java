/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  35. Detector de arquivos duplicados
 * ------------------------------------------------------------
 * O que faz: percorre uma pasta inteira (com subpastas), agrupa os arquivos
 *            primeiro por tamanho e depois por SHA-256, lista os grupos de
 *            duplicados e calcula quanto espaço seria liberado mantendo
 *            uma cópia de cada. Pode gravar um relatório em texto.
 * Como usar: java DuplicadosHash.java ~/Fotos
 *            java DuplicadosHash.java ~/Fotos --min-bytes 1024
 *            java DuplicadosHash.java ~/Fotos --relatorio duplicados.txt
 *            java DuplicadosHash.java --demo
 * O que você aprende: Files.walk com try-with-resources, MessageDigest para
 *            calcular SHA-256 lendo em blocos, HexFormat (Java 17), a
 *            estratégia "tamanho antes de hash" para não ler o disco à toa,
 *            groupingBy e formatação de tamanhos legíveis.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicadosHash {

    record Grupo(String hash, long tamanho, List<Path> arquivos) {
        long desperdicio() { return tamanho * (arquivos.size() - 1); }
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try { System.out.print(executar(args)); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
    }

    static void demo() throws IOException {
        Path pasta = Files.createTempDirectory("codenomad_duplicados_");
        Files.createDirectories(pasta.resolve("backup/antigo"));
        Files.createDirectories(pasta.resolve("fotos"));
        String praia = "foto da praia ".repeat(300), serra = "foto da serra ".repeat(300);
        Map<String, String> conteudo = new LinkedHashMap<>();
        conteudo.put("fotos/praia.jpg", praia);
        conteudo.put("backup/praia.jpg", praia);
        conteudo.put("backup/antigo/praia_copia.jpg", praia);
        conteudo.put("fotos/serra.jpg", serra);
        conteudo.put("backup/serra (1).jpg", serra);
        conteudo.put("fotos/lago.jpg", "foto do lago! ".repeat(300)); // mesmo tamanho da serra, conteúdo diferente
        conteudo.put("notas.txt", "anotações da aula de Java sobre hashes");
        conteudo.put("backup/notas.txt", "anotações da aula de Java sobre hashes");
        conteudo.put("unico.txt", "este arquivo não tem cópia");
        conteudo.put("vazio.txt", "");
        for (var e : conteudo.entrySet()) Files.writeString(pasta.resolve(e.getKey()), e.getValue(), StandardCharsets.UTF_8);
        System.out.println("=== Demonstração em " + pasta + " ===\n");
        Path relatorio = Files.createTempFile("codenomad_duplicados_", "_relatorio.txt"); // fora da pasta analisada
        System.out.print(executar(new String[]{pasta.toString(), "--relatorio", relatorio.toString()}));
        System.out.println("\nPrimeiras linhas do relatório gravado:");
        Files.readAllLines(relatorio, StandardCharsets.UTF_8).stream().limit(4).forEach(l -> System.out.println("  | " + l));
        System.out.println("\n> DuplicadosHash <pasta> --min-bytes 1000  (ignora os arquivos pequenos)");
        System.out.print(executar(new String[]{pasta.toString(), "--min-bytes", "1000"}));
        System.out.println("\n> DuplicadosHash <pasta> --min-bytes muitos");
        try { executar(new String[]{pasta.toString(), "--min-bytes", "muitos"}); }
        catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        Files.delete(relatorio);
        try (Stream<Path> tudo = Files.walk(pasta)) {
            for (Path p : tudo.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    static String executar(String[] args) throws IOException {
        Path pasta = Path.of(args[0]);
        if (!Files.isDirectory(pasta)) throw new IllegalArgumentException("a pasta '" + args[0] + "' não existe");
        long minimo = 1;
        Path relatorio = null;
        for (int i = 1; i < args.length; i += 2) {
            if (i + 1 >= args.length) throw new IllegalArgumentException("a opção " + args[i] + " precisa de um valor");
            switch (args[i]) {
                case "--min-bytes" -> {
                    try { minimo = Math.max(1, Long.parseLong(args[i + 1])); }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("--min-bytes precisa de um número inteiro, recebi '" + args[i + 1] + "'"); }
                }
                case "--relatorio" -> relatorio = Path.of(args[i + 1]);
                default -> throw new IllegalArgumentException("opção desconhecida '" + args[i] + "'; use --min-bytes ou --relatorio");
            }
        }
        String texto = analisar(pasta, minimo);
        if (relatorio != null) {
            Files.writeString(relatorio, texto, StandardCharsets.UTF_8);
            texto += "Relatório gravado em " + relatorio + "\n";
        }
        return texto;
    }

    static String analisar(Path pasta, long minimo) throws IOException {
        // 1) Lista todos os arquivos (vazios ficam de fora: seriam todos "iguais" entre si).
        List<Path> arquivos;
        try (Stream<Path> fluxo = Files.walk(pasta)) {
            arquivos = fluxo.filter(Files::isRegularFile).sorted().toList();
        }
        Map<Long, List<Path>> porTamanho = new TreeMap<>();
        long totalBytes = 0;
        for (Path a : arquivos) {
            long tamanho = Files.size(a);
            totalBytes += tamanho;
            if (tamanho >= minimo) porTamanho.computeIfAbsent(tamanho, k -> new ArrayList<>()).add(a);
        }
        // 2) Só vale a pena calcular o hash de quem tem o tamanho repetido: tamanhos diferentes nunca são iguais.
        List<Grupo> grupos = new ArrayList<>();
        int comparados = 0;
        for (var entrada : porTamanho.entrySet()) {
            if (entrada.getValue().size() < 2) continue;
            comparados += entrada.getValue().size();
            Map<String, List<Path>> porHash = new LinkedHashMap<>();
            for (Path a : entrada.getValue()) porHash.computeIfAbsent(sha256(a), k -> new ArrayList<>()).add(a);
            porHash.forEach((hash, lista) -> { if (lista.size() > 1) grupos.add(new Grupo(hash, entrada.getKey(), lista)); });
        }
        grupos.sort(Comparator.comparingLong(Grupo::desperdicio).reversed());
        // 3) Monta o texto do resultado.
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Analisados %d arquivos (%s); %d tinham tamanho repetido e foram comparados por SHA-256.%n",
                arquivos.size(), tamanhoLegivel(totalBytes), comparados));
        if (grupos.isEmpty()) return sb.append("Nenhum duplicado encontrado.\n").toString();
        int n = 1;
        for (Grupo g : grupos) {
            sb.append(String.format("Grupo %d: %d arquivos de %s cada (sha256 %s...)%n", n++, g.arquivos().size(), tamanhoLegivel(g.tamanho()), g.hash().substring(0, 12)));
            for (Path a : g.arquivos()) sb.append("  ").append(pasta.relativize(a)).append('\n');
        }
        long liberavel = grupos.stream().mapToLong(Grupo::desperdicio).sum();
        return sb.append(String.format("%d grupo(s) de duplicados. Espaço que pode ser liberado mantendo uma cópia de cada: %s.%n",
                grupos.size(), tamanhoLegivel(liberavel))).toString();
    }

    /** Lê o arquivo em blocos de 64 KB e alimenta o resumo; funciona igual para 1 KB ou 10 GB. */
    static String sha256(Path arquivo) throws IOException {
        try (InputStream in = Files.newInputStream(arquivo)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bloco = new byte[64 * 1024];
            for (int lidos = in.read(bloco); lidos > 0; lidos = in.read(bloco)) digest.update(bloco, 0, lidos);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 faz parte de todo JDK; isto não deveria acontecer", e);
        }
    }

    static String tamanhoLegivel(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.forLanguageTag("pt-BR"), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / (1024.0 * 1024));
    }

    static void uso() {
        System.out.println("""
            Uso: java DuplicadosHash.java <pasta> [--min-bytes N] [--relatorio arquivo.txt]
              --min-bytes N          ignora arquivos menores que N bytes (padrão: 1, ou seja, só os vazios)
              --relatorio arquivo    grava o mesmo resultado num arquivo de texto
            Nada é apagado: o programa só mostra o que está repetido e quanto espaço isso ocupa.
            Demonstração: java DuplicadosHash.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Antes do SHA-256 completo, compare só os primeiros 4 KB dos arquivos de mesmo tamanho e meça o ganho.
 *  2. Adicione --apagar, que remove as cópias e mantém a de caminho mais curto, pedindo confirmação.
 *  3. Mostre uma barra de progresso simples contando os bytes já lidos sobre o total a comparar.
 */
