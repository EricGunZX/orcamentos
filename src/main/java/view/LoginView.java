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

/** Ecrã de autenticação (US01). */
public class LoginView extends StackPane {

    public LoginView(Contexto ctx, Runnable aoEntrar) {
        Label titulo = Ui.titulo("Orçamentos");
        Label sub = new Label("Inicie sessão para continuar");
        sub.getStyleClass().add("subtitulo");

        TextField utilizador = new TextField();
        utilizador.setPromptText("Utilizador");
        PasswordField palavraPasse = new PasswordField();
        palavraPasse.setPromptText("Palavra-passe");

        Label erro = new Label();
        erro.getStyleClass().add("erro");
        erro.setWrapText(true);

        Button entrar = Ui.primario("Entrar");
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setDefaultButton(true);
        entrar.setOnAction(e -> {
            try {
                ctx.auth.entrar(utilizador.getText().trim(), palavraPasse.getText());
                aoEntrar.run();
            } catch (SecurityException ex) {
                erro.setText(ex.getMessage());
                palavraPasse.clear();
            }
        });

        VBox cartao = new VBox(8, Ui.logotipo(200), titulo, sub, Ui.rotulo("Utilizador"), utilizador,
                Ui.rotulo("Palavra-passe"), palavraPasse, erro, entrar);
        cartao.getStyleClass().add("cartao");
        cartao.setPadding(new Insets(32));
        cartao.setMaxWidth(380);
        cartao.setMaxHeight(Region.USE_PREF_SIZE);
        cartao.setAlignment(Pos.CENTER_LEFT);

        setPadding(new Insets(24));
        getChildren().add(cartao);
    }
}
