import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * Catálogo de livros com estruturas otimizadas para buscas por ISBN, autor e categoria
 *
 * Observações:
 * - Uso de coleções concorrentes (ConcurrentHashMap / CopyOnWriteArrayList) p/ permitir
 *   acesso seguro em ambientes multithread sem bloquear demais
 * - Chaves normalizadas (ISBN, autor, categoria) no momento da inserção p/ evitar
 *   chamadas repetidas a toLowerCase() em buscas
 * - Métodos públicos documentados abaixo explicam entrada/saída e comportamento.
 */
public class CatalogoLivros {
    private static final Logger LOGGER = Logger.getLogger(CatalogoLivros.class.getName());

    // Mapas p/ acesso rápido
    // Chaves já normalizadas
    private final Map<String, Livro> livrosPorIsbn;                 // ISBN normalizado
    private final Map<String, Set<Livro>> livrosPorAutor;           // autor normalizado
    private final Map<String, Set<Livro>> livrosPorCategoria;       // categoria normalizada
    private final List<Livro> todosLivros;                          // mantém ordem de inserção

    public CatalogoLivros() {
        this.livrosPorIsbn = new ConcurrentHashMap<>();
        this.livrosPorAutor = new ConcurrentHashMap<>();
        this.livrosPorCategoria = new ConcurrentHashMap<>();
        // CopyOnWriteArrayList para leitura frequente
        // usar Collections.synchronizedList(new ArrayList<>()) p/ muitos writes
        this.todosLivros = new CopyOnWriteArrayList<>();
    }

    // Normaliza chaves para uso em mapas
    private static String normalizeKey(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replaceAll("[\\s-]", "");
    }

    /**
     * Adiciona um livro ao catálogo
     * '@param' livro instância de Livro (não nula)
     * '@return' true se adicionado com sucesso; false se ISBN já existe
     */
    public boolean adicionarLivro(Livro livro) {
        if (livro == null) throw new IllegalArgumentException("Livro não pode ser nulo");

        String isbnNorm = normalizeIsbn(livro.getIsbn());

        // putIfAbsent garante atomicidade para a entrada por ISBN
        Livro existente = livrosPorIsbn.putIfAbsent(isbnNorm, livro);
        if (existente != null) {
            LOGGER.warning("Já existe livro com ISBN: " + livro.getIsbn());
            return false;
        }

        // adiciona à lista principal
        todosLivros.add(livro);

        // indexa por autor (autor principal + autores secundários)
        String autorPrincipalNorm = normalizeKey(livro.getAutor());
        livrosPorAutor.computeIfAbsent(autorPrincipalNorm, k -> ConcurrentHashMap.newKeySet()).add(livro);

        for (String autorSec : livro.getAutoresSecundarios()) {
            String aNorm = normalizeKey(autorSec);
            livrosPorAutor.computeIfAbsent(aNorm, k -> ConcurrentHashMap.newKeySet()).add(livro);
        }

        // indexa por categoria
        String catNorm = normalizeKey(livro.getCategoria());
        livrosPorCategoria.computeIfAbsent(catNorm, k -> ConcurrentHashMap.newKeySet()).add(livro);

        return true;
    }

    /**
     * Busca por ISBN.
     * '@param' isbn ISBN (pode conter hífens/espaços)
     * '@return' Livro ou null se não encontrado
     */
    public Livro buscarPorIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) return null;
        return livrosPorIsbn.get(normalizeIsbn(isbn));
    }

    /**
     * Busca por autor (case-insensitive, busca exata de nome)
     * '@param' nomeAutor Nome ou parte do nome do autor
     * '@return' lista (possivelmente vazia) com resultados
     */
    public List<Livro> buscarPorAutor(String nomeAutor) {
        if (nomeAutor == null || nomeAutor.trim().isEmpty()) return Collections.emptyList();

        // busca simples por chave normalizada (exatidão no nome)
        String chave = normalizeKey(nomeAutor);
        Set<Livro> conjunto = livrosPorAutor.get(chave);
        if (conjunto != null) {
            return new ArrayList<>(conjunto);
        }

        // busca parcial (menos performática)
        String termo = chave;
        return todosLivros.stream()
                .filter(l -> l.getAutor() != null && l.getAutor().toLowerCase(Locale.ROOT).contains(termo))
                .collect(Collectors.toList());
    }

    /**
     * Busca por título (busca parcial, case-insensitive)
     */
    public List<Livro> buscarPorTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final String termo = titulo.toLowerCase(Locale.ROOT).trim();
        return todosLivros.stream()
                .filter(livro -> livro.getTitulo().toLowerCase(Locale.ROOT).contains(termo))
                .collect(Collectors.toList());
    }

    /**
     * Busca por categoria.
     */
    public List<Livro> buscarPorCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) return Collections.emptyList();
        Set<Livro> conjunto = livrosPorCategoria.get(normalizeKey(categoria));
        return conjunto != null ? new ArrayList<>(conjunto) : Collections.emptyList();
    }

    /**
     * Remove livro do catálogo por ISBN.
     * '@param' isbn ISBN do livro a remover
     * '@return' true se removeu; false se não encontrado
     */
    public boolean removerPorIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) return false;
        String isbnNorm = normalizeIsbn(isbn);
        Livro removido = livrosPorIsbn.remove(isbnNorm);
        if (removido == null) return false;

        // remove da lista principal
        todosLivros.remove(removido);

        // remove dos índices por autor
        for (String autor : removido.getAutoresSecundarios()) {
            Set<Livro> s = livrosPorAutor.get(normalizeKey(autor));
            if (s != null) {
                s.remove(removido);
                if (s.isEmpty()) livrosPorAutor.remove(normalizeKey(autor));
            }
        }
        // autor principal
        Set<Livro> sAutorPrincipal = livrosPorAutor.get(normalizeKey(removido.getAutor()));
        if (sAutorPrincipal != null) {
            sAutorPrincipal.remove(removido);
            if (sAutorPrincipal.isEmpty()) livrosPorAutor.remove(normalizeKey(removido.getAutor()));
        }

        // remove da categoria
        Set<Livro> sCat = livrosPorCategoria.get(normalizeKey(removido.getCategoria()));
        if (sCat != null) {
            sCat.remove(removido);
            if (sCat.isEmpty()) livrosPorCategoria.remove(normalizeKey(removido.getCategoria()));
        }

        return true;
    }

    /**
     * Gera relatório estatístico simples.
     */
    public String gerarRelatorioEstatisticas() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RELATÓRIO DO CATÁLOGO ===\n");
        sb.append("Total de livros: ").append(todosLivros.size()).append("\n");
        sb.append("Total de autores indexados: ").append(livrosPorAutor.size()).append("\n");
        sb.append("Total de categorias: ").append(livrosPorCategoria.size()).append("\n");

        sb.append("Melhores categorias (amostra):\n");
        livrosPorCategoria.entrySet().stream()
                .limit(10)
                .forEach(e -> sb.append(" - ").append(e.getKey()).append(": ").append(e.getValue().size()).append("\n"));
        return sb.toString();
    }
}
