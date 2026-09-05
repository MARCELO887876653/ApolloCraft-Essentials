# ApolloCraft Essentials — alterações desta versão

Versão personalizada do ApolloCraft baseada no uxmEssentials (UXPLIMA), mantendo a licença GPL-3.0 e o código original dos módulos.

## Feito

- Marca pública principal alterada para **ApolloCraft Essentials**.
- Comando administrativo principal: `/apollo`.
- Aliases de compatibilidade preservados: `/apollocraft`, `/apolloessentials`, `/uxmess`, `/uxmessentials`, `/uxe`.
- Novo `/apollo modules` com painel visual de módulos.
- O painel lista os **36 módulos**; nenhum módulo foi removido do código ou do JAR.
- Clique no painel grava o estado `enabled` do módulo; a mudança entra em vigor no próximo reinício, evitando hot-unload inseguro.
- O painel mostra o estado configurado no boot, o estado desejado no próximo reinício e se há alteração pendente.
- Proteção contra cliques concorrentes no mesmo módulo.
- O Gerenciador de Módulos também aparece na central `/apollo gui`.
- Idioma padrão alterado para português (`pt`).
- Prefixo visual dos catálogos alterado para ApolloCraft.
- Banco SQLite padrão novo: `apollocraft.db` em instalações novas.
- Nome da camada de mapa padrão: ApolloCraft.
- Banner/logs de inicialização e desligamento rebrandados.
- Scoreboard, tablist, Security 2FA e ServerTweaks receberam branding ApolloCraft nos defaults.
- Nome do artefato principal: `ApolloCraft-Essentials`.
- Descritores dos add-ons Discord/Redis/REST atualizados para depender de `ApolloCraftEssentials`.
- Velocity recebeu branding visual ApolloCraft; o ID interno legado foi mantido por compatibilidade.
- Geradores de mundo passam a usar `ApolloCraftEssentials:void|flat`, aceitando também o namespace legado `uxmEssentials:`.
- Namespace Java (`com.uxplima.uxmessentials`), permissões (`uxmessentials.*`) e canais internos foram preservados para reduzir quebras de compatibilidade.
- `LICENSE` original preservada e `NOTICE-APOLLOCRAFT.md` adicionado com atribuição.

## Build

O projeto continua exigindo Java 25 e Gradle 9.4.1. O artefato Paper/Purpur é gerado pelo task `:bukkit-adapter:shadowJar`.

Nesta sessão o build completo não pôde ser executado porque o ambiente local disponível possui Java 21 e não tem acesso de rede às dependências/Toolchain Java 25. As alterações de estrutura foram verificadas estaticamente: nenhum arquivo original foi removido, os 36 módulos seguem registrados, os 12 catálogos contêm a nova chave do menu, e os descritores YAML/JSON modificados são válidos.

- Build: removida a restrição de vendor Adoptium/Temurin do Java toolchain; qualquer JDK 25 compatível (incluindo Termux OpenJDK 25) pode compilar o projeto.
