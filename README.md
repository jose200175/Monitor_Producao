# Monitor de Produção

Aplicação de monitoramento industrial composta por uma API REST Spring Boot e um cliente desktop Swing. O painel consulta a API, acompanha indicadores na janela móvel de cinco minutos, compara produção e limite mínimo em um gráfico, exibe a série temporal das leituras no histórico, permite cadastrar máquinas e registrar leituras e trata alertas. As legendas identificam as cores e, ao passar o mouse sobre os gráficos, são exibidos os valores correspondentes.

## Executar localmente

Use Java 17 ou superior. Em um terminal, inicie a API:

```powershell
.\mvnw.cmd spring-boot:run
```

Em outro terminal, inicie o cliente Swing:

```powershell
.\mvnw.cmd exec:java
```

O cliente usa `http://localhost:8080/` por padrão. Para apontar para outra instância da API, configure `MONITOR_API_URL` antes de iniciar o cliente:

```powershell
$env:MONITOR_API_URL = "http://localhost:8080/"
.\mvnw.cmd exec:java
```

O painel atualiza os dados periodicamente e também oferece o botão **Atualizar agora**. As requisições HTTP são executadas em segundo plano para manter a interface responsiva.

## Executar os testes

No PowerShell, abra a pasta raiz do projeto e execute todos os testes com o Maven Wrapper:

```powershell
.\mvnw.cmd test
```

Para limpar os resultados anteriores antes de testar:

```powershell
.\mvnw.cmd clean test
```

É possível executar uma classe específica pelo nome, por exemplo:

```powershell
.\mvnw.cmd -Dtest=MonitorProducaoRegrasTests test
.\mvnw.cmd -Dtest=ExportadorCsvTests test
```

Os resultados detalhados ficam em `target/surefire-reports`. Use Java 17 ou superior e configure `JAVA_HOME` para o JDK instalado.

## Simulador de máquinas

Na seção **Simulação** do cliente, **Iniciar simulação** cadastra (se necessário) três máquinas e envia leituras a cada 60 segundos para os cenários de produção normal e baixa. A máquina do cenário de ausência fica cadastrada sem receber leituras para permitir a detecção de ausência pela API. **Gerar CSV com 100 leituras** cria `dados/leituras_sinteticas.csv`, com 100 registros, intervalo, quantidade, unidade e chave do evento.

Para instalações existentes usando o perfil `prod`, aplique uma vez o script `src/main/resources/db/migration/V2__chave_evento_leitura.sql` no banco antes de iniciar a nova versão. O perfil `dev` atualiza a estrutura automaticamente.

## Exportar dados

Na visão geral, use **Exportar indicadores CSV** para salvar os indicadores exibidos. No histórico, selecione uma máquina, consulte suas leituras e use **Exportar leituras CSV**. Os arquivos são gravados em UTF-8; campos com vírgulas, aspas ou quebras de linha são escapados no formato CSV.

## Importar leituras e configurar o monitoramento

Na barra lateral, use **Importação CSV** para selecionar um arquivo de leituras. O cabeçalho deve conter `maquina_id`, `inicio_intervalo`, `fim_intervalo`, `quantidade` e `unidade`; `chave_evento` é opcional. Linhas válidas são enviadas à API e linhas rejeitadas aparecem na tabela do painel com o número da linha e o motivo.

Use **Configurações** para salvar ou restaurar o arquivo binário `monitor-config.dat`. O arquivo contém assinatura, versão, janela de indicadores, limite de ausência e os limites e intervalos das máquinas monitoradas. Se o cliente Swing já estiver aberto, feche-o e execute novamente `.\mvnw.cmd exec:java` para carregar a interface atualizada.

Na página **Configurações**, informe uma quantidade inteira de segundos no campo **Nova janela** e use **Aplicar janela**. A API recalcula imediatamente os indicadores usando esse período. Ao salvar, a janela atual é gravada no arquivo binário; ao restaurar, ela é reaplicada automaticamente.
