# Sistema de Monitoramento e Gerenciamento de Flanges em Ambientes Industriais com Soda Cáustica

Projeto desenvolvido com foco no **monitoramento e gerenciamento de flanges em ambientes industriais com soda cáustica (NaOH)**, apoiando a prevenção de acidentes, o acompanhamento de ativos e a organização das atividades operacionais.

O sistema tem como objetivo auxiliar no **monitoramento de flanges**, identificação de possíveis falhas, controle de manutenções e organização das informações relacionadas à segurança industrial.

---

## Nome do Projeto

**Sistema de Monitoramento e Gerenciamento de Flanges em Ambientes Industriais com Soda Cáustica**

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

Cada integrante utiliza seu próprio MySQL local. A aplicação conecta usando o usuário de banco com privilégios restritos `soda_app`; não utiliza a conta administrativa `root`.

1. Copie `.env.example` para um arquivo chamado `.env` na raiz do projeto.
2. Defina uma senha forte em `DB_PASSWORD`.
3. Abra `database/setup.sql` no MySQL Workbench, substitua as duas ocorrências de `TROQUE_POR_UMA_SENHA_FORTE_2026` pela mesma senha definida em `.env` e execute o script usando uma conta administrativa do MySQL.
4. Inicie a aplicação pelo IntelliJ. O Spring Boot importa o arquivo `.env` local automaticamente.
5. Não envie o arquivo `.env` ao GitHub. Ele já está listado no `.gitignore`.

A URL padrão aponta para `localhost`, portanto cada computador mantém seu banco independente. Se o projeto for hospedado em um servidor compartilhado, configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no ambiente de implantação. Para implantação, ative o perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`) atrás de um proxy reverso com HTTPS; esse perfil usa a porta 8080, oculta detalhes internos dos erros e desativa a saída de SQL.

### Primeiro administrador

Em um banco sem usuários, o primeiro cadastro feito pelo formulário público recebe automaticamente o perfil `ADMIN`. Os cadastros seguintes recebem `USER` e precisam ser associados a uma unidade ativa por um administrador. Não é necessário usar um atalho de login nem informar variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD` para esse fluxo.

### Rotação de credenciais expostas

Uma credencial que já foi incluída em um repositório público deve ser considerada comprometida, mesmo depois de removida do arquivo atual. Faça a rotação da senha do usuário MySQL anteriormente utilizado e de qualquer outro segredo que tenha sido publicado. A aplicação passa a usar `soda_app`, com privilégios limitados ao banco do projeto.
