# Pocket — desenvolvimento por agentes

## Estado e fonte da verdade

`main` é o APK GBA estável anterior, com o `applicationId` `com.sayvabr.pocketgba`. `feat/pocket-multisystem-frontend` é um EXPERIMENTO multissistema; não confundir o build desta branch com uma versão aprovada. Este repositório armazena `overrides/` e scripts, não todo o app compilável. O workflow busca Lemuroid no commit `53752bf29bc3f95c50f6c38f70cd4a53450a7098` e LemuroidCores no commit `fee2e824525daa22bcf318f96127fe43fa8a15ad`, aplica patches e compila Gradle.

## Visão do produto

O Pocket será um frontend Android original, clean, escuro e centrado nas CAPAS. Primeiros sistemas: GBA, SNES, Nintendo DS e GB/GBC. Não copiar telas, temas, nomes ou assets de Beacon, Cocoon, Daijishō, ES-DE, iiSU ou ZNeko; observar apenas padrões de UX. Evitar marketing e informações inventadas. A aba Sistemas genérica permanece abolida: biblioteca integrada e filtros de plataforma.

**Capas SEM configuração manual:** normalizar título e região com segurança, consultar metadados/Libretro Thumbnails por HTTPS, cachear e apresentar placeholder legível se faltar arte. NUNCA inventar arte ou forçar match de jogo diferente. O seletor manual de capa legado deve ser retirado da UI sem apagar dados pessoais já existentes. Respeitar licenças de artes e termos de APIs antes da publicação comercial; não adicionar ROMs nem BIOS de terceiros.

O backend atual permanece derivado do Lemuroid (GPL-3.0) com LibretroDroid e cores mGBA, Snes9x, Gambatte e melonDS. Não chamar esta fase de frontend universal externo nem motor próprio. A arquitetura futura deve extrair catálogo, repositório de metadados e `GameLauncher` com adaptadores internos/externos; integração externa precisa validar intents e compartilhamento SAF autorizado pelo usuário. NÃO alterar pacote, identidade de assinatura ou arquivos de save por conveniência.

## Localização do trabalho e build

- Patch de plataforma/GBA em `scripts/prepare_upstream.sh` é aplicado ANTES do patch multissistema.
- Interface Pocket original nasce de `overrides/lemuroid-app/.../PocketShell.kt`, modificada por `scripts/apply_library_personalization.py`, depois `scripts/apply_multisystem_frontend.py` na branch.
- Workflow preview `.github/workflows/build-frontend-preview.yml` instala quatro cores pinados via checkout esparso, aplica patches sequenciais, compila e verifica APK. Novas alterações de source devem entrar no workflow, caso contrário NÃO chegam ao APK. Nunca editar `.work/upstream` como se fosse código versionado.
- Documentação completa, critérios e limitações em `docs/FRONTEND_V1.md`.

## Gates obrigatórios

1. Build limpo com `assembleFreeBundleDebug` e verificação do APK, assinatura compatível, bibliotecas nativas para ABIs anunciadas. Nenhum merge se compilação não passar.
2. Smoke Android com APK EXATO gerado: iniciar, biblioteca vazia e populada, filtros Todos/GBA/SNES/DS/GB/GBC, importação SAF, pesquisa, Voltar entre abas, retomada, nav três botões e gestos; capturas de tela verificadas visualmente. Build sozinho não prova UI ou jogabilidade.
3. ROMs de teste legais de cada plataforma: identificação, arte (online/offline), jogabilidade, áudio, controles, save/load; DS testar duas telas, toque e requisito de BIOS/firmware.
4. Teste sem rede, catálogo desconhecido, nomes regionais, homebrew, diretório misto, remoção de permissão; ausência de capa jamais bloqueia Jogar. Não apagar ROMs ou saves em ação de ocultar.
5. Publicação somente depois de revisão de GPL/cores/fontes e direitos das capas, política de privacidade, build assinado release e QA Android real.

## Agentes e motion

OpenCode pode trabalhar por GitHub, mas instalar app e conceder credenciais exige ação explícita do usuário. Não presumir acesso à conta de modelos e não commitar tokens. Abrir PRs pequenos e revisar diffs. Rive CLI passou um experimento isolado no GitHub, mas animação NÃO está integrada ao Android. Quando integrado: motion original discreto, sem input latency e com reduced-motion/fallback Compose. RetroAchievements também não está implementado: jamais simular conquistas.
