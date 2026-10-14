 

import controller.AdministracaoController;
import controller.AutenticacaoController;
import controller.ClienteController;
import controller.OrcamentoController;
import domain.Capitulo;
import domain.Cliente;
import domain.Dinheiro;
import domain.EstadoOrcamento;
import domain.Orcamento;
import domain.UnidadeMedida;
import repository.Repositorio;

import java.math.BigDecimal;
import java.nio.file.Path;

/** Demonstração em consola do fluxo principal (sem interface gráfica). */
public class DemoConsola {

    public static void main(String[] args) {
        Path ficheiro = Path.of(args.length > 0 ? args[0] : "dados.ser");
        Repositorio repo = new Repositorio(ficheiro);
        AutenticacaoController auth = new AutenticacaoController(repo);
        AdministracaoController admin = new AdministracaoController(repo, auth);
        ClienteController clientes = new ClienteController(repo, auth);
        OrcamentoController orcamentos = new OrcamentoController(repo, auth);

        if (repo.getUtilizadores().isEmpty()) {
            admin.criarAdministradorInicial("admin", "Administrador", "Adm#1234");
        }
        auth.entrar("admin", "Adm#1234");
        System.out.println("Sessão: " + auth.getSessao());

        Cliente maria = repo.procurarClientePorNif("231456789").orElseGet(() ->
                clientes.registarParticular("Maria Ferreira", "231456789",
                        "Rua de São Miguel, 48, 4815-512 Vizela", "912345678", "maria.ferreira@email.pt"));

        Orcamento o = orcamentos.criar(maria, "Remodelação da casa de banho");
        Capitulo civil = orcamentos.adicionarCapitulo(o, "Construção civil");
        Capitulo agua = orcamentos.adicionarCapitulo(o, "Canalizações");
        Capitulo elet = orcamentos.adicionarCapitulo(o, "Eletricidade");
        Capitulo ar = orcamentos.adicionarCapitulo(o, "Ar condicionado");

        orcamentos.adicionarLinha(o, civil, "Demolição de azulejo e reboco na casa de banho",
                new BigDecimal("18"), UnidadeMedida.METRO_QUADRADO, new BigDecimal("14.50"));
        orcamentos.adicionarLinha(o, civil, "Assentamento de mosaico cerâmico, incluindo cola e betume",
                new BigDecimal("18"), UnidadeMedida.METRO_QUADRADO, new BigDecimal("32.00"));
        orcamentos.adicionarLinha(o, agua, "Substituição da rede de águas em multicamada",
                BigDecimal.ONE, UnidadeMedida.VERBA_GLOBAL, new BigDecimal("640.00"));
        orcamentos.adicionarLinha(o, elet, "Novo circuito para iluminação e tomadas",
                new BigDecimal("6"), UnidadeMedida.UNIDADE, new BigDecimal("45.00"));
        orcamentos.adicionarLinha(o, ar, "Instalação de split 12000 BTU, com material",
                BigDecimal.ONE, UnidadeMedida.UNIDADE, new BigDecimal("890.00"));

        orcamentos.mudarEstado(o, EstadoOrcamento.EMITIDO);

        System.out.println("Orçamento " + o.getNumero() + " (" + o.getEstado() + ")");
        for (Capitulo c : o.getCapitulos()) {
            System.out.println("  " + c.getTitulo() + " -> " + Dinheiro.formatar(c.getSubtotal()));
        }
        System.out.println("Subtotal: " + Dinheiro.formatar(o.getSubtotal()));
        System.out.println("IVA " + o.getTaxaIva() + "%: " + Dinheiro.formatar(o.getValorIva()));
        System.out.println("Total:    " + Dinheiro.formatar(o.getTotal()));
        System.out.println("Orçamentos guardados: " + repo.getOrcamentos().size());
    }
}
