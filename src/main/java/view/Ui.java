package view;

import domain.Orcamento;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Optional;
import java.util.function.Function;

/** Funções de apoio comuns às vistas (estilo, diálogos, formatação). */
public final class Ui {

    private static final String CSS = Ui.class.getResource("/estilo.css").toExternalForm();

    static {
        // Tipo de letra do site (Montserrat); se falhar, o CSS usa Segoe UI
        for (String f : new String[]{"Regular", "Bold", "ExtraBold"}) {
            try (InputStream in = Ui.class.getResourceAsStream("/fonts/Montserrat-" + f + ".otf")) {
                if (in != null) {
                    javafx.scene.text.Font.loadFont(in, 12);
                }
            } catch (Exception ignorado) {
                // sem o tipo de letra, usa-se o do sistema
            }
        }
    }

    private Ui() {
    }

    // ------------------------------------------------------------------ janelas

    public static Scene cena(Parent raiz, double largura, double altura) {
        Scene s = new Scene(raiz, largura, altura);
        s.getStylesheets().add(CSS);
        return s;
    }

    private static void estilizar(Dialog<?> d, Window dono) {
        if (dono != null) {
            d.initOwner(dono);
        }
        d.getDialogPane().getStylesheets().add(CSS);
    }

    public static void erro(Window dono, String mensagem) {
        Alert a = new Alert(Alert.AlertType.ERROR,
                mensagem == null || mensagem.isBlank() ? "Ocorreu um erro." : mensagem, ButtonType.OK);
        a.setTitle("Erro");
        a.setHeaderText(null);
        estilizar(a, dono);
        a.showAndWait();
    }

    public static void info(Window dono, String mensagem) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, mensagem, ButtonType.OK);
        a.setTitle("Informação");
        a.setHeaderText(null);
        estilizar(a, dono);
        a.showAndWait();
    }

    public static boolean confirmar(Window dono, String mensagem) {
        ButtonType sim = new ButtonType("Sim", ButtonBar.ButtonData.OK_DONE);
        ButtonType nao = new ButtonType("Não", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensagem, sim, nao);
        a.setTitle("Confirmar");
        a.setHeaderText(null);
        estilizar(a, dono);
        return a.showAndWait().filter(b -> b == sim).isPresent();
    }

    /** Diálogo com botões "textoOk" e "Cancelar". O primeiro botão é o de confirmação. */
    public static Dialog<ButtonType> dialogo(Window dono, String titulo, Node conteudo, String textoOk) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(titulo);
        estilizar(d, dono);
        d.getDialogPane().setContent(conteudo);
        ButtonType ok = new ButtonType(textoOk, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        botaoOk(d).getStyleClass().add("botao-primario");
        return d;
    }

    public static Button botaoOk(Dialog<ButtonType> d) {
        return (Button) d.getDialogPane().lookupButton(d.getDialogPane().getButtonTypes().get(0));
    }

    /** Executa {@code acao} ao carregar em OK; se lançar exceção, mostra o erro e mantém o diálogo aberto. */
    public static void aoConfirmar(Dialog<ButtonType> d, Runnable acao) {
        botaoOk(d).addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                acao.run();
            } catch (RuntimeException ex) {
                erro(d.getDialogPane().getScene().getWindow(), mensagem(ex));
                ev.consume();
            }
        });
    }

    public static boolean confirmado(Optional<ButtonType> r) {
        return r.isPresent() && r.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    public static String mensagem(RuntimeException ex) {
        if (ex instanceof NumberFormatException) {
            return "Valor numérico inválido.";
        }
        return ex.getMessage();
    }

    // ------------------------------------------------------------------ widgets

    public static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo-pagina");
        return l;
    }

    public static Label seccao(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("seccao-titulo");
        return l;
    }

    public static Label rotulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("rotulo");
        return l;
    }

    public static Region espaco() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Button primario(String texto) {
        Button b = new Button(texto);
        b.getStyleClass().add("botao-primario");
        return b;
    }

    public static Button secundario(String texto) {
        Button b = new Button(texto);
        b.getStyleClass().add("botao-secundario");
        return b;
    }

    public static Button perigo(String texto) {
        Button b = new Button(texto);
        b.getStyleClass().add("botao-perigo");
        return b;
    }

    public static Button link(String texto) {
        Button b = new Button(texto);
        b.getStyleClass().add("botao-link");
        return b;
    }

    public static TextField inline(String texto, String dica) {
        TextField t = new TextField(texto == null ? "" : texto);
        t.setPromptText(dica);
        t.getStyleClass().add("campo-inline");
        return t;
    }

    /** Logótipo com letras brancas, para fundos escuros (barra do topo). */
    public static ImageView logotipoBranco(double largura) {
        return carregarLogo("/logo_branco.png", largura);
    }

    public static ImageView logotipo(double largura) {
        return carregarLogo("/logo.png", largura);
    }

    private static ImageView carregarLogo(String recurso, double largura) {
        try (InputStream in = Ui.class.getResourceAsStream(recurso)) {
            ImageView v = new ImageView(new Image(in));
            v.setFitWidth(largura);
            v.setPreserveRatio(true);
            v.setSmooth(true);
            return v;
        } catch (Exception e) {
            return new ImageView();
        }
    }

    // ------------------------------------------------- formulário em secções (estilo "cartão")

    /** Título de secção com linha por baixo (ex.: «Informações básicas»). */
    public static javafx.scene.layout.VBox secaoForm(String titulo) {
        Label l = new Label(titulo);
        l.getStyleClass().add("form-secao");
        Region regra = new Region();
        regra.getStyleClass().add("form-regra");
        regra.setMinHeight(1);
        regra.setPrefHeight(1);
        javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(4, l, regra);
        v.setPadding(new Insets(10, 0, 4, 0));
        return v;
    }

    /** Campo com o rótulo por cima; {@code obrigatorio} acrescenta o asterisco vermelho. */
    public static javafx.scene.layout.VBox campoForm(String rotulo, boolean obrigatorio, Node campo) {
        Label l = new Label(rotulo);
        l.getStyleClass().add("form-rotulo");
        HBox topo = new HBox(2, l);
        if (obrigatorio) {
            Label a = new Label("*");
            a.getStyleClass().add("form-asterisco");
            topo.getChildren().add(a);
        }
        javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(5, topo, campo);
        if (campo instanceof Region) {
            ((Region) campo).setMaxWidth(Double.MAX_VALUE);
        }
        return v;
    }

    /** Grelha de duas colunas iguais para os campos de um formulário. */
    public static GridPane gridForm() {
        GridPane g = new GridPane();
        g.setHgap(26);
        g.setVgap(14);
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);
        return g;
    }

    /** Grelha de formulário: coluna de rótulos + coluna de campos. */
    public static GridPane grelha() {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(10);
        g.setPadding(new Insets(4, 0, 4, 0));
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(150);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(260);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    public static void linha(GridPane g, int linha, String rotulo, Node campo) {
        g.add(rotulo(rotulo), 0, linha);
        g.add(campo, 1, linha);
    }

    public static <T> TableColumn<T, String> coluna(String titulo, Function<T, String> valor, double largura) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyStringWrapper(valor.apply(cd.getValue())));
        c.setPrefWidth(largura);
        return c;
    }

    /** Coluna de largura fixa (os restantes campos ficam com o espaço que sobra, sem cortar o texto). */
    public static <S, T> TableColumn<S, T> fixa(TableColumn<S, T> c, double largura) {
        c.setMinWidth(largura);
        c.setPrefWidth(largura);
        c.setMaxWidth(largura);
        return c;
    }

    // ---------------------------------------------------------------- formatação

    /** Datas no formato português (dd/MM/aaaa) em qualquer computador. */
    public static void formatoData(javafx.scene.control.DatePicker dp) {
        final java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dp.setConverter(new javafx.util.StringConverter<java.time.LocalDate>() {
            @Override
            public String toString(java.time.LocalDate d) {
                return d == null ? "" : d.format(f);
            }

            @Override
            public java.time.LocalDate fromString(String s) {
                try {
                    return s == null || s.isBlank() ? null : java.time.LocalDate.parse(s.trim(), f);
                } catch (java.time.format.DateTimeParseException e) {
                    return null;
                }
            }
        });
    }

    /** Aceita "14,50", "14.50", "1.234,50" e "14,50 €". */
    public static BigDecimal paraDecimal(String texto) {
        if (texto == null) {
            throw new NumberFormatException("vazio");
        }
        String t = texto.replaceAll("[^0-9,.\\-]", "");
        if (t.contains(",")) {
            t = t.replace(".", "").replace(',', '.');
        }
        return new BigDecimal(t);
    }

    public static String semZeros(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    // ----------------------------------------------------------------------- PDF

    /** Pede onde guardar e gera o PDF do orçamento. */
    public static void guardarPdf(Contexto ctx, Orcamento orc, Window dono) {
        String nomeCliente = Normalizer.normalize(orc.getCliente().getNome(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "_");
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF do orçamento");
        fc.setInitialFileName("Orcamento_" + orc.getNumero().replace('/', '-') + "_" + nomeCliente + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(dono);
        if (f == null) {
            return;
        }
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            f = new File(f.getParentFile(), f.getName() + ".pdf");
        }
        try {
            ctx.orcamentos.gerarPdf(orc, f.toPath());
        } catch (RuntimeException ex) {
            erro(dono, mensagem(ex));
            return;
        }
        ButtonType abrir = new ButtonType("Abrir PDF", ButtonBar.ButtonData.LEFT);
        Alert a = new Alert(Alert.AlertType.INFORMATION, "PDF guardado em:\n" + f.getAbsolutePath(),
                abrir, ButtonType.OK);
        a.setTitle("PDF gerado");
        a.setHeaderText(null);
        estilizar(a, dono);
        final String uri = f.toURI().toString();
        a.showAndWait().filter(b -> b == abrir).ifPresent(b -> ctx.host.showDocument(uri));
    }
}
