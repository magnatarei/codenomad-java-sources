/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  50. Quiz de perguntas e respostas
 * ------------------------------------------------------------
 * O que faz: quiz de múltipla escolha lido de um arquivo de texto simples
 *            (vem com um banco embutido sobre programação). Embaralha as
 *            perguntas, conta os pontos, mostra a correção na hora e, no fim,
 *            revisa as erradas. Pode gerar o arquivo de exemplo para você editar.
 * Como usar: java QuizPerguntas.java --gerar perguntas.txt      (cria o arquivo de exemplo)
 *            java QuizPerguntas.java perguntas.txt --embaralhar
 *            java QuizPerguntas.java --embutido
 *            java QuizPerguntas.java --demo
 * O que você aprende: leitura de arquivo linha a linha com validação e número
 *            da linha no erro, records, text blocks, Collections.shuffle com
 *            semente, listas, Scanner e separação entre o jogo e a origem das
 *            respostas (teclado ou automática).
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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntUnaryOperator;

public class QuizPerguntas {

    /** Uma pergunta: enunciado, as opções na ordem e o índice (0 a 3) da certa. */
    record Pergunta(String enunciado, List<String> opcoes, int correta) {
        String letraCerta() { return String.valueOf((char) ('A' + correta)); }
    }

    /** Guarda o que aconteceu em cada pergunta, para a revisão do fim. */
    record Resposta(Pergunta pergunta, int escolhida) {
        boolean acertou() { return escolhida == pergunta.correta(); }
    }

    /** O formato do arquivo: P: enunciado, quatro opções A) a D), R: letra certa, linha vazia entre perguntas. */
    static final String BANCO = """
            P: Qual palavra-chave declara uma constante em Java?
            A) const
            B) final
            C) static
            D) immutable
            R: B

            P: O que o método main precisa ser para a JVM encontrá-lo?
            A) public static
            B) private
            C) protected final
            D) abstract
            R: A

            P: Qual estrutura guarda pares chave e valor?
            A) List
            B) Set
            C) Map
            D) Queue
            R: C

            P: Qual é o resultado de 7 / 2 quando os dois são int?
            A) 3.5
            B) 3
            C) 4
            D) erro de compilação
            R: B

            P: Que tipo de exceção o compilador obriga a tratar ou declarar?
            A) RuntimeException
            B) Error
            C) Exception verificada (checked)
            D) ArithmeticException
            R: C

            P: O que um record gera automaticamente?
            A) Só o construtor
            B) Construtor, acessores, equals, hashCode e toString
            C) Getters e setters
            D) Uma interface
            R: B

            P: Qual coleção não aceita elementos repetidos?
            A) ArrayList
            B) LinkedList
            C) HashSet
            D) ArrayDeque
            R: C

            P: O que acontece ao chamar um método numa referência null?
            A) Devolve null
            B) Lança uma exceção em tempo de execução
            C) Não compila
            D) Devolve 0
            R: B
            """;

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        List<Pergunta> perguntas = null;
        boolean embaralhar = false;
        Random sorteio = new Random();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--demo" -> { demo(); return; }
                case "--embutido" -> perguntas = ler(BANCO.lines().toList(), "banco embutido");
                case "--embaralhar" -> embaralhar = true;
                case "--semente" -> sorteio = new Random(inteiro(args, ++i));
                case "--gerar" -> {
                    if (i + 1 >= args.length) falhar("--gerar precisa do nome do arquivo a criar");
                    Path destino = Path.of(args[++i]);
                    if (Files.exists(destino)) falhar("'" + destino + "' já existe; não vou sobrescrever");
                    Files.writeString(destino, BANCO);
                    System.out.println("Arquivo de exemplo criado em " + destino.toAbsolutePath() + ". Edite e rode o quiz com ele.");
                    return;
                }
                default -> {
                    if (args[i].startsWith("--")) falhar("não conheço a opção '" + args[i] + "'");
                    Path arquivo = Path.of(args[i]);
                    if (!Files.isRegularFile(arquivo)) falhar("não encontrei o arquivo '" + args[i] + "'");
                    perguntas = ler(Files.readAllLines(arquivo, StandardCharsets.UTF_8), arquivo.toString());
                }
            }
        }
        if (perguntas == null) falhar("diga qual arquivo usar, ou --embutido");
        if (embaralhar) Collections.shuffle(perguntas, sorteio);
        var teclado = new Scanner(System.in, StandardCharsets.UTF_8);
        jogar(perguntas, n -> lerDoTeclado(teclado));
    }

    /** Interpreta as linhas do arquivo. Qualquer deslize vira uma mensagem com o número da linha. */
    static List<Pergunta> ler(List<String> linhas, String origem) {
        var perguntas = new ArrayList<Pergunta>();
        String enunciado = null;
        var opcoes = new ArrayList<String>();
        for (int n = 1; n <= linhas.size(); n++) {
            String linha = linhas.get(n - 1).strip();
            if (linha.isEmpty()) continue;
            if (linha.startsWith("P:")) {
                if (enunciado != null) falhar(origem + ", linha " + n + ": a pergunta anterior ficou sem resposta (R:)");
                enunciado = linha.substring(2).strip();
                opcoes = new ArrayList<>();
            } else if (linha.length() > 2 && linha.charAt(1) == ')' && "ABCD".indexOf(linha.charAt(0)) == opcoes.size()) {
                // As opções precisam vir em ordem: A), depois B), e assim por diante.
                if (enunciado == null) falhar(origem + ", linha " + n + ": opção antes de qualquer pergunta (P:)");
                opcoes.add(linha.substring(2).strip());
            } else if (linha.startsWith("R:")) {
                String letra = linha.substring(2).strip().toUpperCase();
                if (enunciado == null || opcoes.size() != 4) falhar(origem + ", linha " + n + ": a resposta precisa vir depois de P: e de quatro opções A) a D)");
                if (letra.length() != 1 || "ABCD".indexOf(letra.charAt(0)) < 0) falhar(origem + ", linha " + n + ": a resposta precisa ser A, B, C ou D, recebi '" + letra + "'");
                perguntas.add(new Pergunta(enunciado, List.copyOf(opcoes), letra.charAt(0) - 'A'));
                enunciado = null;
            } else {
                falhar(origem + ", linha " + n + ": não entendi '" + linha + "' (esperava P:, A) a D) ou R:)");
            }
        }
        if (enunciado != null) falhar(origem + ": a última pergunta ficou sem resposta (R:)");
        if (perguntas.isEmpty()) falhar(origem + ": nenhuma pergunta encontrada");
        return perguntas;
    }

    /** O jogo em si. O respondedor recebe o número da pergunta e devolve a opção escolhida (0 a 3). */
    static void jogar(List<Pergunta> perguntas, IntUnaryOperator respondedor) {
        var respostas = new ArrayList<Resposta>();
        System.out.println("=== Quiz: " + perguntas.size() + " perguntas ===");
        for (int i = 0; i < perguntas.size(); i++) {
            Pergunta p = perguntas.get(i);
            System.out.println();
            System.out.println((i + 1) + ". " + p.enunciado());
            for (int o = 0; o < 4; o++) System.out.println("   " + (char) ('A' + o) + ") " + p.opcoes().get(o));
            int escolhida = respondedor.applyAsInt(i);
            if (escolhida < 0) { System.out.println("Quiz interrompido."); break; }
            var r = new Resposta(p, escolhida);
            respostas.add(r);
            System.out.println(r.acertou() ? "   Certo!" : "   Errado. A certa era " + p.letraCerta() + ") " + p.opcoes().get(p.correta()));
        }
        long acertos = respostas.stream().filter(Resposta::acertou).count();
        System.out.println();
        System.out.printf("Pontuação: %d de %d (%.0f%%)%n", acertos, respostas.size(), 100.0 * acertos / Math.max(1, respostas.size()));
        List<Resposta> erradas = respostas.stream().filter(r -> !r.acertou()).toList();
        if (erradas.isEmpty()) { System.out.println("Gabaritou! Nada para revisar."); return; }
        System.out.println("Revisão das " + erradas.size() + " erradas:");
        for (Resposta r : erradas) {
            System.out.println("  - " + r.pergunta().enunciado());
            System.out.println("    você marcou " + (char) ('A' + r.escolhida()) + ", a certa é " + r.pergunta().letraCerta()
                    + ") " + r.pergunta().opcoes().get(r.pergunta().correta()));
        }
    }

    /** Lê A, B, C ou D do teclado; devolve -1 se a entrada acabar. */
    static int lerDoTeclado(Scanner teclado) {
        while (true) {
            System.out.print("   Sua resposta (A-D): ");
            if (!teclado.hasNextLine()) return -1;
            String texto = teclado.nextLine().strip().toUpperCase();
            if (texto.length() == 1 && "ABCD".indexOf(texto.charAt(0)) >= 0) return texto.charAt(0) - 'A';
            System.out.println("   Digite só a letra: A, B, C ou D.");
        }
    }

    /** Responde sozinho: acerta a maioria e erra as perguntas 2, 5 e 7 de propósito. */
    static void demo() {
        System.out.println("=== Demonstração: respostas automáticas, com três erros de propósito ===");
        List<Pergunta> perguntas = ler(BANCO.lines().toList(), "banco embutido");
        Collections.shuffle(perguntas, new Random(7));     // semente fixa: a mesma ordem em toda execução
        jogar(perguntas, i -> {
            Pergunta p = perguntas.get(i);
            int escolhida = (i == 1 || i == 4 || i == 6) ? (p.correta() + 1) % 4 : p.correta();
            System.out.println("   Sua resposta (A-D): " + (char) ('A' + escolhida));
            return escolhida;
        });
    }

    static int inteiro(String[] args, int i) {
        if (i >= args.length) falhar("--semente precisa de um número");
        try {
            return Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            falhar("--semente precisa de um número inteiro; recebi '" + args[i] + "'");
            return 0;
        }
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo + ".");
        System.exit(1);
    }

    static void uso() {
        System.out.println("""
            Uso: java QuizPerguntas.java <perguntas.txt> [--embaralhar] [--semente N]
                 java QuizPerguntas.java --embutido [--embaralhar]
                 java QuizPerguntas.java --gerar <perguntas.txt>     (cria um arquivo de exemplo para editar)
            Formato do arquivo: "P: pergunta", quatro linhas "A) ..." a "D) ...", "R: letra", linha vazia.
            Demonstração: java QuizPerguntas.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Embaralhe também as opções de cada pergunta, lembrando de recalcular qual é a certa.
 *  2. Adicione um cronômetro por pergunta: respostas em menos de 10 segundos valem ponto dobrado.
 *  3. Grave os resultados com data num CSV e mostre a evolução da nota ao longo das tentativas.
 */
