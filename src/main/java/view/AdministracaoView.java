package view;

import domain.Empresa;
import domain.Utilizador;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/** Configuração do sistema (US02) e gestão de utilizadores (US03). Só para o Administrador. */
public class AdministracaoView extends ScrollPane {

    private final Contexto ctx;
    private final TableView<Utilizador> tabelaUtilizadores = new TableView<>();

    public AdministracaoView(Contexto ctx) {
        this.ctx = ctx;
        VBox pagina = new VBox(16, Ui.titulo("Administração"), cartaoEmpresa(), cartaoParametros(),
                cartaoUtilizadores());
        pagina.getStyleClass().add("pagina");
        setContent(pagina);
        setFitToWidth(true);
        getStyleClass().add("ecra");
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private VBox cartao(String titulo, javafx.scene.Node... conteudo) {
        VBox v = new VBox(12, Ui.seccao(titulo));
        v.getChildren().addAll(conteudo);
        v.getStyleClass().addAll("cartao", "papel");
        v.setMaxWidth(900);
        return v;
    }

    // ------------------------------------------------------------------ empresa

    private VBox cartaoEmpresa() {
        Empresa e = ctx.admin.getEmpresa();
        TextField designacao = new TextField(e.getDesignacao());
        TextField nif = new TextField(e.getNif());
        TextField morada = new TextField(e.getMorada());
        TextField telefone = new TextField(e.getTelefone() == null ? "" : e.getTelefone());
        TextField email = new TextField(e.getEmail() == null ? "" : e.getEmail());
        TextField website = new TextField(e.getWebsite() == null ? "" : e.getWebsite());
        TextField logotipo = new TextField(e.getCaminhoLogotipo() == null ? "" : e.getCaminhoLogotipo());
        logotipo.setPromptText("(logótipo por defeito)");
        Button escolher = Ui.secundario("Escolher…");
        escolher.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Escolher logótipo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(janela());
            if (f != null) {
                logotipo.setText(f.getAbsolutePath());
            }
        });
        HBox linhaLogo = new HBox(8, logotipo, escolher);
        HBox.setHgrow(logotipo, javafx.scene.layout.Priority.ALWAYS);

        GridPane g = Ui.grelha();
        Ui.linha(g, 0, "Designação", designacao);
        Ui.linha(g, 1, "NIF", nif);
        Ui.linha(g, 2, "Morada", morada);
        Ui.linha(g, 3, "Telefone", telefone);
        Ui.linha(g, 4, "E-mail", email);
        Ui.linha(g, 5, "Website", website);
        Ui.linha(g, 6, "Logótipo (PDF)", linhaLogo);

        Button guardar = Ui.primario("Guardar dados da empresa");
        guardar.setOnAction(ev -> {
            try {
                ctx.admin.configurarEmpresa(designacao.getText(), nif.getText(), morada.getText(),
                        telefone.getText(), email.getText(), website.getText());
                ctx.admin.definirLogotipo(logotipo.getText());
                Ui.info(janela(), "Dados da empresa guardados.");
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        });
        return cartao("EMPRESA EMISSORA", g, new HBox(guardar));
    }

    // --------------------------------------------------------------- parâmetros

    private VBox cartaoParametros() {
        Empresa e = ctx.admin.getEmpresa();
        TextField iva = new TextField(Ui.semZeros(e.getTaxaIva()));
        iva.setMaxWidth(100);
        TextField dias = new TextField(String.valueOf(e.getDiasValidadePorDefeito()));
        dias.setMaxWidth(100);
        TextArea pagamento = new TextArea(e.getCondicoesPagamentoPorDefeito() == null
                ? "" : e.getCondicoesPagamentoPorDefeito());
        pagamento.setPrefRowCount(2);
        pagamento.setWrapText(true);

        GridPane g = Ui.grelha();
        Ui.linha(g, 0, "Taxa de IVA (%)", iva);
        Ui.linha(g, 1, "Validade (dias)", dias);
        Ui.linha(g, 2, "Condições de pagamento", pagamento);

        Label nota = new Label("Estes valores aplicam-se aos novos orçamentos; os já criados não mudam.");
        nota.getStyleClass().add("subtitulo");

        Button guardar = Ui.primario("Guardar parâmetros");
        guardar.setOnAction(ev -> {
            try {
                ctx.admin.configurarParametros(Ui.paraDecimal(iva.getText()), pagamento.getText(),
                        Integer.parseInt(dias.getText().trim()));
                Ui.info(janela(), "Parâmetros guardados.");
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        });
        return cartao("PARÂMETROS POR DEFEITO", g, nota, new HBox(guardar));
    }

    // -------------------------------------------------------------- utilizadores

    private VBox cartaoUtilizadores() {
        tabelaUtilizadores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaUtilizadores.setPrefHeight(220);
        tabelaUtilizadores.getColumns().add(Ui.<Utilizador>coluna("Utilizador", Utilizador::getNomeUtilizador, 140));
        tabelaUtilizadores.getColumns().add(Ui.<Utilizador>coluna("Nome", Utilizador::getNome, 200));
        tabelaUtilizadores.getColumns().add(Ui.<Utilizador>coluna("Perfil", Utilizador::getPerfil, 110));
        tabelaUtilizadores.getColumns().add(Ui.<Utilizador>coluna("Estado",
                u -> u.isAtivo() ? "Ativo" : "Desativado", 100));
        atualizarUtilizadores();

        Button novo = Ui.primario("Novo utilizador");
        novo.setOnAction(ev -> novoUtilizador());

        Button alternar = Ui.secundario("Desativar / reativar");
        alternar.disableProperty().bind(tabelaUtilizadores.getSelectionModel().selectedItemProperty().isNull());
        alternar.setOnAction(ev -> {
            Utilizador u = tabelaUtilizadores.getSelectionModel().getSelectedItem();
            if (u == null) {
                return;
            }
            try {
                if (u == ctx.auth.getSessao()) {
                    throw new IllegalStateException("Não pode desativar a sua própria conta.");
                }
                if (u.isAtivo()) {
                    ctx.admin.desativarUtilizador(u.getNomeUtilizador());
                } else {
                    ctx.admin.ativarUtilizador(u.getNomeUtilizador());
                }
                atualizarUtilizadores();
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        });
        return cartao("UTILIZADORES", tabelaUtilizadores, new HBox(10, novo, alternar));
    }

    private void atualizarUtilizadores() {
        tabelaUtilizadores.getItems().setAll(ctx.repo.getUtilizadores());
        tabelaUtilizadores.refresh();
    }

    private void novoUtilizador() {
        ChoiceBox<String> perfil = new ChoiceBox<>();
        perfil.getItems().addAll("Operador", "Administrador");
        perfil.setValue("Operador");
        TextField utilizador = new TextField();
        TextField nome = new TextField();
        PasswordField pw = new PasswordField();
        pw.setPromptText("Máx. 8 caracteres, com letras, números e símbolos");

        GridPane g = Ui.grelha();
        Ui.linha(g, 0, "Perfil", perfil);
        Ui.linha(g, 1, "Utilizador", utilizador);
        Ui.linha(g, 2, "Nome", nome);
        Ui.linha(g, 3, "Palavra-passe", pw);

        Dialog<ButtonType> d = Ui.dialogo(janela(), "Novo utilizador", g, "Criar");
        Ui.aoConfirmar(d, () -> {
            if ("Administrador".equals(perfil.getValue())) {
                ctx.admin.criarAdministrador(utilizador.getText().trim(), nome.getText(), pw.getText());
            } else {
                ctx.admin.criarOperador(utilizador.getText().trim(), nome.getText(), pw.getText());
            }
        });
        if (Ui.confirmado(d.showAndWait())) {
            atualizarUtilizadores();
        }
    }
}
