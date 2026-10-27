/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  27. Calculadora de expressões matemáticas
 * ------------------------------------------------------------
 * O que faz: lê uma expressão em texto com + - * / ^, parênteses e números com
 *            casas decimais, respeita a precedência e a associatividade, mostra
 *            a notação posfixa (polonesa reversa) e calcula o resultado, com
 *            erros claros para expressão mal formada e divisão por zero.
 * Como usar: java CalculadoraExpressoes.java "3 + 4 * (2 - 1) ^ 2"
 *            java CalculadoraExpressoes.java --posfixa "(1,5 + 2,5) * -3" "2 ^ 3 ^ 2"
 *            java CalculadoraExpressoes.java --demo
 * O que você aprende: tokenização de texto, o algoritmo shunting-yard de
 *            Dijkstra (infixa para posfixa), pilhas com ArrayDeque, enum com
 *            precedência e associatividade, sealed interface com records para
 *            os tokens, pattern matching com instanceof e exceções úteis.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalculadoraExpressoes {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Um token é um número, um operador ou um parêntese. A interface selada fecha a lista para o compilador. */
    sealed interface Token permits Numero, Operador, Parentese {}

    record Numero(double valor) implements Token {
        @Override public String toString() { return formatar(valor); }
    }

    record Parentese(boolean abre) implements Token {
        @Override public String toString() { return abre ? "(" : ")"; }
    }

    /** Precedência maior é resolvida antes. "direita" marca os associativos à direita: 2^3^2 = 2^(3^2) = 512. */
    enum Operador implements Token {
        SOMA('+', 1, false), SUBTRACAO('-', 1, false), MULTIPLICACAO('*', 2, false), DIVISAO('/', 2, false),
        NEGACAO('~', 3, true), POTENCIA('^', 4, true);

        final char simbolo;
        final int precedencia;
        final boolean direita;

        Operador(char simbolo, int precedencia, boolean direita) { this.simbolo = simbolo; this.precedencia = precedencia; this.direita = direita; }

        boolean unario() { return this == NEGACAO; }

        static Operador porSimbolo(char c) {
            for (Operador op : values()) if (op.simbolo == c && op != NEGACAO) return op;   // a negação nasce de um "-" no lugar certo
            return null;
        }

        @Override public String toString() { return this == NEGACAO ? "neg" : String.valueOf(simbolo); }
    }

    /** Quebra o texto em tokens. Um "-" vira negação quando não há operando antes dele. */
    static List<Token> tokenizar(String texto) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c) || c == '.' || c == ',') {
                int inicio = i;
                while (i < texto.length() && (Character.isDigit(texto.charAt(i)) || texto.charAt(i) == '.' || texto.charAt(i) == ',')) i++;
                String trecho = texto.substring(inicio, i);
                try {
                    tokens.add(new Numero(Double.parseDouble(trecho.replace(',', '.'))));   // aceita 2,5 e 2.5
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Número mal formado '" + trecho + "' na posição " + inicio + ".");
                }
                continue;
            }
            if (c == '(' || c == ')') { tokens.add(new Parentese(c == '(')); i++; continue; }
            Operador op = Operador.porSimbolo(c);
            if (op == null) throw new IllegalArgumentException("Símbolo desconhecido '" + c + "' na posição " + i + ".");
            if (op == Operador.SUBTRACAO && esperaOperando(tokens)) op = Operador.NEGACAO;
            tokens.add(op);
            i++;
        }
        return tokens;
    }

    /** No início, depois de um operador ou depois de "(" o próximo token precisa ser um operando. */
    static boolean esperaOperando(List<Token> tokens) {
        if (tokens.isEmpty()) return true;
        Token ultimo = tokens.get(tokens.size() - 1);
        return ultimo instanceof Operador || (ultimo instanceof Parentese p && p.abre());
    }

    /** Shunting-yard: números vão direto para a saída; operadores esperam numa pilha até os mais fortes saírem. */
    static List<Token> paraPosfixa(List<Token> tokens) {
        List<Token> saida = new ArrayList<>();
        Deque<Token> pilha = new ArrayDeque<>();
        boolean esperaOperando = true;   // vigia a gramática: depois de número vem operador ou ")", e vice-versa
        for (Token t : tokens) {
            if (t instanceof Numero n) {
                if (!esperaOperando) throw new IllegalArgumentException("Faltou um operador antes de " + n + ".");
                saida.add(n);
                esperaOperando = false;
            } else if (t instanceof Operador op) {
                if (op.unario()) { pilha.push(op); continue; }   // prefixo: não tira ninguém da pilha, só espera o seu operando
                if (esperaOperando) throw new IllegalArgumentException("Operador '" + op + "' sem operando à esquerda.");
                // Sai da pilha quem tem precedência maior, ou igual quando o operador novo é associativo à esquerda.
                while (pilha.peek() instanceof Operador topo && (topo.precedencia > op.precedencia || (topo.precedencia == op.precedencia && !op.direita))) {
                    saida.add(pilha.pop());
                }
                pilha.push(op);
                esperaOperando = true;
            } else if (t instanceof Parentese p && p.abre()) {
                if (!esperaOperando) throw new IllegalArgumentException("Faltou um operador antes de '('.");
                pilha.push(p);
            } else {
                if (esperaOperando) throw new IllegalArgumentException("Faltou um operando antes de ')'.");
                while (!pilha.isEmpty() && !(pilha.peek() instanceof Parentese)) saida.add(pilha.pop());
                if (pilha.isEmpty()) throw new IllegalArgumentException("Parêntese ')' fechado sem ter sido aberto.");
                pilha.pop();   // descarta o "(" correspondente
            }
        }
        if (esperaOperando) throw new IllegalArgumentException(tokens.isEmpty() ? "Expressão vazia." : "A expressão termina sem o último operando.");
        while (!pilha.isEmpty()) {
            Token t = pilha.pop();
            if (t instanceof Parentese) throw new IllegalArgumentException("Parêntese '(' aberto sem fechar.");
            saida.add(t);
        }
        return saida;
    }

    /** Avalia a posfixa com uma pilha de números: cada operador consome os operandos do topo e devolve um resultado. */
    static double avaliar(List<Token> posfixa) {
        Deque<Double> pilha = new ArrayDeque<>();
        for (Token t : posfixa) {
            if (t instanceof Numero n) { pilha.push(n.valor()); continue; }
            Operador op = (Operador) t;
            if (op.unario()) { pilha.push(-pilha.pop()); continue; }
            double b = pilha.pop(), a = pilha.pop();   // b sai primeiro: é o operando da direita
            pilha.push(switch (op) {
                case SOMA -> a + b;
                case SUBTRACAO -> a - b;
                case MULTIPLICACAO -> a * b;
                case DIVISAO -> {
                    if (b == 0) throw new ArithmeticException("Divisão por zero em " + formatar(a) + " / 0.");
                    yield a / b;
                }
                case POTENCIA -> Math.pow(a, b);
                case NEGACAO -> throw new IllegalStateException("A negação já foi tratada acima.");
            });
        }
        double resultado = pilha.pop();
        if (Double.isNaN(resultado) || Double.isInfinite(resultado)) throw new ArithmeticException("O resultado não é um número finito (por exemplo, 0 ^ -1).");
        return resultado;
    }

    static void calcular(String expressao, boolean mostrarPosfixa) {
        List<Token> tokens = tokenizar(expressao);
        List<Token> posfixa = paraPosfixa(tokens);
        if (mostrarPosfixa) {
            System.out.println("   tokens:  " + juntar(tokens));
            System.out.println("   posfixa: " + juntar(posfixa));
        }
        System.out.println("   " + expressao.trim() + " = " + formatar(avaliar(posfixa)));
    }

    static String juntar(List<Token> tokens) {
        return tokens.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    /** Inteiros saem sem casas; os demais com até seis casas, sem zeros sobrando. */
    static String formatar(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.format(PT_BR, "%.0f", v);
        return String.format(PT_BR, "%.6f", v).replaceAll("0+$", "");
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        boolean mostrarPosfixa = args[0].equals("--posfixa");
        if (mostrarPosfixa && args.length == 1) {
            System.out.println("Erro: --posfixa precisa de pelo menos uma expressão.\n");
            uso();
            System.exit(1);
        }
        for (int i = mostrarPosfixa ? 1 : 0; i < args.length; i++) {
            try {
                calcular(args[i], mostrarPosfixa);
            } catch (IllegalArgumentException | ArithmeticException e) {
                System.out.println("   " + args[i] + " -> Erro: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    static void demo() {
        System.out.println("=== Demonstração 1: precedência e associatividade ===");
        for (String e : List.of("3 + 4 * 2", "(3 + 4) * 2", "2 ^ 3 ^ 2", "10 / 4 - 1,5", "-(2 + 3) * 4 ^ 0.5")) calcular(e, false);
        System.out.println("   (2 ^ 3 ^ 2 é 2 ^ 9 = 512, não 8 ^ 2 = 64: a potência associa à direita)");

        System.out.println("\n=== Demonstração 2: a notação posfixa de uma expressão ===");
        calcular("3 + 4 * 2 / (1 - 5) ^ 2", true);
        System.out.println("   Na posfixa não existem parênteses: a ordem dos operadores já diz quem calcula primeiro.");

        System.out.println("\n=== Demonstração 3: erros tratados ===");
        for (String e : List.of("(2 + 3", "10 / (5 - 5)", "2 + * 3", "7 $ 2")) {
            try {
                calcular(e, false);
            } catch (IllegalArgumentException | ArithmeticException ex) {
                System.out.println("   " + e + " -> Erro: " + ex.getMessage());
            }
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraExpressoes.java [--posfixa] "<expressão>" ["<outra>"...]
              Operadores: + - * / ^ e parênteses. Números aceitam vírgula ou ponto. O "-" também nega: -(2 + 3).
              --posfixa mostra os tokens e a notação posfixa antes do resultado.
            Exemplos:
              java CalculadoraExpressoes.java "3 + 4 * (2 - 1) ^ 2"
              java CalculadoraExpressoes.java --posfixa "(1,5 + 2,5) * -3"
              java CalculadoraExpressoes.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente o operador % (resto da divisão) com a mesma precedência de * e /.
 *  2. Aceite funções como raiz(16) e abs(-3): um token de função vai para a pilha e sai quando o ")" dela fecha.
 *  3. Escreva um avaliador por descida recursiva (expressao -> termo -> fator) e compare com o shunting-yard.
 */
