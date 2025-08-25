import javax.swing.SwingUtilities;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Main corrigida que inicializa o CatalogoLivros e injeta um LivroFactory funcional na GUI.
 *
 * Correções implementadas:
 * - Removida tentativa de construtor no-arg inexistente
 * - Adicionada implementação para construtor com 10 parâmetros
 * - Melhorada lógica de fallback para diferentes assinaturas
 * - Adicionado tratamento de exceções mais específico
 */
public class Main {

    public static void main(String[] args) {
        // Instância do catálogo (assume construtor padrão)
        CatalogoLivros catalogo = new CatalogoLivros();

        // Factory corrigida: mapeia parâmetros da GUI para construtor real de Livro
        CatalogoLivrosGUI.LivroFactory factory = (isbn, titulo, autor, autoresSecundarios, categoria) -> {
            try {
                Class<?> livroCls = Class.forName("Livro");

                // 1) Tenta construtor completo (10 parâmetros) - PRIORIDADE MÁXIMA
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 10
                            && params[0] == String.class  // isbn
                            && params[1] == String.class  // titulo
                            && params[2] == String.class  // autor
                            && params[3] == String.class  // editora
                            && params[4] == int.class     // anoPublicacao
                            && params[5] == String.class  // categoria
                            && params[6] == int.class     // numeroPaginas
                            && params[7] == String.class  // idioma
                            && params[8] == boolean.class // disponivel
                            && params[9] == String.class) // localizacao
                    {
                        // Mapear parâmetros da GUI para construtor real
                        String editora = "";  // valor padrão
                        int anoPublicacao = java.time.LocalDate.now().getYear(); // ano atual como padrão
                        int numeroPaginas = 0; // valor padrão
                        String idioma = "Português"; // valor padrão
                        boolean disponivel = true; // padrão disponível
                        String localizacao = ""; // valor padrão
                        
                        return (Livro) ctor.newInstance(isbn, titulo, autor, editora,
                                anoPublicacao, categoria != null ? categoria : "Não categorizado", 
                                numeroPaginas, idioma, disponivel, localizacao);
                    }
                }

                // 2) Tenta construtor reduzido com 5 parâmetros (se existisse)
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 5
                            && params[0] == String.class
                            && params[1] == String.class
                            && params[2] == String.class
                            && java.util.List.class.isAssignableFrom(params[3])
                            && params[4] == String.class) {
                        return (Livro) ctor.newInstance(isbn, titulo, autor, 
                                autoresSecundarios == null ? new ArrayList<>() : autoresSecundarios, categoria);
                    }
                }

                // 3) Tenta construtor com 4 parâmetros (se existisse)
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 4
                            && params[0] == String.class
                            && params[1] == String.class
                            && params[2] == String.class
                            && params[3] == String.class) {
                        String autoresJoined = (autoresSecundarios == null || autoresSecundarios.isEmpty()) ? 
                                "" : String.join(", ", autoresSecundarios);
                        return (Livro) ctor.newInstance(isbn, titulo, autor, categoria == null ? autoresJoined : categoria);
                    }
                }

                // 4) Tenta construtor com 3 parâmetros (se existisse)
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 3
                            && params[0] == String.class
                            && params[1] == String.class
                            && params[2] == String.class) {
                        return (Livro) ctor.newInstance(isbn, titulo, autor);
                    }
                }

                // REMOVIDA: tentativa de construtor no-arg que causava o erro
                // Se chegou até aqui, nenhum construtor compatível foi encontrado
                throw new RuntimeException("Nenhum construtor compatível encontrado na classe Livro. " +
                        "Construtores disponíveis: " + java.util.Arrays.toString(livroCls.getConstructors()));

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Classe Livro não encontrada no classpath: " + e.getMessage(), e);
            } catch (Exception e) {
                // Log detalhado para debug
                System.err.println("Erro detalhado na criação de Livro:");
                System.err.println("ISBN: " + isbn);
                System.err.println("Título: " + titulo);
                System.err.println("Autor: " + autor);
                System.err.println("Categoria: " + categoria);
                e.printStackTrace();
                
                throw new RuntimeException("Falha ao criar instância de Livro: " + e.getMessage(), e);
            }
        };

        // Inicialização da GUI no thread apropriado
        SwingUtilities.invokeLater(() -> {
            try {
                new CatalogoLivrosGUI(catalogo, factory);
            } catch (Exception e) {
                System.err.println("Erro crítico na inicialização da GUI:");
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    /**
     * Método utilitário mantido para compatibilidade (não utilizado na solução principal)
     */
    private static boolean tryInvokeSetter(Class<?> targetClass, Object targetInstance, String methodName, Class<?> paramType, Object value) {
        try {
            for (Method m : targetClass.getMethods()) {
                if (!m.getName().equals(methodName) || m.getParameterCount() != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (paramType == String.class && p == String.class) {
                    m.invoke(targetInstance, value == null ? null : value.toString());
                    return true;
                }
                if (paramType == java.util.List.class && java.util.Collection.class.isAssignableFrom(p)) {
                    m.invoke(targetInstance, value == null ? new ArrayList<>() : value);
                    return true;
                }
                if (p.isAssignableFrom(paramType)) {
                    m.invoke(targetInstance, value);
                    return true;
                }
            }
        } catch (Exception ignored) { 
            // Log silencioso - método de fallback
        }
        return false;
    }
}
