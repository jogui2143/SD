# Sistema de Indexação e Busca Distribuída

Este documento fornece uma visão geral da arquitetura do sistema de indexação e busca distribuída, detalhando os principais componentes, suas funções, e a comunicação entre eles.

## Visão Geral da Arquitetura

O sistema é composto por vários componentes principais, como `Barrel`, `Downloader`, `GatewayFunc` e o servidor `GatewayS`, além de várias classes auxiliares como `PageContent`, `DepthControl` e comparadores. A comunicação entre esses componentes é facilitada por Java RMI e sockets multicast, promovendo operações distribuídas e paralelas.

## Componentes Principais

### 1. Barrel e BarrelFunc

#### Barrel
- **Função:** Responsável pelo armazenamento e gerenciamento do conteúdo das páginas e hyperlinks.
- **Comunicação:** Utiliza Java RMI para operações de busca e armazenamento de conteúdo.
- **Concorrência:** Emprega `ConcurrentHashMap` e `ConcurrentSkipListSet` para um gerenciamento de dados thread-safe.
- **Sockets:** Implementa sockets multicast para comunicação eficiente com o `Downloader`.
- **Escuta e Processamento de Mensagens:** Continuamente escuta mensagens multicast, reconstruindo mensagens fragmentadas e descomprimindo conteúdo.
- **Armazenamento e Indexação:** Armazena e indexa conteúdo de página e hyperlinks, facilitando buscas rápidas e eficientes.
- **Gerenciamento de Estado:** Possui funcionalidades para salvar e carregar seu estado, permitindo persistência entre reinicializações.

#### BarrelFunc
- **Função:** Implementa `BarrelInterface`, fornecendo funcionalidades de busca e recuperação de informações armazenadas.
- **Comunicação:** Comunica-se com o `Barrel` para realizar operações de busca.
- **Performance e Monitoramento:** Monitora o tempo de busca, permitindo análises de desempenho e otimizações futuras.
- **Recuperação de Informações e Metadados:** Oferece métodos para recuperar informações sobre a instância, como o tempo médio de busca e identificador único.
- **Considerações sobre Design:** Demonstra uma clara separação de responsabilidades, com o `Barrel` focado em armazenamento e gerenciamento de dados e o `BarrelFunc` focado na interface de busca e recuperação de informações.

### 2. Downloader

- **Função:** Responsável por baixar e processar o conteúdo de páginas web. Extrai hyperlinks de páginas, gerencia referências de entrada para URLs e envia informações de conteúdo para processamento posterior.

- **Comunicação:** 
  - **RMI:** Recebe URLs para processamento através de uma interface RMI, conectando-se ao `Gateway`.
  - **Sockets Multicast:** Utiliza sockets multicast para enviar informações de conteúdo das páginas processadas, facilitando a distribuição de dados em um ambiente distribuído.

- **Jsoup:** Emprega a biblioteca Jsoup para parsing de HTML, permitindo a extração eficiente de conteúdo e hyperlinks das páginas web.

- **Concorrência:** 
  - Utiliza `ConcurrentHashMap` para gerenciar as contagens de referências de entrada para URLs, assegurando operações thread-safe.
  - Implementa um loop de crawling contínuo, tratando URLs de forma eficiente e responsiva.

- **Processamento de URLs:**
  - Verifica e processa apenas URLs que iniciam com "http://" ou "https://".
  - Para cada URL processada, extrai hyperlinks e cria novos objetos `DepthControl` para cada hyperlink encontrado, adicionando-os à fila de processamento no `Gateway`.

- **Envio de Informações:** 
  - Cria objetos `PageContent` com informações detalhadas das páginas, incluindo título, texto do corpo, URL e hyperlinks.
  - Compacta e serializa esses objetos, enviando-os em seções por meio de sockets multicast, com um ID de mensagem único e informações de rastreamento de partes.

- **Tratamento de Exceções:** 
  - Implementa um método de tratamento de exceções para gerenciar erros de IO e falhas de comunicação, registrando mensagens de erro e detalhes da exceção.

- **Configuração de Rede:**
  - Recupera configurações de endereço multicast, porta e tamanho máximo de pacote do arquivo de configuração da aplicação para a comunicação de rede.

- **Considerações de Design:**
  - O design do `Downloader` reflete a ênfase em eficiência, robustez e escalabilidade, essencial para operações distribuídas e intensivas de processamento de dados.



### 3. GatewayFunc e GatewayS

#### GatewayS
- **Função:** Inicializa e executa o componente gateway em um sistema de rastreamento web distribuído.
- **Iniciação do Servidor:** Cria um registro RMI e associa o objeto `GatewayFunc` a ele, iniciando o gateway no porto RMI especificado.
- **Manuseio de Exceções:** Gerencia exceções relacionadas à inicialização e execução do servidor gateway.

#### GatewayFunc
- **Função:** Gerencia a indexação de URLs e solicitações de busca, atuando como um intermediário na arquitetura.
- **RMI:** Implementa a interface `GatewayInterface`, fornecendo métodos para enfileirar URLs, recuperar novas URLs, realizar buscas e gerenciar barris ativos.
- **Gerenciamento de URLs:** 
  - Utiliza `PriorityBlockingQueue` para gerenciar uma fila de URLs com base na profundidade e no timestamp, permitindo priorização e processamento eficiente.
  - Mantém um registro de frequências de termos de busca e de URLs enfileiradas.
- **Comunicação com Barrels:** 
  - Gerencia conexões com múltiplos barris (Barrels), alternando entre eles usando um mecanismo de round-robin.
  - Implementa um método para conectar-se a barris específicos e recuperar suas interfaces.
- **Busca Distribuída:** 
  - Capaz de realizar buscas por informações relacionadas a termos de busca em todos os barris, com mecanismos de tentativas e backoff exponencial.
  - Procura URLs que conduzem a uma URL específica em vários barris.
- **Monitoramento e Informações de Barrels:** 
  - Fornece informações sobre barris ativos, incluindo ID e tempo médio de busca.
  - Registra informações sobre os termos de busca mais frequentes.
- **Resiliência:** 
  - Implementa um esquema de tentativas com backoff exponencial para a recuperação de falhas durante a busca distribuída e o gerenciamento de barris.


## 4Estruturas de Dados e Concorrência

### Uso de `java.util.concurrent`
O sistema faz um uso extensivo de estruturas de dados do pacote `java.util.concurrent`, garantindo operações seguras em um ambiente multi-threaded. Isso inclui:

- **ConcurrentHashMap:** Utilizado em diversos componentes para armazenamento de dados de forma thread-safe. Por exemplo, o `Downloader` usa para gerenciar contagens de referências de entrada para URLs, e o `GatewayFunc` para manter frequências de termos de busca.

- **PriorityBlockingQueue:** No `GatewayFunc`, essa estrutura é empregada para gerenciar uma fila de URLs com base em critérios de prioridade, como profundidade e timestamp.

- **ConcurrentSkipListSet:** Usado para manter conjuntos ordenados e thread-safe, como visto no retorno de métodos de busca no `GatewayFunc`.

### Gerenciamento de Threads
Vários componentes do sistema são projetados para criar e gerenciar threads de maneira eficiente:

- **Downloader:** Opera em um loop contínuo para processar URLs. Utiliza threads para fazer o download de conteúdo de páginas web e processar hyperlinks.

- **Barrel:** Implementa escuta em sockets multicast em um thread separado, gerenciando a recepção e o armazenamento de dados de página de forma concorrente.

- **GatewayFunc:** Executa operações de busca e recuperação de URLs em um ambiente multi-threaded, lidando com conexões RMI e solicitações de vários barris simultaneamente.

### Considerações de Design
- **Escalabilidade e Performance:** O uso dessas estruturas facilita o escalonamento do sistema e o manejo eficiente de grandes volumes de dados e operações simultâneas.
- **Segurança de Thread:** As escolhas de estruturas de dados e a abordagem de concorrência garantem que o sistema possa operar de forma segura em um ambiente com múltiplas threads, evitando condições de corrida e garantindo a integridade dos dados.
- **Flexibilidade e Manutenção:** A modularidade e a clara separação de responsabilidades facilitam a manutenção e futuras expansões do sistema.

Esses aspectos de estruturas de dados e concorrência são cruciais para o desempenho e a robustez do sistema, permitindo que ele opere de maneira eficiente e confiável em um ambiente distribuído e multithreaded.


## Comunicação de Rede

- **Java RMI:** Essencial para chamadas de método remoto, facilitando a interação entre `GatewayFunc`, `Downloader` e `Barrel`. Permite que esses componentes comuniquem-se em diferentes máquinas virtuais Java, melhorando a escalabilidade e flexibilidade do sistema.
- **Multicast Sockets:** O `Downloader` utiliza sockets multicast para enviar o conteúdo processado (PageContent) para os `Barrels`. Esse método permite a entrega eficiente de mensagens a múltiplos destinatários, reduzindo o tráfego de rede e aumentando a eficiência.

## Organização do Código

- **Modularidade:** O sistema é organizado em módulos bem definidos, cada um com responsabilidades específicas. Isso simplifica a manutenção e facilita futuras expansões ou modificações.
- **Interfaces:** `GatewayInterface` e `BarrelInterface` estabelecem contratos claros entre os componentes, permitindo a substituição ou atualização de componentes sem afetar o restante do sistema.
- **Serialização:** A implementação de Serializable em classes como `PageContent` e `DepthControl` permite que seus objetos sejam transmitidos através da rede de forma eficiente, crucial para operações distribuídas.

## Considerações Finais

A arquitetura atual demonstra robustez e escalabilidade, sendo ideal para o processamento concorrente e distribuído de grandes volumes de dados. Áreas potenciais para futuras melhorias incluem:
- **Balanceamento de Carga Dinâmico:** Otimizar a distribuição de tarefas entre diferentes componentes para melhorar a eficiência.
- **Tratamento de Falhas:** Desenvolver mecanismos mais sofisticados para manejar falhas de comunicação e componentes defeituosos.
- **Otimizações de Performance:** Investigar estratégias para reduzir latência e melhorar o desempenho geral do sistema.

## Protocolo de Comunicação Multicast

- **Endereço Multicast:** Configurado para `225.1.2.3` na porta `7002`, permitindo a comunicação em grupo eficiente entre `Downloader` e `Barrels`.
- **Envio de Informações:** `Downloader` serializa, comprime e envia objetos `PageContent` para o grupo multicast, otimizando a entrega de dados.
- **Recebimento de Informações:** Os `Barrels` escutam o grupo multicast, recebem, deserializam e processam as informações de `PageContent` de forma eficiente.

## Detalhes Sobre o Funcionamento da Componente RMI

- **Métodos Remotos:** Implementação de métodos como `queueUpUrl`, `searchUrls`, `getNewUrl`,`getTopSearchedTerms`,`getActiveBarrels` e `searchURL` permitem a interação remota e a execução de operações cruciais no sistema.
- **Callbacks e Failover:** Embora não explicitamente mencionados, a adoção de callbacks e mecanismos de failover em RMI poderia aumentar a resiliência e a capacidade de resposta do sistema, lidando melhor com falhas de rede e servidores.

---

## Colaboração e Trabalho em Equipe

A equipe adotou uma abordagem colaborativa com reuniões periódicas para alocação equitativa de tarefas e progresso coeso em direção aos objetivos estabelecidos.



# Instruções de Execução do Projeto

Este documento fornece instruções detalhadas sobre como compilar e executar os diferentes componentes do projeto.

## Compilação

Para compilar o projeto, siga as seguintes etapas:

javac -cp "lib/*" -d bin src/main/java/*.java

## Execução

Após a compilação, você pode executar os componentes na seguinte ordem:

1. **Executar o Gateway**:
   - Use este comando para iniciar o gateway:
     ```
     java -cp "bin;lib/*" Gateway
     ```

2. **Executar o Client**:
   - Inicie o cliente com:
     ```
     java -cp "bin;lib/*" Client
     ```

3. **Executar o Downloader**:
   - Para iniciar o downloader, use:
     ```
     java -cp "bin;lib/*" Downloader
     ```

4. **Executar os Barrels**:
   - Finalmente, execute os barrels com:

     ter barrel.port=1099 nas application.properties
     ```
     java -cp "bin;lib/*" Barrel
     ```

     ter barrel.port=1099 nas application.properties
     ```
    java -cp "bin;lib/*" Barrel
     ```




