/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  30. Grafos: BFS e DFS num labirinto
 * ------------------------------------------------------------
 * O que faz: lê um labirinto em texto (# parede, . corredor, E entrada,
 *            S saída), monta um grafo com as células livres, acha o caminho
 *            mais curto com busca em largura (BFS), explora com busca em
 *            profundidade (DFS) e desenha o caminho encontrado no labirinto.
 * Como usar: java GrafoBfsDfs.java labirinto.txt
 *            java GrafoBfsDfs.java --linhas "E..#...." ".#.#.##." ".#...#.S"
 *            java GrafoBfsDfs.java --demo
 * O que você aprende: grafo como mapa de adjacência (Map<Celula, List<Celula>>),
 *            record como chave de HashMap (equals e hashCode de graça),
 *            ArrayDeque usado como fila e como pilha, reconstrução de caminho
 *            pelo mapa de antecessores, enum e switch expression, text blocks.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrafoBfsDfs {

    /** Uma célula do labirinto. Como é record, duas células com a mesma linha e coluna são iguais: serve de chave em Map. */
    record Celula(int linha, int coluna) {
        String posicao() { return "linha " + linha + ", coluna " + coluna; }
    }

    enum Estrategia { LARGURA, PROFUNDIDADE }

    /** O que uma busca devolve: o caminho (vazio se não existe) e quantas células ela precisou visitar. */
    record Resultado(List<Celula> caminho, int visitadas) {
        boolean achou() { return !caminho.isEmpty(); }
    }

    /** O labirinto já convertido em grafo: cada célula livre aponta para as vizinhas livres (cima, baixo, esquerda, direita). */
    static class Labirinto {
        final char[][] grade;
        final Celula entrada, saida;
        final Map<Celula, List<Celula>> adjacencia = new HashMap<>();

        Labirinto(List<String> linhas) {
            if (linhas.isEmpty()) throw new IllegalArgumentException("O labirinto está vazio.");
            int largura = linhas.stream().mapToInt(String::length).max().getAsInt();
            grade = new char[linhas.size()][largura];
            Celula e = null, s = null;
            for (int l = 0; l < linhas.size(); l++) {
                Arrays.fill(grade[l], '#');   // linhas mais curtas ganham parede no fim
                for (int c = 0; c < linhas.get(l).length(); c++) {
                    char ch = linhas.get(l).charAt(c);
                    grade[l][c] = switch (ch) {
                        case '#', '.', 'E', 'S' -> ch;
                        case ' ' -> '.';
                        default -> throw new IllegalArgumentException("Caractere inválido '" + ch + "' na linha " + (l + 1) + ", coluna " + (c + 1) + ". Use # . E S.");
                    };
                    if (ch == 'E') { if (e != null) throw new IllegalArgumentException("Há mais de uma entrada (E)."); e = new Celula(l, c); }
                    if (ch == 'S') { if (s != null) throw new IllegalArgumentException("Há mais de uma saída (S)."); s = new Celula(l, c); }
                }
            }
            if (e == null) throw new IllegalArgumentException("O labirinto não tem entrada (E).");
            if (s == null) throw new IllegalArgumentException("O labirinto não tem saída (S).");
            entrada = e;
            saida = s;
            montarGrafo();
        }

        /** Monta a lista de adjacência: um vértice por célula livre, uma aresta por par de vizinhas livres. */
        private void montarGrafo() {
            int[][] passos = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int l = 0; l < grade.length; l++) {
                for (int c = 0; c < grade[l].length; c++) {
                    if (grade[l][c] == '#') continue;
                    List<Celula> vizinhas = new ArrayList<>();
                    for (int[] p : passos) {
                        int nl = l + p[0], nc = c + p[1];
                        boolean dentro = nl >= 0 && nl < grade.length && nc >= 0 && nc < grade[nl].length;
                        if (dentro && grade[nl][nc] != '#') vizinhas.add(new Celula(nl, nc));
                    }
                    adjacencia.put(new Celula(l, c), vizinhas);
                }
            }
        }

        int vertices() { return adjacencia.size(); }

        int arestas() { return adjacencia.values().stream().mapToInt(List::size).sum() / 2; }   // cada aresta aparece duas vezes

        /** Devolve o desenho do labirinto com o caminho marcado por asteriscos (entrada e saída ficam como estão). */
        String desenhar(List<Celula> caminho) {
            char[][] copia = new char[grade.length][];
            for (int l = 0; l < grade.length; l++) copia[l] = grade[l].clone();
            for (Celula c : caminho) if (copia[c.linha()][c.coluna()] == '.') copia[c.linha()][c.coluna()] = '*';
            StringBuilder sb = new StringBuilder();
            for (char[] linha : copia) sb.append("   ").append(linha).append('\n');
            return sb.toString();
        }
    }

    /**
     * BFS e DFS são o mesmo algoritmo com uma estrutura diferente: a BFS tira da frente
     * (fila, primeiro a entrar é o primeiro a sair) e por isso visita por camadas de distância,
     * o que garante o caminho mais curto; a DFS tira de trás (pilha) e mergulha num corredor
     * até o fim antes de voltar, achando um caminho qualquer, raramente o mais curto.
     */
    static Resultado buscar(Labirinto lab, Estrategia estrategia) {
        Deque<Celula> pendentes = new ArrayDeque<>();
        Map<Celula, Celula> anterior = new HashMap<>();   // de onde chegamos em cada célula; também marca as visitadas
        pendentes.addLast(lab.entrada);
        anterior.put(lab.entrada, lab.entrada);
        while (!pendentes.isEmpty()) {
            Celula atual = estrategia == Estrategia.LARGURA ? pendentes.pollFirst() : pendentes.pollLast();
            if (atual.equals(lab.saida)) return new Resultado(reconstruir(anterior, lab.entrada, lab.saida), anterior.size());
            for (Celula vizinha : lab.adjacencia.get(atual)) {
                if (!anterior.containsKey(vizinha)) {   // marcar na hora de enfileirar evita colocar a mesma célula duas vezes
                    anterior.put(vizinha, atual);
                    pendentes.addLast(vizinha);
                }
            }
        }
        return new Resultado(List.of(), anterior.size());   // a fila esvaziou sem chegar à saída
    }

    /** Anda de trás para a frente pelos antecessores, da saída até a entrada, e depois inverte. */
    static List<Celula> reconstruir(Map<Celula, Celula> anterior, Celula entrada, Celula saida) {
        List<Celula> caminho = new ArrayList<>();
        for (Celula c = saida; !c.equals(entrada); c = anterior.get(c)) caminho.add(c);
        caminho.add(entrada);
        Collections.reverse(caminho);
        return caminho;
    }

    static void resolver(String titulo, List<String> linhas) {
        Labirinto lab = new Labirinto(linhas);
        System.out.println("=== " + titulo + " (" + lab.grade.length + " linhas x " + lab.grade[0].length + " colunas) ===");
        System.out.print(lab.desenhar(List.of()));
        System.out.println("Grafo: " + lab.vertices() + " vértices (células livres) e " + lab.arestas() + " arestas (passos possíveis).");
        System.out.println("Entrada na " + lab.entrada.posicao() + "; saída na " + lab.saida.posicao() + " (contando do zero).");
        for (Estrategia e : Estrategia.values()) {
            Resultado r = buscar(lab, e);
            String nome = e == Estrategia.LARGURA ? "Busca em largura (BFS)" : "Busca em profundidade (DFS)";
            if (r.achou()) {
                System.out.println(nome + ": caminho com " + (r.caminho().size() - 1) + " passos, visitando " + r.visitadas() + " células.");
                System.out.print(lab.desenhar(r.caminho()));
            } else {
                System.out.println(nome + ": não existe caminho. Visitou todas as " + r.visitadas() + " células alcançáveis a partir da entrada.");
            }
        }
        int bfs = buscar(lab, Estrategia.LARGURA).caminho().size(), dfs = buscar(lab, Estrategia.PROFUNDIDADE).caminho().size();
        if (bfs > 0 && dfs > bfs) System.out.println("A BFS garante o caminho mais curto; a DFS achou um " + (dfs - bfs) + " passo(s) mais longo.");
        System.out.println();
    }

    static void demo() {
        System.out.println("CodeNomad Academy :: Grafos, BFS e DFS num labirinto, modo demonstração\n");
        List<String> aberto = """
                E.....#.......#
                .####.#.#####.#
                .#....#.#...#.#
                .#.####.#.#.#.#
                .#......#.#...#
                .######.#.###.#
                ........#.....S
                """.lines().toList();
        resolver("Labirinto com saída", aberto);

        List<String> fechado = """
                E...#....
                .##.#.##.
                .#..#..#.
                .#.###.#.
                ...#...#S
                """.lines().toList();
        resolver("Labirinto sem saída (a parede do meio fecha tudo)", fechado);
        System.out.println("Dica: troque um # por . na coluna do meio do segundo labirinto e rode de novo para ver o caminho aparecer.");
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            List<String> linhas;
            if (args[0].equals("--linhas")) {
                linhas = Arrays.asList(args).subList(1, args.length);
                if (linhas.isEmpty()) throw new IllegalArgumentException("--linhas precisa de pelo menos uma linha do labirinto.");
            } else {
                if (args.length > 1) throw new IllegalArgumentException("Passe um único arquivo, ou use --linhas para escrever o labirinto na hora.");
                try {
                    linhas = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    String motivo = e instanceof java.nio.file.NoSuchFileException ? "ele não existe" : e.getMessage();
                    throw new IllegalArgumentException("Não consegui ler o arquivo '" + args[0] + "': " + motivo + ".");
                }
            }
            resolver("Labirinto de " + (args[0].equals("--linhas") ? "linha de comando" : args[0]), linhas);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java GrafoBfsDfs.java <arquivo.txt>
                 java GrafoBfsDfs.java --linhas <linha1> <linha2> ...
              O labirinto usa # para parede, . (ou espaço) para corredor, E para a entrada e S para a saída.
              Linhas mais curtas são completadas com parede. Só anda em quatro direções, sem diagonal.
            Exemplos:
              java GrafoBfsDfs.java labirinto.txt
              java GrafoBfsDfs.java --linhas "E..#...." ".#.#.##." ".#...#.S"
              java GrafoBfsDfs.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Permita andar na diagonal (oito vizinhas) e veja como o caminho mais curto muda.
 *  2. Marque cada célula com a distância até a entrada (0, 1, 2, ...) usando a ordem em que a BFS visita, e desenhe esse mapa.
 *  3. Dê peso às células (areia custa 3 passos, chão custa 1) e troque a fila por uma fila de prioridade: nasce o algoritmo de Dijkstra.
 */
