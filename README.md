# Números Profícuos · Aplicação de Orçamentos

Aplicação em Java (JavaFX 11) para criar, gerir e emitir orçamentos em PDF.

## Como executar

Requisitos: **JDK 11 ou superior** e **Maven**.

```
mvn javafx:run
```

Ou, no IDE, execute a classe `Main` (pasta `src/main/java`).

A aplicação abre diretamente, sem login nem contas. Os dados ficam
guardados (serialização de objetos) em `~/NumerosProficuos/dados.ser`.

> Em Mac com processador Apple Silicon, altere no `pom.xml` a propriedade
> `javafx.version` de `11.0.2` para `17.0.12`.

## Gerar o executável para entregar ao cliente

O resultado é uma pasta com `Orcamentos.exe` e o Java **já incluído**: o cliente não precisa de
instalar nada. O executável tem de ser gerado no sistema onde vai correr (um `.exe` faz-se em Windows).

**Opção 1: no teu Windows** (precisa de JDK 17+ e Maven no PATH)

1. Faz duplo clique em `build-windows.bat`.
2. No fim aparece `dist\Orcamentos\`. Comprime essa pasta em ZIP e entrega.
3. O cliente descomprime e abre `Orcamentos.exe`.

**Opção 2: no GitHub (Windows, macOS e Linux de uma vez)**

1. Faz push do projeto para um repositório GitHub.
2. Abre o separador *Actions*, escolhe *Gerar aplicação executável* e espera pelo fim.
3. Descarrega o ZIP `Orcamentos-Windows` na secção *Artifacts*.

**Linux / macOS:** `./build-linux-mac.sh` (resultado em `dist/`).

Os dados do cliente ficam em `C:\Users\<utilizador>\NumerosProficuos\dados.ser`, fora da pasta da
aplicação, por isso uma nova versão da aplicação não apaga os orçamentos.

## Aspeto

Interface moderna: barra azul com menu «hambúrguer» e menu lateral (Orçamentos, Clientes, Empresa), formulários em secções com o rótulo por cima, cartões de resumo (orçamentos, rascunhos, adjudicados, valor adjudicado), filtros, tabela com estados em etiquetas coloridas e janelas de diálogo uniformes.

## Editor do orçamento

Folha A4 com barra azul, no estilo da página «opção A · cartão»: clica-se em qualquer texto para o editar
(grava sozinho), `×` remove uma linha (a última é substituída por uma em branco), `+ Adicionar linha`,
`+ Adicionar capítulo`, IVA, «Válido até», totais e observações. Barra de topo com **Novo orçamento** e **Guardar PDF**.
Uma linha só entra no orçamento quando tem descrição e preço (fica a amarelo até lá).

## Estrutura (MVC)

| Pacote | Conteúdo |
|---|---|
| `domain` | Modelo e regras de negócio: `Orcamento`, `Capitulo`, `LinhaOrcamento`, `Cliente` (`ClienteParticular`, `ClienteEmpresa`), `Utilizador` (`Administrador`, `Operador`), `Empresa`, `EstadoOrcamento`, `UnidadeMedida` |
| `view` | Interface JavaFX: login, janela principal, orçamentos, editor do orçamento, clientes, administração |
| `controller` | Ligam as vistas ao modelo: autenticação, administração, clientes, orçamentos |
| `repository` | Persistência por serialização (`Repositorio`) |
| `service` | `GeradorPdf` (Apache PDFBox) |

`Main` apenas arranca a interface. `DemoConsola` é uma demonstração em consola, sem interface.

## Funcionalidades

- US02 Configuração da empresa (separador «Empresa»), IVA, condições de pagamento e validade por defeito
- US04 Gestão de clientes (particular e empresa com A/C)
- US05 Criação de orçamento com número automático (ex.: 2026/014)
- US06 Linhas, capítulos, subtotais, IVA incluído ou não incluído, total
- US07 Estados: rascunho, emitido, adjudicado, recusado, anulado (só rascunhos são editáveis)
- US08 PDF igual ao modelo Exemplar_NP (cabeçalho: logótipo, nome e morada; rodapé: contacto, e-mail e site), com paginação
- US09 Pesquisa por número, cliente, data e estado
- US10 Duplicar orçamento

## Por fazer

- Testes unitários (JUnit 5) e relatório de cobertura (JaCoCo): já estão no `pom.xml`.
