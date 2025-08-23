import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Versão otimizada da GUI do catálogo que recebe explicitamente as dependências
 * CatalogoLivros e um LivroFactory para criar instâncias de Livro.
 *
 * Motivação:
 * - Elimina a dependência por reflection na lógica principal da UI.
 * - Permite injeção de dependência (testabilidade e controle das assinaturas de Livro).
 * - Mantém flexibilidade por meio da interface LivroFactory — adapte-a ao seu construtor de Livro.
 *
 * Uso típico:
 *   CatalogoLivros catalogo = new CatalogoLivros();
 *   LivroFactory factory = (isbn, titulo, autor, autoresSec, categoria) -> new Livro(isbn, titulo, autor, autoresSec, categoria);
 *   new CatalogoLivrosGUI(catalogo, factory);
 *
 * Observação: o exemplo acima pressupõe que exista um construtor Livro(String, String, String, List<String>, String).
 * Se o seu Livro tiver outra assinatura, implemente o LivroFactory adequadamente (lambda ou classe anônima).
 */
public class CatalogoLivrosGUI extends JFrame {
    // ---------------------- Dependências injetadas ----------------------
    private final CatalogoLivros catalogo;
    private final LivroFactory livroFactory;

    // ---------------------- Componentes UI ----------------------
    private final JTextField isbnField = new JTextField(20);
    private final JTextField tituloField = new JTextField(30);
    private final JTextField autorField = new JTextField(30);
    private final JTextField autoresSecField = new JTextField(30);
    private final JTextField categoriaField = new JTextField(20);

    private final JComboBox<String> searchType = new JComboBox<>(new String[]{"ISBN", "Autor", "Título", "Categoria"});
    private final JTextField searchField = new JTextField(30);
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTextArea statsArea = new JTextArea(8, 60);

    /**
     * Interface de fábrica para criar objetos Livro com a assinatura desejada.
     * Implemente conforme sua classe Livro.
     */
    public interface LivroFactory {
        Livro create(String isbn, String titulo, String autor, List<String> autoresSecundarios, String categoria);
    }

    /**
     * Construtor principal — injetar o catálogo e a fábrica de Livro.
     */
    public CatalogoLivrosGUI(CatalogoLivros catalogo, LivroFactory factory) {
        super("Catálogo de Livros — GUI (injeção)");
        if (catalogo == null) throw new IllegalArgumentException("catalogo não pode ser null");
        if (factory == null) throw new IllegalArgumentException("factory não pode ser null");
        this.catalogo = catalogo;
        this.livroFactory = factory;

        initLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Helper: inicializa e organiza os componentes da interface.
     */
    private void initLayout() {
        JPanel main = new JPanel(new BorderLayout(8, 8));

        // --- Formulário de adição ---
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, c, row++, "ISBN:", isbnField);
        addRow(form, c, row++, "Título:", tituloField);
        addRow(form, c, row++, "Autor principal:", autorField);
        addRow(form, c, row++, "Autores secundários (vírgula):", autoresSecField);
        addRow(form, c, row++, "Categoria:", categoriaField);

        JButton addBtn = new JButton("Adicionar Livro");
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        form.add(addBtn, c);
        addBtn.addActionListener(e -> onAddLivro());

        // --- Busca e remoção ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Buscar por:"));
        searchPanel.add(searchType);
        searchPanel.add(searchField);
        JButton searchBtn = new JButton("Buscar");
        searchPanel.add(searchBtn);
        JButton removeBtn = new JButton("Remover por ISBN");
        searchPanel.add(removeBtn);

        searchBtn.addActionListener(e -> onSearch());
        removeBtn.addActionListener(e -> onRemove());

        // --- Tabela de resultados ---
        tableModel.setColumnIdentifiers(new String[]{"ISBN", "Título", "Autor", "Autores Sec.", "Categoria"});
        JTable table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        // --- Estatísticas ---
        statsArea.setEditable(false);
        JScrollPane statsScroll = new JScrollPane(statsArea);
        JButton refreshStats = new JButton("Atualizar Estatísticas");
        refreshStats.addActionListener(ev -> updateStats());

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(searchPanel, BorderLayout.SOUTH);

        main.add(top, BorderLayout.NORTH);
        main.add(tableScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(refreshStats, BorderLayout.NORTH);
        bottom.add(statsScroll, BorderLayout.CENTER);

        main.add(bottom, BorderLayout.SOUTH);

        setContentPane(main);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent comp) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(comp, c);
        c.fill = GridBagConstraints.NONE;
    }

    // ---------------------- Ações ----------------------
    private void onAddLivro() {
        String isbn = isbnField.getText().trim();
        String titulo = tituloField.getText().trim();
        String autor = autorField.getText().trim();
        String autoresSec = autoresSecField.getText().trim();
        String categoria = categoriaField.getText().trim();

        if (isbn.isEmpty() || titulo.isEmpty() || autor.isEmpty()) {
            showError("ISBN, Título e Autor principal são obrigatórios.");
            return;
        }

        List<String> autoresList = autoresSec.isEmpty() ? new ArrayList<>() : Arrays.asList(autoresSec.split("\\s*,\\s*"));
        Livro livro = livroFactory.create(isbn, titulo, autor, autoresList, categoria);

        boolean ok = catalogo.adicionarLivro(livro);
        if (ok) {
            showInfo("Livro adicionado com sucesso.");
            clearForm();
        } else {
            showError("Falha ao adicionar livro (retorno false).");
        }
    }

    private void onSearch() {
        String tipo = (String) searchType.getSelectedItem();
        String termo = searchField.getText().trim();
        if (termo.isEmpty()) { showError("Informe o termo de busca."); return; }

        try {
            List<Livro> resultados = new ArrayList<>();
            switch (tipo) {
                case "ISBN":
                    Livro one = catalogo.buscarPorIsbn(termo);
                    if (one != null) resultados.add(one);
                    break;
                case "Autor":
                    resultados = catalogo.buscarPorAutor(termo);
                    break;
                case "Título":
                    resultados = catalogo.buscarPorTitulo(termo);
                    break;
                case "Categoria":
                    resultados = catalogo.buscarPorCategoria(termo);
                    break;
                default:
                    showError("Tipo de busca desconhecido: " + tipo);
                    return;
            }

            populateTable(resultados);
        } catch (Exception ex) {
            showError("Erro ao buscar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void onRemove() {
        String isbn = JOptionPane.showInputDialog(this, "Informe o ISBN do livro a remover:");
        if (isbn == null || isbn.trim().isEmpty()) return;
        try {
            boolean ok = catalogo.removerPorIsbn(isbn.trim());
            if (ok) showInfo("Remoção concluída (retorno true). Atualize a busca para ver o efeito.");
            else showError("Remoção falhou (retorno false).");
        } catch (Exception ex) {
            showError("Erro ao remover: " + ex.getMessage());
        }
    }

    private void populateTable(List<Livro> resultados) {
        tableModel.setRowCount(0);
        for (Livro livro : resultados) {
            String isbn = safeGet(livro.getIsbn());
            String titulo = safeGet(livro.getTitulo());
            String autor = safeGet(livro.getAutor());
            String autoresSec = (livro.getAutoresSecundarios() == null) ? "" : String.join(", ", livro.getAutoresSecundarios());
            String categoria = safeGet(livro.getCategoria());
            tableModel.addRow(new Object[]{isbn, titulo, autor, autoresSec, categoria});
        }
    }

    private String safeGet(String s) { return s == null ? "" : s; }

    private void updateStats() {
        try {
            String rel = catalogo.gerarRelatorioEstatisticas();
            statsArea.setText(rel == null ? "" : rel);
        } catch (Exception ex) {
            showError("Erro ao gerar relatório: " + ex.getMessage());
        }
    }

    private void clearForm() {
        isbnField.setText("");
        tituloField.setText("");
        autorField.setText("");
        autoresSecField.setText("");
        categoriaField.setText("");
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String msg) { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }

    /**
     * Método exemplo (opcional) que demonstra como instanciar a GUI com injeção.
     * Comentado para evitar erros de compilação caso a assinatura de Livro seja diferente.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Exemplo (descomente e adeque conforme sua classe Livro):
                // CatalogoLivros catalogo = new CatalogoLivros();
                // LivroFactory factory = (isbn, titulo, autor, autoresSec, categoria) -> new Livro(isbn, titulo, autor, autoresSec, categoria);
                // new CatalogoLivrosGUI(catalogo, factory);

                JOptionPane.showMessageDialog(null, "Inicie a GUI a partir da sua aplicação. Exemplo de uso está comentado no código.");

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Erro ao inicializar GUI: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
