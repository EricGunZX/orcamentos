package view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import repository.Repositorio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Aplicação JavaFX: decide que ecrã mostrar (configuração inicial, login ou janela principal). */
public class AppFx extends Application {

    private Stage stage;
    private Contexto ctx;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Números Profícuos · Orçamentos");
        try {
            Path pasta = Path.of(System.getProperty("user.home"), "NumerosProficuos");
            Files.createDirectories(pasta);
            ctx = new Contexto(new Repositorio(pasta.resolve("dados.ser")), getHostServices());
        } catch (IOException | RuntimeException e) {
            Ui.erro(null, "Não foi possível abrir os dados da aplicação.\n" + e.getMessage());
            Platform.exit();
            return;
        }
        if (ctx.repo.getUtilizadores().isEmpty()) {
            mostrarConfiguracaoInicial();
        } else {
            mostrarLogin();
        }
        stage.show();
    }

    private void mostrarConfiguracaoInicial() {
        stage.setScene(Ui.cena(new ConfiguracaoInicialView(ctx, this::mostrarPrincipal), 520, 700));
        stage.centerOnScreen();
    }

    private void mostrarLogin() {
        stage.setScene(Ui.cena(new LoginView(ctx, this::mostrarPrincipal), 520, 640));
        stage.centerOnScreen();
    }

    private void mostrarPrincipal() {
        stage.setScene(Ui.cena(new JanelaPrincipal(ctx, this::terminarSessao), 1240, 780));
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.centerOnScreen();
    }

    private void terminarSessao() {
        ctx.auth.sair();
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        mostrarLogin();
    }
}
