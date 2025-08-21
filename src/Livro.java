// =====================================================
// SISTEMA DE CATALOGAÇÃO DE BIBLIOTECA DIGITAL
// Desenvolvido p/ otimização de gerenciamento de livros
// =====================================================

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

/**
 * Classe que representa um livro no sistema.
 *
 * Comentários:
 * - Cada instância de Livro contém os campos básicos que descrevem um livro.
 * - Os getters e setters fazem validações mínimas (por exemplo, ano de publicação).
 * - equals() e hashCode() baseiam-se no ISBN, assumindo que ISBN identifica unicamente um livro.
 *
 * Contém todos os atributos essenciais p/ catalogação.
 */
public class Livro {
    // Atributos principais - usando tipos apropriados p/ cada campo
    private final String isbn;                      // Identificador único internacional
    private String titulo;                          // Título completo do livro
    private String autor;                           // Autor principal
    private final List<String> autoresSecundarios;  // P/ livros com múltiplos autores
    private String editora;                         // Editora
    private int anoPublicacao;                      // Ano de publicação
    private String categoria;                       // Categoria/Gênero
    private int numeroPaginas;                      // Número de páginas
    private String idioma;                          // Idioma
    private boolean disponivel;                     // Status de disponibilidade
    private final LocalDateTime dataCadastro;       // Data/hora de cadastro
    private String localizacao;                     // Localização física ou digital

    /**
     * Construtor principal.
     *
     * Regras/validações importantes:
     * - 'isbn' e 'titulo' são obrigatórios (não podem ser nulos/empty).
     *
     */
    public Livro(String isbn, String titulo, String autor, String editora,
                 int anoPublicacao, String categoria, int numeroPaginas,
                 String idioma, boolean disponivel, String localizacao) {
        // Validações básicas p/ evitar dados inconsistentes
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN é obrigatório");
        }
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("Título é obrigatório");
        }

        // Normalização básica
        this.isbn = normalizeIsbn(isbn);
        this.titulo = titulo.trim();
        this.autor = autor != null ? autor.trim() : "";
        this.autoresSecundarios = new ArrayList<>();
        this.editora = editora != null ? editora.trim() : "";
        this.setAnoPublicacao(anoPublicacao); // chama setter com validação
        this.categoria = categoria != null ? categoria.trim() : "Não categorizado";
        this.numeroPaginas = Math.max(0, numeroPaginas); // evita valores negativos
        this.idioma = idioma != null ? idioma.trim() : "Desconhecido";
        this.disponivel = disponivel;
        this.dataCadastro = LocalDateTime.now();
        this.localizacao = localizacao != null ? localizacao.trim() : "";
    }

    // ----------------------------
    // Métodos utilitários privados
    // ----------------------------
    private static String normalizeIsbn(String raw) {
        // Remove espaços e hífens p/ armazenamento consistente
        return raw.replaceAll("[\\s-]", "");
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // Getters/Setters com validações apropriadas
    /** Getter acessa o valor do atributo privado, retornando público e lendo-o
     *  de forma que não modifique o atributo (seguro)
     *  Setter controla a modificação do atributo, validando-o antes de aceitar
     *  novo valor. Ponto único de controle de mudanças.
     */
    public String getIsbn() {
        return isbn;

    }

    public String getTitulo() {
        return titulo;

    }
    public void setTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("Título não pode ser vazio");
        }
        this.titulo = titulo.trim();
    }

    public String getAutor() {
        return autor;

    }
    public void setAutor(String autor) {
        this.autor = autor != null ? autor.trim() : "";
    }

    public List<String> getAutoresSecundarios() {
        // Retorna cópia defensiva p/ impedir alterações externas na lista interna
        return new ArrayList<>(autoresSecundarios);
    }

    public void adicionarAutorSecundario(String autor) {
        if (autor != null && !autor.trim().isEmpty()) {
            this.autoresSecundarios.add(autor.trim());
        }
    }

    public String getEditora() {
        return editora;

    }
    public void setEditora(String editora) {
        this.editora = editora != null ? editora.trim() : "";
    }

    public int getAnoPublicacao() {
        return anoPublicacao;

    }
    public void setAnoPublicacao(int anoPublicacao) {
        // Validação básica: não aceita anos futuros ou muito antigos
        int anoAtual = java.time.LocalDate.now().getYear();
        if (anoPublicacao < 1000 || anoPublicacao > anoAtual + 1) {
            throw new IllegalArgumentException("Ano de publicação inválido");
        }
        this.anoPublicacao = anoPublicacao;
    }

    public String getCategoria() {
        return categoria;

    }
    public void setCategoria(String categoria) {
        this.categoria = categoria != null ? categoria.trim() : "Não categorizado";
    }

    public boolean isDisponivel() {
        return disponivel;

    }
    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public int getNumeroPaginas() {
        return numeroPaginas;
    }
    public void setNumeroPaginas(int numeroPaginas) {
        this.numeroPaginas = Math.max(0, numeroPaginas);
    }

    public String getIdioma() {
        return idioma;
    }
    public void setIdioma(String idioma) {
        this.idioma = idioma != null ? idioma.trim() : "Desconhecido";
    }

    public String getLocalizacao() {
        return localizacao;
    }
    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao != null ? localizacao.trim() : "";
    }

    // Método para obter todos os autores (principal + secundários)
    public List<String> getTodosAutores() {
        List<String> todosAutores = new ArrayList<>();
        todosAutores.add(this.autor);
        todosAutores.addAll(this.autoresSecundarios);
        return todosAutores;
    }

    // Override dos métodos equals e hashCode baseados no ISBN
    // Isso é crucial p/ evitar duplicatas no sistema
    @Override
    public boolean equals(Object obj) {
        // Dois livros são iguais se o ISBN (normalizado) for igual.
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Livro other = (Livro) obj;
        return Objects.equals(this.isbn, other.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    // toString p/ facilitar debug e logs
    @Override
    public String toString() {
        return String.format("Livro{ISBN='%s', Título='%s', Autor='%s', Disponível='%s'}",
                isbn, titulo, autor, String.valueOf(disponivel));
    }
} 