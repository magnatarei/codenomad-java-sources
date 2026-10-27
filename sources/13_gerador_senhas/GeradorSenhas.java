/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  13. Gerador de senhas seguras
 * ------------------------------------------------------------
 * O que faz: gera senhas com comprimento e classes de símbolos à sua escolha,
 *            usando o sorteio criptográfico da biblioteca padrão (SecureRandom),
 *            garante ao menos um símbolo de cada classe pedida e estima a entropia em bits.
 * Como usar: java GeradorSenhas.java --tamanho 16 --quantidade 3
 *            java GeradorSenhas.java --tamanho 20 --sem-simbolos --sem-ambiguos
 *            java GeradorSenhas.java --demo
 * O que você aprende: SecureRandom em vez de Random, record com validação no
 *            construtor, Collections.shuffle, logaritmo em base 2 para medir
 *            entropia, leitura de flags com switch e mensagens de erro úteis.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GeradorSenhas {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    static final String MAIUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final String DIGITOS = "0123456789";
    static final String SIMBOLOS = "!#$%&*+-=?@^_~";

    /** Parecidos demais em algumas fontes: zero e O maiúsculo, um, L minúsculo e I maiúsculo. */
    static final String AMBIGUOS = "0O1lI";

    /** Um único sorteador para o programa inteiro: SecureRandom é caro de criar e barato de usar. */
    static final SecureRandom SORTEIO = new SecureRandom();

    /** Tudo que define uma senha: tamanho, classes ligadas e se tira os símbolos ambíguos. */
    record Perfil(int tamanho, boolean minusculas, boolean maiusculas, boolean digitos, boolean simbolos, boolean semAmbiguos) {

        Perfil {
            if (tamanho < 4 || tamanho > 128) throw new IllegalArgumentException("o tamanho deve ficar entre 4 e 128, recebi " + tamanho + ".");
            if (!minusculas && !maiusculas && !digitos && !simbolos) throw new IllegalArgumentException("pelo menos uma classe de símbolos precisa ficar ligada.");
        }

        /** As classes ligadas, já sem os ambíguos quando pedido. */
        List<String> classes() {
            List<String> lista = new ArrayList<>();
            if (minusculas) lista.add(filtrar(MINUSCULAS));
            if (maiusculas) lista.add(filtrar(MAIUSCULAS));
            if (digitos) lista.add(filtrar(DIGITOS));
            if (simbolos) lista.add(SIMBOLOS);
            return lista;
        }

        String filtrar(String classe) {
            if (!semAmbiguos) return classe;
            StringBuilder sb = new StringBuilder();
            for (char c : classe.toCharArray()) if (AMBIGUOS.indexOf(c) < 0) sb.append(c);
            return sb.toString();
        }

        /** O alfabeto é a união das classes; o tamanho dele é o que define a entropia. */
        String alfabeto() {
            return String.join("", classes());
        }

        /** Entropia = tamanho x log2(alfabeto). Math não tem log2: usa a mudança de base. */
        double entropiaBits() {
            return tamanho * Math.log(alfabeto().length()) / Math.log(2);
        }

        String descricao() {
            List<String> nomes = new ArrayList<>();
            if (minusculas) nomes.add("minúsculas");
            if (maiusculas) nomes.add("maiúsculas");
            if (digitos) nomes.add("dígitos");
            if (simbolos) nomes.add("símbolos");
            return String.join(", ", nomes) + (semAmbiguos ? " (sem ambíguos)" : "");
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        if (args[0].equals("--demo")) {
            demo();
            return;
        }
        try {
            executar(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static String gerar(Perfil perfil) {
        List<Character> letras = new ArrayList<>();
        // Primeiro um símbolo de cada classe, para cumprir a garantia...
        for (String classe : perfil.classes()) letras.add(sortear(classe));
        // ...depois o resto vem do alfabeto inteiro...
        String alfabeto = perfil.alfabeto();
        while (letras.size() < perfil.tamanho()) letras.add(sortear(alfabeto));
        // ...e o embaralhamento espalha os garantidos, que senão ficariam sempre no começo.
        Collections.shuffle(letras, SORTEIO);
        StringBuilder sb = new StringBuilder();
        for (char c : letras) sb.append(c);
        return sb.toString();
    }

    static char sortear(String alfabeto) {
        return alfabeto.charAt(SORTEIO.nextInt(alfabeto.length()));
    }

    static String avaliar(double bits) {
        if (bits < 40) return "fraca";
        if (bits < 60) return "razoável";
        if (bits < 80) return "forte";
        return "muito forte";
    }

    /** Tempo para testar metade das combinações a 10 bilhões de tentativas por segundo. */
    static String tempoDeQuebra(double bits) {
        double segundos = Math.pow(2, bits - 1) / 1e10;
        if (segundos < 1) return "menos de um segundo";
        if (segundos < 3600) return String.format(PT_BR, "%.0f segundos", segundos);
        if (segundos < 86400) return String.format(PT_BR, "%.1f horas", segundos / 3600);
        double anos = segundos / 31_557_600; // ano de 365,25 dias; o sublinhado só ajuda a ler
        if (anos < 1) return String.format(PT_BR, "%.0f dias", segundos / 86400);
        if (anos < 1000) return String.format(PT_BR, "%.0f anos", anos);
        if (anos < 1e6) return String.format(PT_BR, "%.0f mil anos", anos / 1e3);
        if (anos < 1e9) return String.format(PT_BR, "%.0f milhões de anos", anos / 1e6);
        return "mais de um bilhão de anos";
    }

    static void imprimirPerfil(Perfil p) {
        double bits = p.entropiaBits();
        System.out.printf(PT_BR, "Perfil: %d símbolos, classes: %s, alfabeto com %d opções%n",
                p.tamanho(), p.descricao(), p.alfabeto().length());
        System.out.printf(PT_BR, "Entropia: %.1f bits (%s), força bruta levaria %s%n", bits, avaliar(bits), tempoDeQuebra(bits));
    }

    static void executar(String[] args) {
        int tamanho = 16, quantidade = 1;
        boolean minusculas = true, maiusculas = true, digitos = true, simbolos = true, semAmbiguos = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--tamanho" -> tamanho = inteiro(args, ++i, "--tamanho");
                case "--quantidade" -> quantidade = inteiro(args, ++i, "--quantidade");
                case "--sem-minusculas" -> minusculas = false;
                case "--sem-maiusculas" -> maiusculas = false;
                case "--sem-digitos" -> digitos = false;
                case "--sem-simbolos" -> simbolos = false;
                case "--sem-ambiguos" -> semAmbiguos = true;
                default -> throw new IllegalArgumentException("opção desconhecida '" + args[i] + "'.");
            }
        }
        if (quantidade < 1 || quantidade > 100) throw new IllegalArgumentException("a quantidade deve ficar entre 1 e 100, recebi " + quantidade + ".");
        Perfil perfil = new Perfil(tamanho, minusculas, maiusculas, digitos, simbolos, semAmbiguos);
        imprimirPerfil(perfil);
        for (int i = 0; i < quantidade; i++) System.out.println(gerar(perfil));
    }

    /** Lê o valor que vem depois de uma flag, com erro claro se faltar ou não for inteiro. */
    static int inteiro(String[] args, int posicao, String flag) {
        if (posicao >= args.length) throw new IllegalArgumentException(flag + " precisa de um valor.");
        try {
            return Integer.parseInt(args[posicao]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(flag + " precisa de um inteiro, recebi '" + args[posicao] + "'.");
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: três perfis, três senhas de cada ===");
        List<Perfil> perfis = List.of(
                new Perfil(12, true, true, true, true, false),
                new Perfil(16, true, true, true, false, true),
                new Perfil(24, true, false, true, false, false));
        int numero = 1;
        for (Perfil p : perfis) {
            System.out.println();
            System.out.println("Perfil " + numero++ + ":");
            imprimirPerfil(p);
            for (int i = 0; i < 3; i++) System.out.println("  " + gerar(p));
        }
        System.out.println();
        System.out.println("=== Como a entropia cresce com o tamanho (as quatro classes ligadas) ===");
        for (int t : new int[] {8, 12, 16, 24, 32}) {
            Perfil p = new Perfil(t, true, true, true, true, false);
            System.out.printf(PT_BR, "  %2d símbolos -> %6.1f bits, %-12s força bruta: %s%n",
                    t, p.entropiaBits(), avaliar(p.entropiaBits()) + ",", tempoDeQuebra(p.entropiaBits()));
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java GeradorSenhas.java [opções]
              --tamanho N        símbolos na senha, de 4 a 128 (padrão 16)
              --quantidade N     quantas senhas gerar, de 1 a 100 (padrão 1)
              --sem-minusculas   desliga as letras minúsculas
              --sem-maiusculas   desliga as letras maiúsculas
              --sem-digitos      desliga os dígitos
              --sem-simbolos     desliga os símbolos !#$%&*+-=?@^_~
              --sem-ambiguos     tira 0, O, 1, l e I, fáceis de confundir
              --demo             mostra três perfis e a tabela de entropia
            Exemplos:
              java GeradorSenhas.java --tamanho 16 --quantidade 3
              java GeradorSenhas.java --tamanho 20 --sem-simbolos --sem-ambiguos""");
    }
}

/*
 * Para ir além:
 *  1. Gere frases-senha: sorteie quatro palavras de uma lista e junte com hífen (mais fáceis de lembrar).
 *  2. Adicione --pronunciavel, alternando consoantes e vogais para senhas que dá para ditar.
 *  3. Copie a senha para a área de transferência com java.awt.Toolkit e avise o usuário.
 */
