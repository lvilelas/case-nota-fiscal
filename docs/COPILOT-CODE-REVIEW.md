# Revisao automatica com GitHub Copilot

O repositorio inclui instrucoes gerais em `.github/copilot-instructions.md` e criterios especificos de Java em `.github/instructions/java-review.instructions.md`. O GitHub Copilot Code Review le esses arquivos para contextualizar a revisao.

A solicitacao automatica de revisao nao pode ser habilitada apenas por um arquivo ou workflow versionado: ela depende do plano Copilot e de uma regra configurada no GitHub. Para ativa-la no repositorio:

1. Acesse `Settings > Rules > Rulesets`.
2. Crie um branch ruleset ativo com a branch alvo `main`.
3. Marque `Automatically request Copilot code review`.
4. Opcionalmente marque `Review new pushes`; deixe revisao de drafts desabilitada se quiser revisar somente PRs prontos.
5. Em `Settings > Copilot > Code review`, mantenha habilitado o uso de custom instructions.

Essa configuracao faz o Copilot revisar automaticamente PRs abertos para `main`. A revisao continua sendo complementar: os gates da CI e a aprovacao humana permanecem necessarios.

Referencias oficiais:

- [Configurar revisao automatica pelo Copilot](https://docs.github.com/en/copilot/how-tos/copilot-on-github/set-up-copilot/configure-automatic-review)
- [Adicionar instrucoes customizadas ao repositorio](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions)
