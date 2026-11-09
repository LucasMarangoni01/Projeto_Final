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

### Gestão de pessoas por unidade

A ficha da unidade mantém duas listas separadas:

- **Contas de acesso:** somente contas USER ativas podem ser vinculadas por ID ou nome exato. Administradores continuam com acesso global. Desvincular bloqueia o login até nova associação, sem excluir a conta.
- **Funcionários operacionais:** cadastros usados como executores de manutenção também podem ser vinculados/desvinculados por ID ou nome exato. A desvinculação preserva o cadastro e os registros históricos.

Quando houver nomes repetidos, o sistema exige o ID para evitar associar a pessoa errada. Unidades arquivadas preservam os vínculos e o histórico; a exclusão definitiva só é permitida quando não há contas, funcionários ou flanges associados.

### Matriz de permissões por perfil

| Recurso | USER | ADMIN |
|---|---|---|
| Dashboard | Consulta dados da própria unidade | Consulta todas as unidades |
| Flanges | Consulta flanges da própria unidade | Cadastra, consulta, edita e exclui |
| Funcionários operacionais | Consulta funcionários da própria unidade | Cadastra, consulta, edita e exclui; define a unidade |
| Manutenções | Consulta o histórico da própria unidade e registra novos lançamentos | Consulta todas as unidades, registra, edita e exclui |
| Meu perfil | Edita nome, telefone e foto; altera a própria senha | Mesmos recursos para a própria conta |
| Dados profissionais (matrícula, cargo e setor) | Somente leitura | Edita pela administração de usuários |
| Usuários e permissões | Sem acesso administrativo | Gerencia contas, perfis, status e vínculos |
| Unidades | Sem acesso administrativo | Cria, edita, arquiva, reativa, remove unidades vazias e gerencia membros |
| Incidentes | Sem acesso administrativo | Administração dos registros |

As restrições são aplicadas no Spring Security e nos controladores/serviços. Esconder links no menu é apenas uma melhoria de interface, não o mecanismo de segurança. USER só pode registrar manutenção nova; não pode editar ou apagar um lançamento já enviado. O formulário valida que a flange e o funcionário pertençam à mesma unidade do usuário. Contas USER sem unidade ativa não podem autenticar.

### Governança dos dados do usuário

O titular da conta pode atualizar o nome, a foto e o telefone de contato. O telefone é formatado automaticamente com DDD (por exemplo, `(11) 99999-9999`; telefones fixos usam `(11) 9999-9999`). Matrícula, cargo e setor são dados corporativos: somente um ADMIN pode alterá-los na tela de edição administrativa do usuário. As mudanças são registradas na trilha de auditoria.

### Funcionalidades do módulo de usuários

O módulo de contas oferece recursos de identificação, controle de acesso e rastreabilidade:

- Foto de perfil individual (JPEG/PNG, até 2 MB), armazenada em diretório privado e servida por rota autenticada; quando não há foto, o sistema usa um avatar padrão.
- Edição do nome, telefone e informações profissionais (matrícula, cargo e setor) pelo titular da conta.
- Alteração de senha mediante confirmação da senha atual, com mínimo de 12 caracteres.
- Perfis de acesso ADMIN e USER, ativação/desativação e associação a uma unidade industrial.
- Bloqueio de contas USER sem unidade ativa vinculada e proteção contra desativar ou rebaixar o último administrador ativo.
- Data de criação, último acesso e registro dos principais eventos: cadastro, login, alteração de perfil, foto, senha, unidade, permissão e status.
- Consulta dos últimos 30 eventos por conta na área de perfil e na administração de usuários.

A alteração de e-mail de acesso não é oferecida no autoatendimento: o e-mail funciona como identificador único e deve ser tratado como dado administrativo. O histórico é uma trilha operacional da aplicação, não substitui um sistema corporativo de auditoria com retenção, imutabilidade e controles de acesso dedicados.

## Configuração do ambiente local

A conexão com o MySQL desta branch está configurada diretamente em `src/main/resources/application.properties`, conforme a configuração local definida para o projeto. **Não é necessário criar ou preencher um arquivo `.env` para iniciar a aplicação.** O Spring Boot não carrega arquivos `.env` automaticamente nesta configuração.

Confira no `application.properties` o endereço do banco, usuário e senha que correspondem ao seu MySQL local. Não cadastre variáveis `DB_URL`, `DB_USERNAME` ou `DB_PASSWORD` na configuração de execução do IntelliJ, pois variáveis de ambiente podem sobrescrever os valores do arquivo.

### Primeiro administrador

O bootstrap cria a primeira conta ADMIN somente quando as variáveis de ambiente `ADMIN_EMAIL` e `ADMIN_PASSWORD` são informadas juntas. Se o banco já possui um administrador, não é necessário configurá-las.

No IntelliJ IDEA:

1. Abra **Run > Edit Configurations...** e selecione a aplicação.
2. Em **Environment variables**, informe `ADMIN_EMAIL` com um e-mail que ainda não esteja cadastrado.
3. Informe `ADMIN_PASSWORD` com uma senha de pelo menos 12 caracteres.
4. Execute a aplicação. O bootstrap cria a conta apenas se o e-mail ainda não existir; ele não redefine a senha de contas existentes.
5. Depois do primeiro acesso, remova essas duas variáveis da configuração de execução.

Para executar pelo PowerShell, defina as duas variáveis apenas na janela atual:

```powershell
$env:ADMIN_EMAIL = "admin@empresa.com.br"
$env:ADMIN_PASSWORD = "Use-Uma-Senha-Forte-Com-12+Caracteres"

.\mvnw.cmd spring-boot:run
```

### Atenção ao administrador já existente

Contas criadas pelo formulário público recebem o perfil USER e ficam sem unidade inicialmente; por segurança, não conseguem entrar até que um ADMIN as associe a uma unidade ativa. O primeiro ADMIN deve ser criado pelas variáveis de ambiente `ADMIN_EMAIL` e `ADMIN_PASSWORD` na configuração de execução do IntelliJ ou no terminal. O bootstrap cria essa conta somente quando o e-mail configurado ainda não existe. Ele não promove nem redefine senhas de contas existentes. Em bancos já utilizados, configure um novo e-mail administrativo, entre com a nova conta e desative a conta inicial antiga pela administração de usuários, depois de confirmar o acesso.

### Rotação de credenciais

Se uma senha já foi publicada no histórico de um repositório público, alterá-la apenas no arquivo atual não é suficiente. Troque a senha correspondente no próprio MySQL e atualize `spring.datasource.password` em `application.properties` (ou injete a nova credencial por um mecanismo de segredos da implantação). Senhas administrativas antigas também devem ser substituídas ou as respectivas contas desativadas.
