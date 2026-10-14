package view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repository.Repositorio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Aplicação JavaFX: abre diretamente a janela principal (não há contas nem login). */
public class AppFx extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Números Profícuos · Orçamentos");
        try (InputStream in = AppFx.class.getResourceAsStream("/logo.png")) {
            if (in != null) {
                stage.getIcons().add(new Image(in));
            }
        } catch (IOException ignorado) {
            // sem ícone
        }
        Contexto ctx;
        try {
            Path pasta = Path.of(System.getProperty("user.home"), "NumerosProficuos");
            Files.createDirectories(pasta);
            ctx = new Contexto(new Repositorio(pasta.resolve("dados.ser")), getHostServices());
            ctx.auth.iniciarSessaoAutomatica();
        } catch (IOException | RuntimeException e) {
            Ui.erro(null, "Não foi possível abrir os dados da aplicação.\n" + e.getMessage());
            Platform.exit();
            return;
        }
        // Janela adaptada ao ecrã: ocupa a área útil (sem esconder a barra de tarefas) e pode redimensionar-se
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        double largura = Math.min(1280, area.getWidth());
        double altura = Math.min(900, area.getHeight());
        stage.setScene(Ui.cena(new JanelaPrincipal(ctx), largura, altura));
        stage.setMinWidth(Math.min(900, area.getWidth()));
        stage.setMinHeight(Math.min(600, area.getHeight()));
        stage.setX(area.getMinX() + (area.getWidth() - largura) / 2);
        stage.setY(area.getMinY() + (area.getHeight() - altura) / 2);
        stage.show();
    }
}
