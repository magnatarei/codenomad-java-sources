/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  10. Calculadora de geometria
 * ------------------------------------------------------------
 * O que faz: área e perímetro de quadrado, retângulo, triângulo (conferindo
 *            se ele existe pela desigualdade triangular), círculo e trapézio,
 *            e volume e área de superfície de cubo, esfera e cilindro.
 * Como usar: java CalculadoraGeometria.java triangulo 3 4 5
 *            java CalculadoraGeometria.java circulo 2,5
 *            java CalculadoraGeometria.java trapezio 10 6 4
 *            java CalculadoraGeometria.java cilindro 3 10
 *            java CalculadoraGeometria.java --demo
 * O que você aprende: sealed interfaces, records que implementam interfaces,
 *            pattern matching com instanceof, switch expression para criar
 *            objetos a partir de texto, Math (sqrt, hypot, PI, pow) e a
 *            fórmula de Heron.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CalculadoraGeometria {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    // "sealed" fecha a lista de quem pode implementar: o compilador conhece todos os casos.
    sealed interface Figura permits Forma, Solido {
        String nome();
    }

    sealed interface Forma extends Figura permits Quadrado, Retangulo, Triangulo, Circulo, Trapezio {
        double area();
        double perimetro();
    }

    sealed interface Solido extends Figura permits Cubo, Esfera, Cilindro {
        double volume();
        double areaSuperficie();
    }

    record Quadrado(double lado) implements Forma {
        Quadrado { positivo(lado, "lado"); }
        public String nome() { return "Quadrado de lado " + n(lado); }
        public double area() { return lado * lado; }
        public double perimetro() { return 4 * lado; }
    }

    record Retangulo(double base, double altura) implements Forma {
        Retangulo { positivo(base, "base"); positivo(altura, "altura"); }
        public String nome() { return "Retângulo " + n(base) + " x " + n(altura); }
        public double area() { return base * altura; }
        public double perimetro() { return 2 * (base + altura); }
    }

    record Triangulo(double a, double b, double c) implements Forma {
        Triangulo {
            positivo(a, "lado a"); positivo(b, "lado b"); positivo(c, "lado c");
            // Desigualdade triangular: cada lado precisa ser menor que a soma dos outros dois.
            if (a >= b + c || b >= a + c || c >= a + b) {
                throw new IllegalArgumentException("Não existe triângulo com lados " + n(a) + ", " + n(b) + " e " + n(c)
                        + ": cada lado deve ser menor que a soma dos outros dois.");
            }
        }
        public String nome() { return "Triângulo " + n(a) + ", " + n(b) + ", " + n(c) + " (" + tipo() + ")"; }
        public double perimetro() { return a + b + c; }
        /** Fórmula de Heron: só precisa dos três lados, sem conhecer a altura. */
        public double area() {
            double s = perimetro() / 2;
            return Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }
        String tipo() {
            if (a == b && b == c) return "equilátero";
            if (a == b || b == c || a == c) return "isósceles";
            return "escaleno";
        }
    }

    record Circulo(double raio) implements Forma {
        Circulo { positivo(raio, "raio"); }
        public String nome() { return "Círculo de raio " + n(raio); }
        public double area() { return Math.PI * raio * raio; }
        public double perimetro() { return 2 * Math.PI * raio; }
    }

    record Trapezio(double baseMaior, double baseMenor, double altura, double lado1, double lado2) implements Forma {
        Trapezio {
            positivo(baseMaior, "base maior"); positivo(baseMenor, "base menor"); positivo(altura, "altura");
            positivo(lado1, "lado 1"); positivo(lado2, "lado 2");
            if (baseMenor > baseMaior) throw new IllegalArgumentException("A base maior (" + n(baseMaior) + ") deve ser maior ou igual à menor (" + n(baseMenor) + ").");
        }
        /** Sem os lados, assumimos o trapézio isósceles e achamos o lado por Pitágoras (Math.hypot). */
        Trapezio(double baseMaior, double baseMenor, double altura) {
            this(baseMaior, baseMenor, altura,
                    Math.hypot(altura, (baseMaior - baseMenor) / 2), Math.hypot(altura, (baseMaior - baseMenor) / 2));
        }
        public String nome() { return "Trapézio bases " + n(baseMaior) + " e " + n(baseMenor) + ", altura " + n(altura); }
        public double area() { return (baseMaior + baseMenor) * altura / 2; }
        public double perimetro() { return baseMaior + baseMenor + lado1 + lado2; }
    }

    record Cubo(double aresta) implements Solido {
        Cubo { positivo(aresta, "aresta"); }
        public String nome() { return "Cubo de aresta " + n(aresta); }
        public double volume() { return Math.pow(aresta, 3); }
        public double areaSuperficie() { return 6 * aresta * aresta; }
    }

    record Esfera(double raio) implements Solido {
        Esfera { positivo(raio, "raio"); }
        public String nome() { return "Esfera de raio " + n(raio); }
        public double volume() { return 4.0 / 3.0 * Math.PI * Math.pow(raio, 3); }
        public double areaSuperficie() { return 4 * Math.PI * raio * raio; }
    }

    record Cilindro(double raio, double altura) implements Solido {
        Cilindro { positivo(raio, "raio"); positivo(altura, "altura"); }
        public String nome() { return "Cilindro raio " + n(raio) + ", altura " + n(altura); }
        public double volume() { return Math.PI * raio * raio * altura; }
        public double areaSuperficie() { return 2 * Math.PI * raio * (raio + altura); }
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
            double[] medidas = new double[args.length - 1];
            for (int i = 1; i < args.length; i++) medidas[i - 1] = numero(args[i]);
            imprimir(criar(args[0], medidas));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: todas as formas e sólidos com medidas de exemplo ===");
        System.out.println();
        List<Figura> figuras = List.of(
                new Quadrado(4), new Retangulo(3, 5), new Triangulo(3, 4, 5), new Triangulo(6, 6, 6),
                new Circulo(2.5), new Trapezio(10, 6, 4),
                new Cubo(3), new Esfera(2), new Cilindro(3, 10));
        System.out.printf("%-42s | %14s | %14s%n", "Figura", "Área / Volume", "Perím. / Superf.");
        System.out.println("-".repeat(76));
        for (Figura f : figuras) imprimir(f);
        System.out.println("-".repeat(76));
        System.out.println("Medidas em unidades genéricas: área em u², volume em u³.");
        System.out.println();
        System.out.println("> triangulo 1 2 10");
        try {
            criar("triangulo", new double[]{1, 2, 10});
        } catch (IllegalArgumentException e) {
            System.out.println("  Erro tratado: " + e.getMessage());
        }
    }

    /** Monta a figura certa a partir do nome e das medidas. Aceita com e sem acento. */
    static Figura criar(String nome, double[] m) {
        return switch (nome.toLowerCase()) {
            case "quadrado" -> { exigir(m, 1, "quadrado <lado>"); yield new Quadrado(m[0]); }
            case "retangulo", "retângulo" -> { exigir(m, 2, "retangulo <base> <altura>"); yield new Retangulo(m[0], m[1]); }
            case "triangulo", "triângulo" -> { exigir(m, 3, "triangulo <a> <b> <c>"); yield new Triangulo(m[0], m[1], m[2]); }
            case "circulo", "círculo" -> { exigir(m, 1, "circulo <raio>"); yield new Circulo(m[0]); }
            case "trapezio", "trapézio" -> {
                if (m.length == 5) yield new Trapezio(m[0], m[1], m[2], m[3], m[4]);
                exigir(m, 3, "trapezio <base maior> <base menor> <altura> [lado1 lado2]");
                yield new Trapezio(m[0], m[1], m[2]);
            }
            case "cubo" -> { exigir(m, 1, "cubo <aresta>"); yield new Cubo(m[0]); }
            case "esfera" -> { exigir(m, 1, "esfera <raio>"); yield new Esfera(m[0]); }
            case "cilindro" -> { exigir(m, 2, "cilindro <raio> <altura>"); yield new Cilindro(m[0], m[1]); }
            default -> throw new IllegalArgumentException("Forma desconhecida '" + nome + "'. Veja a lista no uso.");
        };
    }

    /** O instanceof com padrão já declara a variável do tipo certo: sem cast à parte. */
    static void imprimir(Figura f) {
        if (f instanceof Forma forma) {
            System.out.printf(PT_BR, "%-42s | %14.4f | %14.4f%n", forma.nome(), forma.area(), forma.perimetro());
        } else if (f instanceof Solido solido) {
            System.out.printf(PT_BR, "%-42s | %14.4f | %14.4f%n", solido.nome(), solido.volume(), solido.areaSuperficie());
        }
    }

    static void positivo(double valor, String campo) {
        if (valor <= 0 || Double.isNaN(valor)) throw new IllegalArgumentException("O campo '" + campo + "' deve ser maior que zero, recebi " + n(valor) + ".");
    }

    static void exigir(double[] m, int quantidade, String forma) {
        if (m.length != quantidade) throw new IllegalArgumentException("Use: " + forma + " (recebi " + m.length + " medida(s): " + Arrays.toString(m) + ")");
    }

    /** Inteiros saem sem casas; os demais com até duas casas. */
    static String n(double valor) {
        return valor == Math.rint(valor) ? String.format(PT_BR, "%.0f", valor) : String.format(PT_BR, "%.2f", valor);
    }

    /** Aceita "2.5" e "2,5": troca a vírgula por ponto antes de converter. */
    static double numero(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A medida precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraGeometria.java <forma> <medidas...>
              quadrado <lado>                 retangulo <base> <altura>
              triangulo <a> <b> <c>           circulo <raio>
              trapezio <base maior> <base menor> <altura> [lado1 lado2]
              cubo <aresta>                   esfera <raio>
              cilindro <raio> <altura>
            Exemplos:
              java CalculadoraGeometria.java triangulo 3 4 5
              java CalculadoraGeometria.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente o losango e o hexágono regular só criando novos records e um case no switch.
 *  2. Ordene uma lista de formas pela área com Comparator.comparingDouble(Forma::area).
 *  3. Receba a unidade (cm, m) e mostre os resultados com cm², cm³ etc.
 */
