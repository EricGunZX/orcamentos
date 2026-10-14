import javafx.application.Application;
import view.AppFx;

/**
 * Ponto de entrada da aplicação. Não contém dados: apenas arranca a interface JavaFX.
 * (Não estende Application para poder ser executado a partir do IDE sem configurar módulos.)
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(AppFx.class, args);
    }
}
