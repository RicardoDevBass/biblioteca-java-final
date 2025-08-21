import java.util.*;
import java.util.regex.Pattern;

/**
 * BibliotecaCLI.java
 *
 * Classe de interface console para bibliotecários interagirem com o sistema de catálogo.
 *
 * Funcionalidades:
 * - verificar quantos livros estão cadastrados
 * - adicionar novos livros no sistema (via API pública ou interativo)
 * - remover livros do sistema
 * - confirmar empréstimo de livros (registrando nome + celular)
 * - confirmar recebimento (devolução) de livros emprestados
 * - listar livros e empréstimos ativos
 *
 * Observações de design:
 * - Campos internos são privados para preservar encapsulamento. Foram adicionados
 *   métodos públicos controlados para operações necessárias por outras classes.
 * - Empréstimos são mantidos em memória no Map emprestimosAtivos. Em produção,
 *   persistência em banco/arquivo é recomendada.
 * - Validações básicas (ISBN, celular, ano) são realizadas. O construtor de Livro
 *   ainda valida ano e campos obrigatórios.
 */
public class BibliotecaCLI {

    // Scanner para leitura de entrada do usuário — privado para evitar acesso indevido.
    private final Scanner scanner;

    // Referência ao catálogo (classe CatalogoLivros já implementada).
    private final CatalogoLivros catalogo;

    // Empréstimos ativos em memória: chave = ISBN normalizado (sem hífens/espacos)
    private final Map<String, EmprestimoRecord> emprestimosAtivos;

    // Validação simples para número de celular (apenas dígitos, 8-15 dígitos)
    private static final Pattern CELULAR_PATTERN = Pattern.compile("^\\d{8,15}$");

    // ----------------------------
    // Construtor
    // ----------------------------
    public BibliotecaCLI() {
        this.scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        this.catalogo = new CatalogoLivros();
        this.emprestimosAtivos = new HashMap<>();
    }

    // ----------------------------
    // API pública (evita expor campos privados)
    // ----------------------------

    /**
     * Adiciona um livro ao catálogo usando API pública.
     * @param livro instância de Livro
     * @return true se adicionado com sucesso, false se ISBN já existente
     */
    public boolean adicionarLivroAoCatalogo(Livro livro) {
        return catalogo.adicionarLivro(livro);
    }

    /**
     * Gera e retorna relatório estatístico do catálogo.
     * @return string com resumo do catálogo
     */
    public String gerarRelatorioCatalogo() {
        return catalogo.gerarRelatorioEstatisticas();
    }

    /**
     * Retorna referência ao catálogo caso seja necessário consulta direta.
     * Preferível usar métodos controlados ao invés de manipular o catálogo externamente.
     */
    public CatalogoLivros getCatalogo() {
        return this.catalogo;
    }

    /**
     * Fecha recursos internos (Scanner). Deve ser chamado ao finalizar a aplicação.
     */
    public void fechar() {
        try {
            scanner.close();
        } catch (Exception e) {
            // Não interromper a aplicação; apenas tentativa de limpeza
        }
    }

    // ----------------------------
    // Loop interativo (público) — permite iniciar a CLI a partir de outra classe
    // ----------------------------
    public void run() {
        System.out.println("=== Sistema auxiliar de catalogação (Console) ===");
        boolean continuar = true;
        while (continuar) {
            mostrarMenu();
            int opcao = lerInteiro("Escolha uma opção (número): ");
            switch (opcao) {
                case 1:
                    mostrarQuantidadeLivros();
                    break;
                case 2:
                    adicionarLivroInterativo();
                    break;
                case 3:
                    removerLivroInterativo();
                    break;
                case 4:
                    confirmarEmprestimoInterativo();
                    break;
                case 5:
                    confirmarRecebimentoInterativo();
                    break;
                case 6:
                    listarLivrosResumo();
                    break;
                case 7:
                    listarEmprestimosAtivos();
                    break;
                case 0:
                    System.out.println("Saindo do sistema. Até logo!");
                    continuar = false;
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }

    // ----------------------------
    // Menu e utilitários de entrada
    // ----------------------------
    private void mostrarMenu() {
        System.out.println();
        System.out.println("Menu:");
        System.out.println("1  - Verificar quantidade de livros cadastrados");
        System.out.println("2  - Adicionar novo livro");
        System.out.println("3  - Remover livro por ISBN");
        System.out.println("4  - Confirmar empréstimo de livro");
        System.out.println("5  - Confirmar recebimento (devolução) de livro");
        System.out.println("6  - Listar todos os livros (resumo)");
        System.out.println("7  - Listar empréstimos ativos");
        System.out.println("0  - Sair");
    }

    private int lerInteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String linha = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linha);
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Informe um número inteiro.");
            }
        }
    }

    private String lerTexto(String mensagem, boolean required) {
        while (true) {
            System.out.print(mensagem);
            String linha = scanner.nextLine();
            if (!required) return linha.trim();
            if (linha != null && !linha.trim().isEmpty()) {
                return linha.trim();
            }
            System.out.println("Este campo é obrigatório. Por favor, preencha.");
        }
    }

    // ----------------------------
    // Operações (interativas)
    // ----------------------------
    private void mostrarQuantidadeLivros() {
        String rel = catalogo.gerarRelatorioEstatisticas();
        System.out.println(rel);
    }

    private void adicionarLivroInterativo() {
        System.out.println("--- Adicionar novo livro ---");

        String isbn = lerTexto("ISBN (pode conter hífens / espaços): ", true);
        if (catalogo.buscarPorIsbn(isbn) != null) {
            System.out.println("Já existe um livro cadastrado com esse ISBN. Operação abortada.");
            return;
        }

        String titulo = lerTexto("Título: ", true);
        String autor = lerTexto("Autor principal: ", true);
        String editora = lerTexto("Editora (opcional): ", false);

        int anoPublicacao = lerInteiro("Ano de publicação (ex: 2020): ");

        int numeroPaginas = lerInteiro("Número de páginas (0 se desconhecido): ");
        String categoria = lerTexto("Categoria (ex: Programação): ", false);
        String idioma = lerTexto("Idioma (ex: Português): ", false);
        boolean disponivel = true;
        String localizacao = lerTexto("Localização física/virtual (opcional): ", false);

        try {
            Livro novo = new Livro(isbn, titulo, autor, editora,
                    anoPublicacao, categoria, numeroPaginas, idioma, disponivel, localizacao);

            boolean ok = catalogo.adicionarLivro(novo);
            if (ok) {
                System.out.println("Livro adicionado com sucesso: " + novo);
            } else {
                System.out.println("Falha ao adicionar livro (provavelmente ISBN duplicado).");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro ao criar livro: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro inesperado ao adicionar livro: " + e.getMessage());
        }
    }

    private void removerLivroInterativo() {
        System.out.println("--- Remover livro por ISBN ---");
        String isbn = lerTexto("ISBN do livro a remover: ", true);

        EmprestimoRecord er = emprestimosAtivos.get(normalizeIsbnSimple(isbn));
        if (er != null) {
            System.out.println("Aviso: existe um empréstimo ativo para este livro: " + er);
            String resp = lerTexto("Deseja remover o cadastro mesmo assim? (s/N): ", false);
            if (!resp.equalsIgnoreCase("s") && !resp.equalsIgnoreCase("sim")) {
                System.out.println("Operação de remoção cancelada.");
                return;
            } else {
                emprestimosAtivos.remove(normalizeIsbnSimple(isbn));
            }
        }

        boolean removed = catalogo.removerPorIsbn(isbn);
        if (removed) {
            System.out.println("Livro removido com sucesso.");
        } else {
            System.out.println("Livro não encontrado com esse ISBN.");
        }
    }

    private void confirmarEmprestimoInterativo() {
        System.out.println("--- Confirmar empréstimo ---");
        String isbn = lerTexto("ISBN do livro a emprestar: ", true);
        Livro livro = catalogo.buscarPorIsbn(isbn);
        if (livro == null) {
            System.out.println("Livro não encontrado com esse ISBN.");
            return;
        }
        if (!livro.isDisponivel()) {
            System.out.println("Livro atualmente indisponível para empréstimo.");
            return;
        }

        String nomeTomador = lerTexto("Nome completo do tomador: ", true);
        String celular;
        while (true) {
            celular = lerTexto("Telefone celular (apenas dígitos, sem + ou espaço): ", true);
            if (CELULAR_PATTERN.matcher(celular).matches()) break;
            System.out.println("Número de celular inválido. Informe apenas dígitos (8 a 15 dígitos).");
        }

        String key = normalizeIsbnSimple(isbn);
        EmprestimoRecord reg = new EmprestimoRecord(key, nomeTomador, celular, new Date());
        emprestimosAtivos.put(key, reg);

        livro.setDisponivel(false);

        System.out.println("Empréstimo registrado com sucesso:");
        System.out.println(reg);
    }

    private void confirmarRecebimentoInterativo() {
        System.out.println("--- Confirmar recebimento (devolução) ---");
        String isbn = lerTexto("ISBN do livro sendo devolvido: ", true);
        String celular = lerTexto("Telefone celular do tomador (apenas dígitos): ", true);
        if (!CELULAR_PATTERN.matcher(celular).matches()) {
            System.out.println("Formato de celular inválido. Operação abortada.");
            return;
        }

        String key = normalizeIsbnSimple(isbn);
        EmprestimoRecord reg = emprestimosAtivos.get(key);
        if (reg == null) {
            System.out.println("Não existe empréstimo ativo registrado para este ISBN.");
            return;
        }

        if (!reg.celular.equals(celular)) {
            System.out.println("Aviso: o celular fornecido não confere com o registro de empréstimo.");
            String resp = lerTexto("Deseja prosseguir e aceitar a devolução mesmo assim? (s/N): ", false);
            if (!resp.equalsIgnoreCase("s") && !resp.equalsIgnoreCase("sim")) {
                System.out.println("Devolução cancelada. Verifique os dados com o tomador.");
                return;
            }
        }

        Livro livro = catalogo.buscarPorIsbn(isbn);
        if (livro != null) {
            livro.setDisponivel(true);
        } else {
            System.out.println("Atenção: o livro não foi encontrado no catálogo (talvez foi removido).");
        }

        emprestimosAtivos.remove(key);

        System.out.println("Devolução registrada com sucesso.");
    }

    private void listarLivrosResumo() {
        System.out.println("--- Lista resumida de livros (amostra) ---");
        String rel = catalogo.gerarRelatorioEstatisticas();
        System.out.println(rel);
        System.out.println("Para detalhes, use a listagem por ISBN/consulta no sistema.");
    }

    private void listarEmprestimosAtivos() {
        System.out.println("--- Empréstimos ativos ---");
        if (emprestimosAtivos.isEmpty()) {
            System.out.println("Nenhum empréstimo ativo registrado.");
            return;
        }
        emprestimosAtivos.values().forEach(System.out::println);
    }

    // ----------------------------
    // Utilitários internos
    // ----------------------------
    private static String normalizeIsbnSimple(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[\\s-]", "");
    }

    // ----------------------------
    // Classe interna para representar registro de empréstimo
    // ----------------------------
    private static class EmprestimoRecord {
        final String isbn;
        final String nome;
        final String celular;
        final Date dataHora;

        EmprestimoRecord(String isbn, String nome, String celular, Date dataHora) {
            this.isbn = isbn;
            this.nome = nome;
            this.celular = celular;
            this.dataHora = (Date) dataHora.clone();
        }

        @Override
        public String toString() {
            return String.format("Emprestimo{ISBN='%s', Nome='%s', Celular='%s', Data='%s'}",
                    isbn, nome, celular, dataHora.toString());
        }
    }
}
