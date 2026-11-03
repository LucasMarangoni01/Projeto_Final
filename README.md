# Sistema de Prevenção e Monitoramento de Soda Cáustica

Projeto desenvolvido com foco na **prevenção de acidentes envolvendo soda cáustica**, especialmente em ambientes industriais que utilizam linhas, tubulações e flanges para transporte de NaOH.

O sistema tem como objetivo auxiliar no **monitoramento de flanges**, identificação de possíveis falhas, controle de manutenções e organização das informações relacionadas à segurança industrial.

---

## Sobre o Projeto

A soda cáustica é uma substância altamente corrosiva e perigosa. Em ambientes industriais, vazamentos em flanges, tubulações ou conexões podem causar acidentes graves, prejuízos materiais e riscos à saúde dos funcionários.

Pensando nisso, este projeto foi criado para funcionar como um sistema de apoio à **prevenção, controle e monitoramento de vazamentos**, permitindo o cadastro e acompanhamento de flanges, funcionários, manutenções e possíveis incidentes.

---

## Objetivo

O principal objetivo do sistema é oferecer uma plataforma simples e funcional para:

- Cadastrar flanges utilizadas na linha de soda cáustica;
- Monitorar informações importantes das flanges;
- Registrar funcionários responsáveis;
- Controlar manutenções realizadas;
- Auxiliar na prevenção de vazamentos;
- Melhorar a segurança no ambiente industrial;
- Facilitar a visualização de dados por meio de uma interface web.

---

## Tecnologias Utilizadas

O projeto foi desenvolvido utilizando as seguintes tecnologias:

- Java
- Spring Boot
- MySQL
- HTML
- CSS
- JavaScript
- Thymeleaf
- Maven

---

## Funcionalidades

### Flanges

- Cadastro de flanges;
- Listagem de flanges;
- Alteração de dados;
- Exclusão de registros;
- Registro de localização;
- Registro de temperatura ambiente;
- Data de entrada em operação;
- Upload de foto da flange.

### Funcionários

- Cadastro de funcionários;
- Listagem de funcionários;
- Alteração de dados;
- Exclusão de funcionários;
- Associação do funcionário com uma flange;
- Controle de idade e data de nascimento.

### Manutenções

- Cadastro de manutenções;
- Registro de ocorrências;
- Número de série;
- Data da manutenção;
- Associação da manutenção com flange e funcionário.

### Monitoramento

- Painel visual para acompanhamento das flanges;
- Indicação de status normal ou alerta;
- Organização dos dados para auxiliar na prevenção de falhas;
- Possibilidade de integração com sensores físicos, como Arduino e sensor térmico.


---

## Configuração do ambiente local

As credenciais não devem ser armazenadas no repositório. O projeto carrega automaticamente um arquivo local chamado `.env` na raiz. Esse arquivo é ignorado pelo Git e não será publicado. Para evitar usar a conta administrativa `root` na aplicação, crie uma conta exclusiva chamada `soda_app` usando o script `database/setup.sql`. Depois copie `.env.example` para `.env` e configure a mesma senha nos dois locais. Para criar o primeiro administrador, preencha também `ADMIN_EMAIL` com um e-mail ainda não cadastrado e `ADMIN_PASSWORD` com uma senha de pelo menos 12 caracteres. Sem essas duas variáveis, nenhum ADMIN inicial será criado.

### Variáveis necessárias

| Variável | Finalidade |
|---|---|
| `DB_URL` | URL JDBC do MySQL. Caso omitida, é utilizado o endereço local padrão do projeto. |
| `DB_USERNAME` | Usuário da aplicação no MySQL. O modelo utiliza `soda_app`; não use `root` na execução normal. |
| `DB_PASSWORD` | Senha do usuário do MySQL. Obrigatória. |
| `ADMIN_EMAIL` | E-mail da conta ADMIN inicial. Opcional quando já existe uma conta administrativa. |
| `ADMIN_PASSWORD` | Senha da conta ADMIN inicial, com pelo menos 12 caracteres. Deve ser definida junto com `ADMIN_EMAIL`. |

### Configuração recomendada no IntelliJ IDEA (Windows)

1. Abra o MySQL Workbench com uma conta que tenha permissão para criar usuários e bancos.
2. Abra e execute `database/setup.sql`. Antes, substitua o texto `TROQUE_POR_UMA_SENHA_FORTE_2026` pela senha forte que você escolheu.
3. Na raiz do projeto, copie `.env.example` e renomeie a cópia para `.env`.
4. No `.env`, substitua o valor de `DB_PASSWORD` pela mesma senha colocada no script SQL. Mantenha `DB_USERNAME=soda_app`.
5. No IntelliJ, abra **Run > Edit Configurations...** e selecione a configuração da aplicação.
6. Em **Environment variables**, remova valores antigos de `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. O IntelliJ pode sobrescrever os valores do arquivo `.env`.
7. Confirme que o **Working directory** aponta para a pasta raiz do projeto (a mesma pasta onde estão `pom.xml`, `.env.example` e a pasta `src`) e execute novamente. O arquivo `.env` não vem dentro do ZIP do GitHub: você precisa criá-lo copiando `.env.example`. Sem ele, a aplicação usa o usuário padrão `soda_app` e senha vazia; o MySQL recusará a conexão até você configurar a senha.

O arquivo `.env` deve usar o formato `NOME=valor`, sem `export` e sem aspas. Não compartilhe esse arquivo nem envie sua senha pelo chat. Se você não conseguir entrar no Workbench com uma conta administrativa, será necessário recuperar esse acesso antes de executar o script; não existe uma alteração segura no Spring Boot que possa corrigir uma senha recusada pelo próprio MySQL.

### Exemplo no PowerShell (Windows)

Substitua os valores de exemplo pelas credenciais locais configuradas no seu MySQL:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/soda_caustica_flanges?createDatabaseIfNotExist=true"
$env:DB_USERNAME = "soda_app"
$env:DB_PASSWORD = "A_MESMA_SENHA_CONFIGURADA_NO_SETUP_SQL"

# Para criar uma nova conta ADMIN inicial, use um e-mail ainda não cadastrado.
$env:ADMIN_EMAIL = "admin-projeto@example.com"
$env:ADMIN_PASSWORD = "Use-Uma-Senha-Forte-Com-12+Caracteres"

.\mvnw.cmd spring-boot:run
```

As variáveis definidas dessa forma permanecem disponíveis apenas na janela atual do PowerShell. Na IDE, elas também podem ser cadastradas na seção **Environment variables** da configuração de execução da aplicação.

### Atenção ao administrador já existente

Contas criadas pelo formulário público recebem o perfil USER e ficam sem unidade inicialmente; por segurança, não conseguem entrar até que um ADMIN as associe a uma unidade ativa. O primeiro ADMIN deve ser criado pelas variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD` no `.env`. O bootstrap cria essa conta somente quando o e-mail configurado ainda não existe. Ele não promove nem redefine senhas de contas existentes. Em bancos já utilizados, configure um novo e-mail administrativo, entre com a nova conta e desative a conta inicial antiga pela administração de usuários, depois de confirmar o acesso.

### Rotação de credenciais

Se uma senha já foi publicada no histórico de um repositório público, alterá-la apenas no arquivo atual não é suficiente. Troque a senha correspondente no próprio MySQL e atualize a variável `DB_PASSWORD`. Senhas administrativas antigas também devem ser substituídas ou as respectivas contas desativadas.
