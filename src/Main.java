public class Main {
    public static void main(String[] args) {
        BibliotecaCLI app = new BibliotecaCLI();

        // Exemplo de livros de teste (opcional). Em produção, remova ou comente.
        /**try {
            app.adicionarLivroAoCatalogo(new Livro("978-0-123456-47-2", "Java Essencial", "Ana Silva",
                    "Editora X", 2021, "Programação", 450, "Português", true, "Prateleira A1"));
            app.adicionarLivroAoCatalogo(new Livro("9780123456489", "Algoritmos Avançados", "Bruno Souza",
                    "Editora Y", 2018, "Ciência da Computação", 320, "Português", true, "Prateleira B2"));
        } catch (Exception e) {
            System.out.println("Aviso (exemplo): não foi possível inserir livro de exemplo: " + e.getMessage());
        }
        */
        // Inicia a interface interativa
        app.run();

        // Fecha recursos
        app.fechar();
    }
}