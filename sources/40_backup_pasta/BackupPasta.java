/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  40. Backup de pasta com data
 * ------------------------------------------------------------
 * O que faz: copia uma pasta inteira (com subpastas) para um destino numa
 *            subpasta carimbada com data e hora, pulando os padrões que você
 *            mandar ignorar. Mostra o que foi copiado, acha o backup anterior
 *            no mesmo destino e lista o que é novo, o que mudou e o que sumiu.
 * Como usar: java BackupPasta.java ./projeto ./backups
 *            java BackupPasta.java ./projeto ./backups --ignorar "*.log,tmp,.git"
 *            java BackupPasta.java --demo
 * O que você aprende: Files.walk, copy com atributos, createDirectories,
 *            PathMatcher com padrões glob, Files.mismatch para comparar
 *            conteúdo (Java 12+), relativize, TreeMap para comparar dois
 *            conjuntos de arquivos e DateTimeFormatter para carimbar nomes.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

public class BackupPasta {

    static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** O resultado de um backup: onde ficou e o que foi copiado (caminho relativo para tamanho). */
    record Resultado(Path pasta, Map<String, Long> copiados, List<String> ignorados) {
        long bytes() { return copiados.values().stream().mapToLong(Long::longValue).sum(); }
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            if (args.length != 2 && args.length != 4) throw new IllegalArgumentException("use: <origem> <destino> [--ignorar \"padrão1,padrão2\"]");
            List<String> ignorar = List.of();
            if (args.length == 4) {
                if (!args[2].equals("--ignorar")) throw new IllegalArgumentException("opção desconhecida '" + args[2] + "'; a única opção é --ignorar");
                ignorar = List.of(args[3].split(","));
            }
            System.out.print(executar(Path.of(args[0]), Path.of(args[1]), ignorar, LocalDateTime.now()));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }

    static void demo() throws IOException {
        Path raiz = Files.createTempDirectory("codenomad_backup_");
        Path origem = raiz.resolve("projeto"), destino = raiz.resolve("backups");
        Files.createDirectories(origem.resolve("src"));
        Files.createDirectories(origem.resolve("tmp"));
        Files.writeString(origem.resolve("LEIAME.md"), "# Projeto de exemplo\n", StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("src/Main.java"), "public class Main {}\n", StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("src/Util.java"), "class Util {}\n", StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("notas.txt"), "primeira versão das notas\n", StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("saida.log"), "log enorme que não interessa\n".repeat(50), StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("tmp/cache.bin"), "x".repeat(1000), StandardCharsets.UTF_8);
        System.out.println("=== Demonstração em " + raiz + " ===");
        List<String> ignorar = List.of("*.log", "tmp");
        System.out.println("\n> BackupPasta projeto backups --ignorar \"*.log,tmp\"   (primeiro backup)");
        System.out.print(executar(origem, destino, ignorar, LocalDateTime.of(2026, 10, 25, 8, 0, 0)));

        System.out.println("\n... um dia depois: notas.txt mudou, Util.java foi apagado e config.json apareceu ...");
        Files.writeString(origem.resolve("notas.txt"), "segunda versão das notas, bem maior\n", StandardCharsets.UTF_8);
        Files.delete(origem.resolve("src/Util.java"));
        Files.writeString(origem.resolve("config.json"), "{\"tema\": \"escuro\"}\n", StandardCharsets.UTF_8);
        System.out.println("\n> BackupPasta projeto backups --ignorar \"*.log,tmp\"   (segundo backup)");
        System.out.print(executar(origem, destino, ignorar, LocalDateTime.of(2026, 10, 26, 8, 0, 0)));

        System.out.println("\n> BackupPasta projeto backups   (mesmo carimbo de novo)");
        try { executar(origem, destino, ignorar, LocalDateTime.of(2026, 10, 26, 8, 0, 0)); }
        catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        System.out.println("\n> BackupPasta projeto projeto/backups");
        try { executar(origem, origem.resolve("backups"), ignorar, LocalDateTime.now()); }
        catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        try (Stream<Path> tudo = Files.walk(raiz)) {
            for (Path p : tudo.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    static String executar(Path origem, Path destino, List<String> ignorar, LocalDateTime quando) throws IOException {
        if (!Files.isDirectory(origem)) throw new IllegalArgumentException("a pasta de origem '" + origem + "' não existe");
        Path origemAbs = origem.toAbsolutePath().normalize(), destinoAbs = destino.toAbsolutePath().normalize();
        if (destinoAbs.startsWith(origemAbs)) throw new IllegalArgumentException("o destino não pode ficar dentro da origem, senão o backup copiaria a si mesmo");
        String prefixo = origemAbs.getFileName() + "_";
        Path pasta = destinoAbs.resolve(prefixo + CARIMBO.format(quando));
        if (Files.exists(pasta)) throw new IllegalArgumentException("já existe um backup chamado " + pasta.getFileName() + "; espere um segundo e tente de novo");
        Optional<Path> anterior = ultimoBackup(destinoAbs, prefixo);
        Resultado r = copiar(origemAbs, pasta, ignorar);

        StringBuilder sb = new StringBuilder("Backup criado em " + pasta + "\n");
        r.copiados().forEach((rel, tamanho) -> sb.append(String.format("  copiado   %-28s %s%n", rel, tamanhoLegivel(tamanho))));
        r.ignorados().forEach(rel -> sb.append(String.format("  ignorado  %s%n", rel)));
        sb.append(String.format("%d arquivo(s), %s no total, %d ignorado(s).%n", r.copiados().size(), tamanhoLegivel(r.bytes()), r.ignorados().size()));
        if (anterior.isEmpty()) return sb.append("Nenhum backup anterior em " + destinoAbs + " para comparar.\n").toString();
        return sb.append(comparar(anterior.get(), pasta)).toString();
    }

    /** Percorre a origem e copia arquivo por arquivo, recriando as subpastas no destino. */
    static Resultado copiar(Path origem, Path destino, List<String> ignorar) throws IOException {
        List<PathMatcher> padroes = ignorar.stream().map(String::strip).filter(p -> !p.isEmpty())
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p)).toList();
        Map<String, Long> copiados = new TreeMap<>();
        List<String> ignorados = new ArrayList<>();
        try (Stream<Path> fluxo = Files.walk(origem)) {
            for (Path arquivo : fluxo.filter(Files::isRegularFile).sorted().toList()) {
                Path rel = origem.relativize(arquivo);
                // Um padrão vale para qualquer pedaço do caminho: "tmp" ignora a pasta inteira, "*.log" ignora pelo nome.
                boolean ignorado = false;
                for (Path pedaco : rel) for (PathMatcher m : padroes) ignorado |= m.matches(pedaco);
                if (ignorado) { ignorados.add(chave(rel)); continue; }
                Path alvo = destino.resolve(rel);
                Files.createDirectories(alvo.getParent());
                Files.copy(arquivo, alvo, StandardCopyOption.COPY_ATTRIBUTES); // mantém a data de modificação original
                copiados.put(chave(rel), Files.size(arquivo));
            }
        }
        return new Resultado(destino, copiados, ignorados);
    }

    /** O backup mais recente é o de maior nome: o carimbo ano-mês-dia_hora foi escolhido para ordenar como texto. */
    static Optional<Path> ultimoBackup(Path destino, String prefixo) throws IOException {
        if (!Files.isDirectory(destino)) return Optional.empty();
        try (Stream<Path> fluxo = Files.list(destino)) {
            return fluxo.filter(Files::isDirectory).filter(p -> p.getFileName().toString().startsWith(prefixo)).max(Comparator.naturalOrder());
        }
    }

    /** Compara dois backups pelos caminhos relativos: só num lado é novo ou removido; nos dois, confere o conteúdo. */
    static String comparar(Path antigo, Path novo) throws IOException {
        Map<String, Path> a = listar(antigo), b = listar(novo);
        StringBuilder sb = new StringBuilder("Comparado com o backup anterior " + antigo.getFileName() + ":\n");
        int mudancas = 0;
        for (String rel : b.keySet()) {
            if (!a.containsKey(rel)) { sb.append("  novo        ").append(rel).append('\n'); mudancas++; }
            else if (Files.mismatch(a.get(rel), b.get(rel)) != -1) { // -1 significa conteúdo idêntico
                sb.append(String.format("  modificado  %s (%s -> %s)%n", rel, tamanhoLegivel(Files.size(a.get(rel))), tamanhoLegivel(Files.size(b.get(rel)))));
                mudancas++;
            }
        }
        for (String rel : a.keySet()) if (!b.containsKey(rel)) { sb.append("  removido    ").append(rel).append('\n'); mudancas++; }
        return sb.append(mudancas == 0 ? "  nada mudou desde o backup anterior\n" : "  " + mudancas + " mudança(s)\n").toString();
    }

    static Map<String, Path> listar(Path pasta) throws IOException {
        Map<String, Path> mapa = new TreeMap<>();
        try (Stream<Path> fluxo = Files.walk(pasta)) {
            fluxo.filter(Files::isRegularFile).forEach(p -> mapa.put(chave(pasta.relativize(p)), p));
        }
        return mapa;
    }

    /** Sempre com barra normal, para o mesmo backup ser comparável entre Windows, Linux e macOS. */
    static String chave(Path rel) {
        return rel.toString().replace('\\', '/');
    }

    static String tamanhoLegivel(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.forLanguageTag("pt-BR"), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / (1024.0 * 1024));
    }

    static void uso() {
        System.out.println("""
            Uso: java BackupPasta.java <origem> <destino> [--ignorar "padrão1,padrão2"]
              Cria <destino>/<origem>_aaaa-mm-dd_hh-mm-ss/ com uma cópia da origem.
              --ignorar aceita padrões glob separados por vírgula: "*.log,tmp,.git,*.bak"
              Se houver backup anterior no destino, mostra o que é novo, o que mudou e o que foi removido.
            Demonstração: java BackupPasta.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Adicione --manter N, que apaga os backups mais antigos deixando só os N mais recentes.
 *  2. Compacte o backup num .zip com java.util.zip.ZipOutputStream em vez de copiar pasta por pasta.
 *  3. Faça um backup incremental: copie só o que mudou e crie links para os arquivos iguais ao anterior.
 */
