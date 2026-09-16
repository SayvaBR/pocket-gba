# Pocket — frontend multissistema (direção de produto e critérios de aceite)

Status: **implementação experimental isolada** em `feat/pocket-multisystem-frontend`. Este documento não é uma alegação de que todos os itens já funcionam no APK. O produto mantém o mesmo `applicationId` para preservar dados da instalação atual.

## Identidade original

Nome de trabalho: **Pocket** (antes Pocket GBA). Biblioteca particular de jogos Android, não clones dos layouts e marcas de outros frontends. As referências Beacon, Cocoon, Daijishō, ES-DE, iiSU e ZNeko servem para observar princípios: abertura direta da biblioteca, navegação com controle, capas com informação legível, personalização contida e retomada. Não copiar logos, arquivos de tema, layouts inteiros nem assets comerciais.

**Assinatura visual:** tela escura azul-noite (`#0B111D`), superfícies em `#141F2D`, tipografia sans forte e legível, um único acento selecionável, imagem como elemento central; títulos, console e no máximo uma ação principal por card. Numa tela de jogo, menu contextual inclui Jogar, Favoritar, Renomear, Informações e Ocultar sem apagar arquivo; não inclui escolher capa. Evitar slogans, banners promocionais, caixas dentro de caixas e informações não verificadas.

**Motion:** 120–180 ms nas interações de foco/seleção e 200–250 ms nas mudanças de seções; nenhuma animação bloqueia toque/controle, e respeitar opção de reduzir movimento. Rive CLI já tem prova de execução separada, mas só conectar assets `.riv` originais depois de teste de APK e alternativa Compose. Não reproduzir arte ou animação de terceiros.

## Primeira biblioteca

Consoles: GBA (`.gba`), GB (`.gb`), GBC (`.gbc`), SNES (`.sfc`/`.smc`) e NDS (`.nds`). GB/GBC são apresentados como família Game Boy, mas mantêm identificação correta do arquivo. ZIP exige identificar sistema com segurança, não adivinhar se o conteúdo é ambíguo. Primeira versão usa a pasta concedida pelo usuário via Storage Access Framework, incluindo subpastas quando o scanner existente permitir. Respeitar URIs persistentes; **não solicitar acesso irrestrito aos arquivos**.

### Capas: somente automáticas

1. Detectar sistema e buscar título/metadados do banco local de ROMs, sem nomes fantasiosos.
2. Consultar URL HTTPS de `Libretro Thumbnails`/`Named_Boxarts` quando há correspondência catalogada. Usar nome de arquivo normalizado como tentativa complementar, com região preservada sempre que confiável.
3. Cache de imagens, tentativas em segundo plano e placeholder original tipográfico com nome real + sistema quando não há arte encontrada; o usuário nunca precisa escolher a capa manualmente.
4. Não fazer correspondência por substring vaga, não mostrar capa de jogo diferente e não prometer 100% de cobertura (hacks, homebrew e ROMs sem catálogo existem).
5. Não embutir ROMs, BIOS ou coleções comerciais de capas no repositório/APK. Validar termos e direitos do provedor de mídia antes de lançar produto comercial. ScreenScraper exige credenciais de desenvolvedor e permissão para uso não totalmente gratuito: **não usar como fallback oculto sem autorização**.
6. Manter overrides legados armazenados para possível migração, mas não oferecer novo seletor manual nem usar a capa escolhida como fonte prioritária.

## Estrutura de produto

- **Início:** um único destaque contextual de jogo recente ou primeiro jogo disponível, continuação e biblioteca. Inclui seletor rápido de plataforma sem mudar a aba em silêncio.
- **Biblioteca:** arte integral em grade, filtro horizontal Todos/GBA/SNES/DS/GB/GBC, contagem real, ordenação estável e importação discreta. Filtrar na fonte de dados paginada, nunca gerar buracos na grade.
- **Busca:** busca local em todas as plataformas sem carregar todo o diretório na UI.
- **Jogo:** foco na capa, título correto, plataforma, status de última partida verificado e ação Jogar. Long-press seguro; não apagar ROM/saves.
- **Ajustes:** aparência, controles, imagem, áudio e armazenamento; sem opções redundantes de trocar capas.
- **Controles físicos:** navegação por D-pad e A/B, feedback de foco e confirmação; implementar e testar, não considerar pronto apenas porque tocar funciona.

## Arquitetura e compatibilidade

**Etapa experimental atual:** interface Pocket em Kotlin/Jetpack Compose; modelos e biblioteca/scan/saves e emulação continuam provenientes do backend Lemuroid/LibretroDroid pinado. Para os quatro grupos iniciais, os cores mGBA, Gambatte, Snes9x e melonDS são habilitados numa branch preview. Isso é um frontend integrado, NÃO um novo motor e NÃO ainda um launcher universal de emuladores externos.

**Etapa seguinte:** extrair `GameCatalog` (ROM URI, digest, console, título, artwork, metadados, último acesso), `ArtworkRepository` e uma interface `GameLauncher` com adaptadores distintos `EmbeddedLibretro` e `ExternalIntent`. Um aplicativo instalado não pode abrir ROMs de outro app por caminho privado; validar intents documentados e compartilhamento seguro de URI antes de anunciar integração RetroArch/melonDS externos. Conservar ID de instalação e migração de saves; licenças GPL-3.0 da base e licenças individuais dos cores continuam aplicáveis.

OpenCode é um agente de desenvolvimento e pode receber issues/PRs no GitHub via `opencode github install` após instalação/conexão e configuração consciente de credenciais. **Nenhuma app de terceiros foi instalada ou autorizada automaticamente**. Preferir PRs pequenos, revisão manual, controles de permissão mínimos e testes Android contra o APK exato.

## Gates que bloqueiam merge/release

- Build Android concluído, APK com APENAS os quatro cores esperados nas ABIs suportadas e instalação por cima da versão atual com assinatura compatível.
- No emulador Android: início, 4 filtros, busca, navigation bar de três botões, gestos e Voltar/retomada sem regressão. Capturas avaliadas visualmente; build não prova beleza.
- Testes com ROMs de teste legalmente distribuíveis de cada plataforma: detectar, exibir nome/plataforma correto, lançar, áudio/entrada, SRAM e save state; para DS verificar toque, telas e BIOS/firmware conforme necessidade.
- Testar pastas mistas, vazias, sem permissão, títulos regionais, ROM sem capa, rede indisponível e biblioteca grande; garantir que ausência de arte não impeça jogar.
- Antes do Play Store: arquivo-fonte GPL/avisos, licenças dos cores, termos das capas, política de privacidade, build release assinado e QA física Android 16.

## Fontes verificadas durante a concepção

- Cocoon: https://github.com/inssekt/CocoonFE
- Beacon: https://play.google.com/store/apps/details?id=com.radikal.gamelauncher
- iiSU: https://github.com/iisu-network/iiSU
- ZNeko: https://github.com/zneko-org/zneko-launcher
- Libretro Thumbnails: https://github.com/libretro-thumbnails/libretro-thumbnails
- API ScreenScraper: https://www.screenscraper.fr/webapi2.php
- OpenCode GitHub: https://opencode.ai/docs/github
- Android Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
