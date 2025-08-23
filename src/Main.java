import javax.swing.SwingUtilities;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Main que inicializa o CatalogoLivros e injeta um LivroFactory adaptável na GUI.
 *
 * Observações:
 * - Mantém dependência tipada de CatalogoLivros e Livro (necessário que essas classes
 *   existam no mesmo package/classpath).
 * - A implementação de LivroFactory usa reflexão internamente para tentar diferentes
 *   construtores ou, se necessário, usar no-arg + setters. Assim, é compatível com
 *   variações de assinatura de Livro.
 *
 * Se seu projeto usa pacote (ex: package br.com.exemplo;), adicione a linha de package no topo.
 */
public class Main {

    public static void main(String[] args) {
        // Crie sua instância de catálogo (assume construtor padrão)
        CatalogoLivros catalogo = new CatalogoLivros();

        // Fábrica de Livro: tenta criar instancia de Livro usando reflex ao detectar assinatura disponível
        CatalogoLivrosGUI.LivroFactory factory = (isbn, titulo, autor, autoresSecundarios, categoria) -> {
            try {
                Class<?> livroCls = Class.forName("Livro");

                // 1) Tenta construtor (String, String, String, List<String>, String)
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 5
                            && params[0] == String.class
                            && params[1] == String.class
                            && params[2] == String.class
                            && java.util.List.class.isAssignableFrom(params[3])
                            && params[4] == String.class) {
                        return (Livro) ctor.newInstance(isbn, titulo, autor, autoresSecundarios == null ? new ArrayList<>() : autoresSecundarios, categoria);
                    }
                }

                // 2) Tenta construtor (String, String, String, String) - autores secundários como String
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 4
                            && params[0] == String.class
                            && params[1] == String.class
                            && params[2] == String.class
                            && params[3] == String.class) {
                        String autoresJoined = (autoresSecundarios == null || autoresSecundarios.isEmpty()) ? "" : String.join(", ", autoresSecundarios);
                        return (Livro) ctor.newInstance(isbn, titulo, autor, categoria == null ? autoresJoined : categoria);
                    }
                }

                // 3) Tenta construtor (String, String, String)
                for (Constructor<?> ctor : livroCls.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 3
                            && params[0] == String.class
                            && params[1] == String.class
                            && params[2] == String.class) {
                        return (Livro) ctor.newInstance(isbn, titulo, autor);
                    }
                }

                // 4) Tenta no-arg + setters (setIsbn, setTitulo, setAutor, setAutoresSecundarios, setCategoria)
                Object inst = livroCls.getDeclaredConstructor().newInstance();
                tryInvokeSetter(livroCls, inst, "setIsbn", String.class, isbn);
                tryInvokeSetter(livroCls, inst, "setTitulo", String.class, titulo);
                tryInvokeSetter(livroCls, inst, "setAutor", String.class, autor);

                // autores secundários: tenta set(List) ou set(String)
                if (autoresSecundarios != null) {
                    boolean setOk = tryInvokeSetter(livroCls, inst, "setAutoresSecundarios", java.util.List.class, autoresSecundarios);
                    if (!setOk) {
                        tryInvokeSetter(livroCls, inst, "setAutoresSecundarios", String.class, String.join(", ", autoresSecundarios));
                    }
                }

                tryInvokeSetter(livroCls, inst, "setCategoria", String.class, categoria);

                return (Livro) inst;

            } catch (Exception e) {
                // Transformer em runtime exception para não exigir checked exception no lambda
                throw new RuntimeException("Falha ao criar instância de Livro via reflexão: " + e.getMessage(), e);
            }
        };

        // Inicializa GUI no EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> new CatalogoLivrosGUI(catalogo, factory));
    }

    /**
     * Tenta invocar um setter com o nome e tipo indicado; retorna true se invocado com sucesso.
     */
    private static boolean tryInvokeSetter(Class<?> targetClass, Object targetInstance, String methodName, Class<?> paramType, Object value) {
        try {
            // procura método compatível (aceita hierarquia)
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
        } catch (Exception ignored) { }
        return false;
    }
}
