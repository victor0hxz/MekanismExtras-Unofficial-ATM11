# Mekanism Extras FIX3 — ATM11 0.9.0

Alvo: ATM11 0.9.0, Minecraft 26.1.2, NeoForge 26.1.2.109, Java 25.

Copie somente o JAR desta pasta para a pasta mods da instância. Substitua versões anteriores do mesmo mod; os JARs de dependências já presentes no ATM11 continuam necessários. Cada mod possui sua própria pasta de entrega.

Os testes foram feitos em mundos de desenvolvimento separados da instância do CurseForge. A instância e os saves do usuário não foram modificados. Os testes confirmam os cenários descritos abaixo; ainda é necessário validar no pack completo, especialmente automação prolongada e integrações com outros mods.

O JAR contém as classes e os recursos do mod, sem incluir cópias das dependências ou as classes de testes. Todos os arquivos Java da produção possuem sua classe no JAR; nenhuma família de recursos foi removida para obter a compilação.

Esta versão incorpora as correções FIX1/FIX2 de capacidades, conservação de energia, persistência de calor e modelos dos vinte transmissores no inventário.

JEI atualizado para 29.37.0.99, igual ao ATM11 instalado. Registro de subtipos de itens/tanques/energia dos addons. Corrigido o registro das fábricas avançadas: as estações do Mekanism básico não tinham o atributo procurado pelo registro anterior; as nove categorias agora possuem mapeamento explícito para os quatro níveis de fábricas do Extras. Registradas também as fábricas MoreMachine. Três receitas MoreMachine/AE2 de processadores agora usam o formato moderno de ingredientes, condicionado à presença do AE2.

Teste real do cliente: todos os 100 itens de fábricas Extras presentes com MoreMachine foram encontrados como catalisadores JEI; receitas de processamento disponíveis; oito categorias JEMM; oito níveis de matriz. Cliente e servidor com os mods portados, Mekanism/Generators/Additions/Tools, HNN/Placebo, Pipez, Jade, AE2/GuideME, JEI, Sodium/Iris. A última execução de validação não registrou ERROR no servidor e completou a validação JEI.

Dependências: módulos Mekanism Version Locked da instância. MoreMachine é opcional e habilita suas famílias de fábricas, como no mod original; não está incluído no JAR. Use junto com o JEMM port2 para a calculadora de matrizes dos níveis Extras.

Uma inicialização intermediária de desenvolvimento reportou “Some intrusive holders were not registered: []”. O erro não se repetiu nas duas inicializações seguintes. Preserve os logs de um eventual reaparecimento no pack completo para identificar o registro envolvido.

Auditoria: 415 classes, 1376 arquivos JSON; bytecode Java 25.

JAR: `mekanism_extras-26.1.2-1.4.2-fix3-atm11-0.9.0.jar`

SHA-256: `cf6efa3af7ffb9cb10c03f8499dd229dd328f0018c626ecdb06d6ac46a613781`
