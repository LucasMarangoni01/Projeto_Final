# Sistema de Monitoramento de Flanges — Soda Cáustica

Projeto desenvolvido para monitoramento industrial de flanges em tubulações que transportam soda cáustica, com foco em segurança, controle de manutenção, prevenção de vazamentos e acompanhamento de incidentes.

O sistema utiliza uma aplicação web desenvolvida em **Spring Boot**, com interface em **Thymeleaf**, banco de dados **MySQL** e estrutura MVC. A proposta do projeto também considera integração com sensores físicos, como Arduino e sensores térmicos, para alertas visuais em caso de possíveis vazamentos ou aumento anormal de temperatura.

---

## Objetivo do Projeto

O objetivo principal é auxiliar empresas e ambientes industriais no controle de flanges utilizadas em tubulações de soda cáustica, reduzindo riscos de acidentes, falhas de manutenção e vazamentos.

A soda cáustica é uma substância corrosiva e pode causar danos sérios em caso de contato. Por isso, o sistema busca melhorar o acompanhamento das flanges, registrar manutenções, monitorar temperaturas e facilitar a visualização de pontos críticos.

---

## Funcionalidades

- Cadastro de flanges
- Listagem de flanges cadastradas
- Alteração de dados das flanges
- Exclusão de flanges
- Upload de imagem da flange
- Cadastro de funcionários
- Listagem de funcionários
- Alteração e exclusão de funcionários
- Cálculo automático de idade pela data de nascimento
- Cadastro de manutenções
- Associação de manutenção com flange e funcionário
- Registro de ocorrências
- Mapa visual das flanges
- Painel com status das flanges
- Simulação de alerta de vazamento
- Dashboard com informações do sistema
- Gráficos de incidentes
- Controle de usuários com login e senha
- Separação de permissões por usuário

---

## Tecnologias Utilizadas

### Back-End

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Spring Security
- Maven
- Validation

### Front-End

- HTML5
- CSS3
- Thymeleaf
- JavaScript
- Chart.js
- SVG para o mapa das flanges

### Banco de Dados

- MySQL

### Hardware previsto no projeto

- Arduino
- Sensor térmico
- LED verde para situação normal
- LED vermelho para situação de alerta
- Sistema de alerta para possível vazamento

---

## Estrutura do Projeto
