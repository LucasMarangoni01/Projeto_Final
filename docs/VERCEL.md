# Hospedar o Projeto_Final na Vercel

> A Vercel passou a suportar aplicações HTTP Java/Spring Boot pelo runtime **Container (beta)**. Esta implantação é um contêiner HTTP que pode iniciar e parar conforme o tráfego. Não é um servidor permanente.

## Arquitetura

- **Vercel Container:** executa o Spring Boot 4 / Java 21, páginas Thymeleaf, CSS, JavaScript, login, cadastros e relatórios.
- **MySQL hospedado externamente:** armazena unidades, usuários, flanges, manutenções, incidentes, sessões de login e imagens privadas.
- **MQTT:** desabilitado neste contêiner. Para leituras contínuas, execute o assinante em um processo dedicado sempre ativo (fora da função), apontando para o mesmo MySQL.

A imagem Docker é montada pelo arquivo `Dockerfile.vercel` na raiz; `vercel.json` seleciona o preset `container`. A build usa `mvn -Pvercel` para incluir o Spring Session JDBC sem mudar o modo de desenvolvimento local.

## 1. Preparar um banco MySQL externo

Crie um banco MySQL acessível de fora da sua rede local (um serviço gerenciado costuma ser mais simples). **O MySQL instalado apenas no seu PC e `localhost` não funcionam na Vercel.**

Crie o schema `soda_caustica_flanges` e um usuário de aplicação com permissões apenas nesse schema. Configure TLS para a conexão com o provedor. A URL JDBC precisa apontar para o host do serviço, por exemplo:

```text
jdbc:mysql://SEU_HOST_MYSQL:3306/soda_caustica_flanges?sslMode=REQUIRED
```

O `DB_URL` deve corresponder ao formato JDBC esperado pelo MySQL Connector/J, não a uma URL `mysql://` genérica. Use `sslMode=VERIFY_IDENTITY` quando seu provedor disponibilizar certificados confiáveis e hostname correspondente; acompanhe a documentação dele.

O Hibernate mantém `ddl-auto=update` para facilitar o ambiente de TCC. Faça backup do banco antes de mudar o schema e prefira migrações versionadas para um uso permanente. A tabela de imagens privadas será criada pelo Hibernate; as tabelas `SPRING_SESSION` e `SPRING_SESSION_ATTRIBUTES` usam o script idempotente do perfil Vercel.

## 2. Criar projeto na Vercel

1. Acesse [vercel.com/new](https://vercel.com/new) e importe o repositório GitHub `LucasMarangoni01/Projeto_Final`.
2. Mantenha **Root Directory** na raiz do repositório (`./`).
3. Selecione o preset **Container** se não for identificado automaticamente. O repositório inclui `vercel.json` com `"framework": "container"`.
4. Nas **Environment Variables**, configure os valores abaixo para `Production` (e para `Preview` apenas com um banco independente, se for habilitar previews).

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:mysql://SEU_HOST:3306/soda_caustica_flanges?sslMode=REQUIRED` (ajuste conforme provedor) |
| `DB_USERNAME` | Usuário do MySQL remoto |
| `DB_PASSWORD` | Senha forte e exclusiva do MySQL |
| `APP_SETUP_KEY` | Código aleatório longo e secreto para o **primeiro ADMIN** |

**Nunca salve as senhas no GitHub ou em `vercel.json`.** O `APP_SETUP_KEY` não é senha de login, e sim a proteção do primeiro cadastro administrativo. Não compartilhe esse valor em imagens públicas.

5. Clique em **Deploy**. A Vercel construirá o contêiner com Maven e fornecerá um endereço `.vercel.app`.

Se sua conta ainda não oferece o runtime Container beta, não será possível usar este preset até que ele esteja disponível. Você também pode executar o mesmo contêiner em um host de Docker tradicional.

## 3. Primeiro acesso

1. Abra `/cadastro` após o deploy.
2. Crie a **primeira conta** e preencha o código da variável `APP_SETUP_KEY`.
3. O primeiro cadastro válido será ADMIN, conforme as regras atuais do projeto.
4. Acesse `/login` com esse e-mail e senha. Crie unidades e vincule as contas USER antes de liberar acesso a outras pessoas.

Sem o `APP_SETUP_KEY`, a criação do primeiro administrador na Vercel fica bloqueada por segurança. Se já houver usuários no MySQL utilizado, o sistema não pede código adicional no cadastro; administre o acesso normalmente com as contas existentes.

> **Antes de publicar publicamente:** use dados fictícios no ambiente de demonstração e confirme que o MySQL não contém dados pessoais/industriais de terceiros.

## 4. Armazenamento e sessões

No perfil `vercel`, a opção `app.upload.storage=database` faz as novas fotos de flanges e de perfis serem armazenadas no MySQL, em vez do disco temporário do contêiner. O controle de acesso às imagens continua nos controllers existentes. O banco deve ser dimensionado e ter backup para as fotos (máximo de 2 MB por envio).

Fotos antigas existentes apenas na pasta `uploads/` do computador **não são transferidas automaticamente** para o banco remoto. Reenvie as fotos após publicar, ou faça uma migração autorizada. Capturas do guia incluídas no Git não são afetadas.

Com o perfil Maven `vercel`, o Spring Session JDBC mantém as sessões de login no MySQL, evitando perdas causadas pela recriação de instâncias. A limpeza de sessões expiradas é responsabilidade do Spring Session.

## 5. MQTT e operação contínua

Vercel Container Functions são **stateless** e podem escalar para zero quando não há requisições. Portanto, `app.mqtt.enabled=false` é fixo neste perfil. Não ligue o consumidor MQTT na função HTTP: uma conexão que precisa ficar ativa constantemente pode ser interrompida sem aviso.

Os dados históricos já registrados no MySQL continuam disponíveis. Se precisar de leituras novas ininterruptas, execute o componente de coleta MQTT em um processo sempre ativo, com autenticação e conexão segura ao broker, e compartilhe apenas o banco externo com o site. A comunicação real com equipamentos deve ser validada em um ambiente controlado.

## 6. Testar antes de apresentar

- `/login` abre e mostra a opção de tema claro/escuro.
- Primeiro cadastro exige o código de instalação quando a base está vazia.
- Login de ADMIN e USER funciona; USER sem unidade vinculada continua bloqueado.
- Flanges, manutenções e alertas persistem após novo deploy.
- Fotos enviadas depois da publicação reaparecem após reiniciar a aplicação.
- Não surgem mensagens de erro de banco ou de criação das tabelas de sessão.
- Os arquivos e imagens do guia continuam abrindo.
- Regras de permissões continuam funcionando.
- O dashboard não deve alegar que há leitura ao vivo se a integração MQTT estiver desligada.

O build do GitHub Actions executa os testes usando o perfil Maven `vercel`. Isso verifica a compilação e os testes unitários, mas **não garante acesso ao seu MySQL externo nem valida a implantação real na Vercel**.

## Limitações e custos

O runtime Container está em beta; recursos, disponibilidade e cobrança podem mudar. A Vercel pode aplicar limites de memória, duração por requisição e tamanho de payload. Um banco MySQL remoto normalmente tem cobrança própria. O uso de sessões e fotos em banco facilita a demonstração, mas, para produção empresarial, considere banco administrado, backups, migrações de schema, restrição de cadastro e armazenamento de arquivos em um serviço dedicado.

Documentação oficial:
- [Vercel: Run any Dockerfile](https://vercel.com/blog/dockerfile-on-vercel)
- [Vercel: Containers](https://vercel.com/kb/guide/does-vercel-support-docker-deployments)
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html)
