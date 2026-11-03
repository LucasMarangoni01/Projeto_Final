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

As credenciais não devem ser armazenadas no repositório. Antes de executar a aplicação, configure as variáveis de ambiente abaixo na configuração de execução da IDE ou no terminal.

### Variáveis necessárias

| Variável | Finalidade |
|---|---|
| `DB_URL` | URL JDBC do MySQL. Caso omitida, é utilizado o endereço local padrão do projeto. |
| `DB_USERNAME` | Usuário do MySQL. Caso omitida, o padrão é `root`. |
| `DB_PASSWORD` | Senha do usuário do MySQL. Obrigatória. |
| `ADMIN_EMAIL` | E-mail da conta ADMIN inicial. Opcional quando já existe uma conta administrativa. |
| `ADMIN_PASSWORD` | Senha da conta ADMIN inicial, com pelo menos 12 caracteres. Deve ser definida junto com `ADMIN_EMAIL`. |

### Exemplo no PowerShell (Windows)

Substitua os valores de exemplo pelas credenciais locais configuradas no seu MySQL:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/soda_caustica_flanges?createDatabaseIfNotExist=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "SUA_NOVA_SENHA_DO_MYSQL"

# Para criar uma nova conta ADMIN inicial, use um e-mail ainda não cadastrado.
$env:ADMIN_EMAIL = "admin-projeto@example.com"
$env:ADMIN_PASSWORD = "Use-Uma-Senha-Forte-Com-12+Caracteres"

.`mvnw.cmd` spring-boot:run
```

As variáveis definidas dessa forma permanecem disponíveis apenas na janela atual do PowerShell. Na IDE, elas também podem ser cadastradas na seção **Environment variables** da configuração de execução da aplicação.

### Atenção ao administrador já existente

O bootstrap cria uma conta ADMIN somente quando o e-mail configurado ainda não existe. Ele não redefine senhas de contas existentes. Em bancos já utilizados, configure um novo e-mail administrativo, entre com a nova conta e desative a conta inicial antiga pela administração de usuários, depois de confirmar o acesso.

### Rotação de credenciais

Se uma senha já foi publicada no histórico de um repositório público, alterá-la apenas no arquivo atual não é suficiente. Troque a senha correspondente no próprio MySQL e atualize a variável `DB_PASSWORD`. Senhas administrativas antigas também devem ser substituídas ou as respectivas contas desativadas.
