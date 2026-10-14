package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Primeira utilização: cria o Administrador inicial. */
public class ConfiguracaoInicialView extends StackPane {

    public ConfiguracaoInicialView(Contexto ctx, Runnable aoConcluir) {
        Label titulo = Ui.titulo("Primeira utilização");
        Label sub = new Label("Crie a conta de Administrador. Poderá depois criar os Operadores.");
        sub.getStyleClass().add("subtitulo");
        sub.setWrapText(true);

        TextField nome = new TextField();
        nome.setPromptText("Nome");
        TextField utilizador = new TextField();
        utilizador.setPromptText("Nome de utilizador");
        PasswordField pw1 = new PasswordField();
        pw1.setPromptText("Palavra-passe");
        PasswordField pw2 = new PasswordField();
        pw2.setPromptText("Repetir palavra-passe");
        Label regra = new Label("A palavra-passe tem no máximo 8 caracteres e inclui letras, números e símbolos.");
        regra.getStyleClass().add("rotulo");
        regra.setWrapText(true);

        Label erro = new Label();
        erro.getStyleClass().add("erro");
        erro.setWrapText(true);

        Button criar = Ui.primario("Criar Administrador");
        criar.setMaxWidth(Double.MAX_VALUE);
        criar.setDefaultButton(true);
        criar.setOnAction(e -> {
            try {
                if (!pw1.getText().equals(pw2.getText())) {
                    throw new IllegalArgumentException("As palavras-passe não coincidem.");
                }
                ctx.admin.criarAdministradorInicial(utilizador.getText().trim(), nome.getText(), pw1.getText());
                ctx.auth.entrar(utilizador.getText().trim(), pw1.getText());
                aoConcluir.run();
            } catch (RuntimeException ex) {
                erro.setText(Ui.mensagem(ex));
            }
        });

        VBox cartao = new VBox(8, Ui.logotipo(200), titulo, sub, Ui.rotulo("Nome"), nome,
                Ui.rotulo("Nome de utilizador"), utilizador, Ui.rotulo("Palavra-passe"), pw1,
                Ui.rotulo("Repetir palavra-passe"), pw2, regra, erro, criar);
        cartao.getStyleClass().add("cartao");
        cartao.setPadding(new Insets(32));
        cartao.setMaxWidth(400);
        cartao.setMaxHeight(Region.USE_PREF_SIZE);
        cartao.setAlignment(Pos.CENTER_LEFT);

        setPadding(new Insets(24));
        getChildren().add(cartao);
    }
}
