package view;

import domain.Empresa;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/** Dados da empresa e parâmetros por defeito (US02). */
public class AdministracaoView extends ScrollPane {

    private final Contexto ctx;

    public AdministracaoView(Contexto ctx) {
        this.ctx = ctx;
        VBox pagina = new VBox(16, Ui.titulo("Definições"), cartaoEmpresa(), cartaoParametros());
        pagina.getStyleClass().add("pagina");
        setContent(pagina);
        setFitToWidth(true);
        getStyleClass().add("ecra");
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private VBox cartao(String titulo, javafx.scene.Node... conteudo) {
        Label cab = new Label(titulo);
        cab.getStyleClass().add("form-titulo");
        VBox v = new VBox(10, cab);
        v.getChildren().addAll(conteudo);
        v.getStyleClass().addAll("cartao", "papel");
        v.setMaxWidth(1000);
        return v;
    }

    // ------------------------------------------------------------------ empresa

    private VBox cartaoEmpresa() {
        Empresa e = ctx.admin.getEmpresa();
        TextField designacao = new TextField(e.getDesignacao());
        TextField nif = new TextField(e.getNif());
        TextArea morada = new TextArea(e.getMorada());
        morada.setPrefRowCount(2);
        morada.setMinHeight(64);
        morada.setPrefHeight(64);
        morada.setWrapText(true);
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

        GridPane g1 = Ui.gridForm();
        g1.add(Ui.campoForm("Nome da empresa", true, designacao), 0, 0);
        g1.add(Ui.campoForm("NIF", true, nif), 1, 0);
        GridPane g2 = Ui.gridForm();
        g2.add(Ui.campoForm("Telefone", false, telefone), 0, 0);
        g2.add(Ui.campoForm("E-mail", false, email), 1, 0);
        g2.add(Ui.campoForm("Página do site", false, website), 0, 1, 2, 1);
        GridPane g3 = Ui.gridForm();
        g3.add(Ui.campoForm("Morada", true, morada), 0, 0, 2, 1);
        g3.add(Ui.campoForm("Logótipo (imagem PNG ou JPG)", false, linhaLogo), 0, 1, 2, 1);

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
        return cartao("Dados da empresa", Ui.secaoForm("Informações básicas"), g1, Ui.secaoForm("Contacto"), g2,
                Ui.secaoForm("Localização"), g3, new HBox(guardar));
    }

    // --------------------------------------------------------------- parâmetros

    private VBox cartaoParametros() {
        Empresa e = ctx.admin.getEmpresa();
        TextField iva = new TextField(Ui.semZeros(e.getTaxaIva()));
        TextField dias = new TextField(String.valueOf(e.getDiasValidadePorDefeito()));
        TextArea pagamento = new TextArea(e.getCondicoesPagamentoPorDefeito() == null
                ? "" : e.getCondicoesPagamentoPorDefeito());
        pagamento.setPrefRowCount(2);
        pagamento.setWrapText(true);

        GridPane g = Ui.gridForm();
        g.add(Ui.campoForm("Taxa de IVA (%)", true, iva), 0, 0);
        g.add(Ui.campoForm("Validade do orçamento (dias)", true, dias), 1, 0);
        g.add(Ui.campoForm("Condições de pagamento", false, pagamento), 0, 1, 2, 1);

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
        return cartao("Valores por defeito dos orçamentos", g, nota, new HBox(guardar));
    }
}
