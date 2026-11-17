# Monitor de Produção

Aplicação de monitoramento industrial composta por uma API REST Spring Boot e um cliente desktop Swing. O painel consulta a API, acompanha indicadores na janela móvel de cinco minutos, permite cadastrar máquinas e registrar leituras, trata alertas e consulta o histórico de produção.

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
