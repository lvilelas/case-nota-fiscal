---
applyTo: "**/*.java"
---

Na revisao de Java, verifique nulabilidade nas fronteiras, precisao de valores monetarios, colecoes mutaveis compartilhadas, contratos de records/DTOs, limites de tabelas tributarias e preservacao do sinal de interrupcao.

Prefira injecao por construtor, classes pequenas e nomes orientados ao dominio. Sinalize dependencias de framework dentro de `domain` ou `application`, chamadas AWS/JDBC fora de adaptadores e regras de negocio dentro de controllers, schedulers ou listeners.

Confirme que excecoes de negocio chegam como 422, payloads invalidos como 400 e indisponibilidade de infraestrutura como 503, sem expor stack traces ou detalhes internos ao cliente.
