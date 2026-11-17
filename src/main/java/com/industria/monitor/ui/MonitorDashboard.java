package com.industria.monitor.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.industria.monitor.client.MonitorApiClient;
import com.industria.monitor.dto.alerta.AlertaResponseDTO;
import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.exportacao.ExportadorCsv;
import com.industria.monitor.model.enums.AcaoAlerta;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.StatusProducao;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.simulacao.CenarioSimulacao;
import com.industria.monitor.simulacao.SimuladorMaquina;

public class MonitorDashboard extends JFrame {

	private static final Logger LOGGER = Logger.getLogger(MonitorDashboard.class.getName());
	private static final Color NAVY = new Color(20, 33, 54);
	private static final Color BLUE = new Color(37, 99, 235);
	private static final Color CANVAS = new Color(244, 247, 251);
	private static final Color MUTED = new Color(105, 119, 139);
	private static final Color BORDER = new Color(225, 231, 239);
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final DateTimeFormatter REQUEST_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final MonitorApiClient api;
	private final SimuladorMaquina simulador;
	private final CardLayout pageLayout = new CardLayout();
	private final JPanel pages = new JPanel(pageLayout);
	private final DefaultTableModel overviewModel = tableModel("Máquina", "Produção / 5 min", "Limite", "Unidade", "Situação");
	private final DefaultTableModel machineModel = tableModel("ID", "Máquina", "Unidade", "Limite mínimo", "Intervalo (s)", "Situação");
	private final DefaultTableModel alertModel = tableModel("ID", "Máquina", "Regra", "Estado", "Criado em");
	private final DefaultTableModel historyModel = tableModel("Chave do evento", "Início do intervalo", "Fim do intervalo", "Quantidade", "Unidade", "Recebido em");
	private final List<JButton> navigationButtons = new ArrayList<>();
	private final JTable overviewTable = createTable(overviewModel, 4);
	private final JTable machineTable = createTable(machineModel, 5);
	private final JTable alertTable = createTable(alertModel, 3);
	private final JTable historyTable = createTable(historyModel, -1);
	private final JComboBox<MaquinaResponseDTO> historyMachine = new JComboBox<>();
	private final GraficoIndicadores graficoIndicadores = new GraficoIndicadores();
	private final GraficoHistorico graficoHistorico = new GraficoHistorico();
	private final JLabel machineCount = new JLabel("—");
	private final JLabel normalCount = new JLabel("—");
	private final JLabel attentionCount = new JLabel("—");
	private final JLabel activeAlertCount = new JLabel("—");
	private final JLabel connectionStatus = new JLabel("Conectando à API…");
	private final JLabel lastUpdated = new JLabel("Aguardando primeira atualização");
	private final JButton refreshButton = new JButton("Atualizar agora");
	private final JButton addMachineButton = new JButton("＋  Cadastrar máquina");
	private final JButton addReadingButton = new JButton("＋  Registrar leitura");
	private final JButton acknowledgeButton = new JButton("Reconhecer alerta");
	private final JButton resolveButton = new JButton("Resolver alerta");
	private final JButton historyRefreshButton = new JButton("Consultar histórico");
	private final JButton exportReadingsButton = new JButton("Exportar leituras CSV");
	private final JButton exportIndicatorsButton = new JButton("Exportar indicadores CSV");
	private final JButton simulationButton = new JButton("Iniciar simulação");
	private final JButton generateDatasetButton = new JButton("Gerar CSV com 100 leituras");
	private final JLabel simulationStatus = new JLabel("Simulação parada");
	private final JLabel datasetStatus = new JLabel("Dataset ainda não gerado");
	private final JLabel historyChartTitle = sectionTitle("Quantidade por intervalo");
	private boolean preparandoSimulacao;
	private boolean atualizandoOpcoesDeMaquinas;
	private List<MaquinaResponseDTO> machines = List.of();
	private List<AlertaResponseDTO> alerts = List.of();
	private List<IndicadorProducaoResponseDTO> indicators = List.of();
	private boolean connectionErrorShown;
	private boolean refreshInProgress;

	public MonitorDashboard(MonitorApiClient api) {
		this.api = api;
		this.simulador = new SimuladorMaquina(api);
		setTitle("Monitor de Produção");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(1000, 680));
		setSize(1320, 820);
		setLocationRelativeTo(null);
		getContentPane().setBackground(CANVAS);
		setLayout(new BorderLayout());
		add(buildSidebar(), BorderLayout.WEST);
		add(buildMainContent(), BorderLayout.CENTER);

		refreshButton.addActionListener(event -> atualizarPainel());
		addMachineButton.addActionListener(event -> abrirCadastroMaquina());
		addReadingButton.addActionListener(event -> abrirRegistroLeitura());
		acknowledgeButton.addActionListener(event -> executarAcaoAlerta(AcaoAlerta.RECONHECER));
		resolveButton.addActionListener(event -> executarAcaoAlerta(AcaoAlerta.RESOLVER));
		simulationButton.addActionListener(event -> alternarSimulacao());
		generateDatasetButton.addActionListener(event -> gerarDataset());
		exportReadingsButton.addActionListener(event -> exportarLeituras());
		exportIndicatorsButton.addActionListener(event -> exportarIndicadores());
		historyMachine.addActionListener(event -> {
			if (!atualizandoOpcoesDeMaquinas) {
				atualizarBotaoExportarLeituras();
				graficoHistorico.setLeituras(List.of());
				historyModel.setRowCount(0);
				atualizarTituloGraficoHistorico();
			}
		});
		alertTable.getSelectionModel().addListSelectionListener(event -> atualizarBotoesAlerta());
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				simulador.close();
			}
		});
		new Timer(10000, event -> atualizarPainel()).start();
		atualizarPainel();
	}

	private JPanel buildSidebar() {
		JPanel sidebar = new JPanel(new BorderLayout(0, 24));
		sidebar.setBackground(new Color(236, 241, 247));
		sidebar.setPreferredSize(new Dimension(230, 0));
		sidebar.setBorder(BorderFactory.createEmptyBorder(28, 16, 20, 16));

		JPanel brand = new JPanel(new BorderLayout(0, 4));
		brand.setOpaque(false);
		JLabel mark = new JLabel("  MP  ", SwingConstants.CENTER);
		mark.setOpaque(true);
		mark.setBackground(BLUE);
		mark.setForeground(Color.WHITE);
		mark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
		mark.setPreferredSize(new Dimension(54, 46));
		JLabel title = new JLabel("MONITOR");
		title.setForeground(NAVY);
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
		JLabel subtitle = new JLabel("PRODUÇÃO INDUSTRIAL");
		subtitle.setForeground(MUTED);
		subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
		JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
		brandText.setOpaque(false);
		brandText.add(title);
		brandText.add(subtitle);
		brand.add(mark, BorderLayout.WEST);
		brand.add(brandText, BorderLayout.CENTER);

		JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 8));
		navigation.setOpaque(false);
		navigation.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
		navigation.add(navButton("  Visão geral", "overview", true));
		navigation.add(navButton("  Máquinas", "machines", false));
		navigation.add(navButton("  Alertas", "alerts", false));
		navigation.add(navButton("  Histórico", "history", false));
		navigation.add(navButton("  Simulação", "simulation", false));

		JPanel sideContent = new JPanel(new BorderLayout());
		sideContent.setOpaque(false);
		JLabel section = new JLabel("  OPERAÇÃO");
		section.setForeground(MUTED);
		section.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		sideContent.add(section, BorderLayout.NORTH);
		sideContent.add(navigation, BorderLayout.CENTER);

		JPanel footer = new JPanel(new BorderLayout(0, 8));
		footer.setOpaque(false);
		JLabel apiLabel = new JLabel("SERVIÇO REST");
		apiLabel.setForeground(MUTED);
		apiLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		connectionStatus.setForeground(NAVY);
		connectionStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		footer.add(apiLabel, BorderLayout.NORTH);
		footer.add(connectionStatus, BorderLayout.CENTER);

		sidebar.add(brand, BorderLayout.NORTH);
		sidebar.add(sideContent, BorderLayout.CENTER);
		sidebar.add(footer, BorderLayout.SOUTH);
		return sidebar;
	}

	private JButton navButton(String text, String page, boolean selected) {
		JButton button = new JButton(text);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		styleNavigationButton(button, selected);
		navigationButtons.add(button);
		button.addActionListener(event -> {
			pageLayout.show(pages, page);
			if (page.equals("history")) {
				carregarHistorico();
			}
			for (JButton navigationButton : navigationButtons) {
				styleNavigationButton(navigationButton, navigationButton == button);
			}
		});
		return button;
	}

	private void styleNavigationButton(JButton button, boolean selected) {
		button.setForeground(NAVY);
		button.setFont(new Font(Font.SANS_SERIF, selected ? Font.BOLD : Font.PLAIN, 13));
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorderPainted(true);
		button.setBorder(selected
				? BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0, 3, 0, 0, BLUE),
						BorderFactory.createEmptyBorder(12, 9, 12, 8))
				: BorderFactory.createEmptyBorder(12, 12, 12, 8));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private JPanel buildMainContent() {
		JPanel content = new JPanel(new BorderLayout(0, 20));
		content.setBackground(CANVAS);
		content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
		content.add(buildHeader(), BorderLayout.NORTH);

		pages.setOpaque(false);
		pages.add(buildOverviewPage(), "overview");
		pages.add(buildMachinesPage(), "machines");
		pages.add(buildAlertsPage(), "alerts");
		pages.add(buildHistoryPage(), "history");
		pages.add(buildSimulationPage(), "simulation");
		content.add(pages, BorderLayout.CENTER);
		return content;
	}

	private JPanel buildHeader() {
		JPanel header = new JPanel(new BorderLayout(16, 8));
		header.setOpaque(false);
		JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
		heading.setOpaque(false);
		JLabel title = new JLabel("Painel de produção");
		title.setForeground(NAVY);
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
		lastUpdated.setForeground(MUTED);
		lastUpdated.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		heading.add(title);
		heading.add(lastUpdated);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
		actions.setOpaque(false);
		styleButton(refreshButton, false);
		styleButton(addReadingButton, true);
		styleButton(addMachineButton, true);
		actions.add(refreshButton);
		actions.add(addReadingButton);
		actions.add(addMachineButton);

		header.add(heading, BorderLayout.CENTER);
		header.add(actions, BorderLayout.EAST);
		return header;
	}

	private JPanel buildOverviewPage() {
		JPanel page = new JPanel(new BorderLayout(0, 18));
		page.setOpaque(false);
		JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
		stats.setOpaque(false);
		stats.add(statCard("MÁQUINAS CADASTRADAS", machineCount, new Color(37, 99, 235)));
		stats.add(statCard("PRODUÇÃO NORMAL", normalCount, new Color(16, 148, 103)));
		stats.add(statCard("EM ATENÇÃO", attentionCount, new Color(217, 119, 6)));
		stats.add(statCard("ALERTAS ATIVOS", activeAlertCount, new Color(220, 65, 75)));

		JPanel monitorCard = cardPanel();
		monitorCard.setLayout(new BorderLayout(0, 12));
		JPanel cardHeader = new JPanel(new BorderLayout());
		cardHeader.setOpaque(false);
		cardHeader.add(sectionTitle("Acompanhamento ao vivo"), BorderLayout.WEST);
		JLabel window = new JLabel("JANELA MÓVEL  •  5 MIN");
		window.setForeground(BLUE);
		window.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		styleButton(exportIndicatorsButton, false);
		exportIndicatorsButton.setEnabled(false);
		JPanel cardActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		cardActions.setOpaque(false);
		cardActions.add(window);
		cardActions.add(exportIndicatorsButton);
		cardHeader.add(cardActions, BorderLayout.EAST);
		monitorCard.add(cardHeader, BorderLayout.NORTH);

		JPanel chartCard = cardPanel();
		chartCard.setLayout(new BorderLayout(0, 8));
		chartCard.add(sectionTitle("Produção x limite mínimo — janela de 5 minutos"), BorderLayout.NORTH);
		JScrollPane chartScroll = new JScrollPane(
				graficoIndicadores,
				JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		chartScroll.setBorder(BorderFactory.createLineBorder(BORDER));
		chartScroll.getViewport().setBackground(Color.WHITE);
		chartCard.add(chartScroll, BorderLayout.CENTER);

		JSplitPane overviewSplit = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT, chartCard, tableScroll(overviewTable));
		overviewSplit.setResizeWeight(0.48);
		overviewSplit.setDividerLocation(240);
		overviewSplit.setContinuousLayout(true);
		overviewSplit.setBorder(null);
		overviewSplit.setOpaque(false);
		monitorCard.add(overviewSplit, BorderLayout.CENTER);
		page.add(stats, BorderLayout.NORTH);
		page.add(monitorCard, BorderLayout.CENTER);
		return page;
	}

	private JPanel buildSimulationPage() {
		JPanel page = cardPanel();
		page.setLayout(new BorderLayout(0, 18));
		page.add(sectionTitle("Simulador de máquinas"), BorderLayout.NORTH);

		JPanel scenarios = new JPanel(new GridLayout(1, 3, 12, 0));
		scenarios.setOpaque(false);
		scenarios.add(scenarioCard("Produção normal",
				CenarioSimulacao.PRODUCAO_NORMAL.descricao()));
		scenarios.add(scenarioCard("Baixa produção",
				CenarioSimulacao.BAIXA_PRODUCAO.descricao()));
		scenarios.add(scenarioCard("Ausência de envio",
				CenarioSimulacao.AUSENCIA_ENVIO.descricao()));

		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 8));
		controls.setOpaque(false);
		styleButton(simulationButton, true);
		styleButton(generateDatasetButton, false);
		simulationStatus.setForeground(NAVY);
		datasetStatus.setForeground(MUTED);
		controls.add(simulationButton);
		controls.add(simulationStatus);
		controls.add(generateDatasetButton);
		controls.add(datasetStatus);

		JPanel content = new JPanel(new BorderLayout(0, 20));
		content.setOpaque(false);
		content.add(scenarios, BorderLayout.NORTH);
		content.add(controls, BorderLayout.CENTER);
		page.add(content, BorderLayout.CENTER);
		return page;
	}

	private JPanel scenarioCard(String title, String description) {
		JPanel card = cardPanel();
		card.setLayout(new BorderLayout(0, 10));
		card.add(sectionTitle(title), BorderLayout.NORTH);
		JLabel detail = new JLabel("<html>" + description + "</html>");
		detail.setForeground(MUTED);
		detail.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		card.add(detail, BorderLayout.CENTER);
		return card;
	}

	private JPanel buildMachinesPage() {
		JPanel page = cardPanel();
		page.setLayout(new BorderLayout(0, 14));
		JPanel cardHeader = new JPanel(new BorderLayout());
		cardHeader.setOpaque(false);
		cardHeader.add(sectionTitle("Máquinas monitoradas"), BorderLayout.WEST);
		JLabel hint = new JLabel("Os indicadores são atualizados automaticamente a cada 10 segundos");
		hint.setForeground(MUTED);
		hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		cardHeader.add(hint, BorderLayout.EAST);
		page.add(cardHeader, BorderLayout.NORTH);
		page.add(tableScroll(machineTable), BorderLayout.CENTER);
		return page;
	}

	private JPanel buildAlertsPage() {
		JPanel page = cardPanel();
		page.setLayout(new BorderLayout(0, 14));
		JPanel cardHeader = new JPanel(new BorderLayout());
		cardHeader.setOpaque(false);
		cardHeader.add(sectionTitle("Central de alertas"), BorderLayout.WEST);
		JLabel hint = new JLabel("Reconheça para sinalizar que o alerta está sendo tratado");
		hint.setForeground(MUTED);
		hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		cardHeader.add(hint, BorderLayout.EAST);
		page.add(cardHeader, BorderLayout.NORTH);
		page.add(tableScroll(alertTable), BorderLayout.CENTER);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		styleButton(acknowledgeButton, false);
		styleButton(resolveButton, true);
		acknowledgeButton.setEnabled(false);
		resolveButton.setEnabled(false);
		actions.add(acknowledgeButton);
		actions.add(resolveButton);
		page.add(actions, BorderLayout.SOUTH);
		return page;
	}

	private JPanel buildHistoryPage() {
		JPanel page = cardPanel();
		page.setLayout(new BorderLayout(0, 14));
		JPanel cardHeader = new JPanel(new BorderLayout(12, 0));
		cardHeader.setOpaque(false);
		cardHeader.add(sectionTitle("Histórico de leituras"), BorderLayout.WEST);
		JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		filters.setOpaque(false);
		historyMachine.setPreferredSize(new Dimension(220, 32));
		historyMachine.setRenderer((list, value, index, selected, focused) ->
				new JLabel(value == null ? "" : value.nome() + "  ·  " + value.unidade()));
		styleButton(historyRefreshButton, true);
		styleButton(exportReadingsButton, false);
		exportReadingsButton.setEnabled(false);
		historyRefreshButton.addActionListener(event -> carregarHistorico());
		filters.add(historyMachine);
		filters.add(historyRefreshButton);
		filters.add(exportReadingsButton);
		cardHeader.add(filters, BorderLayout.EAST);
		page.add(cardHeader, BorderLayout.NORTH);
		JPanel chartCard = cardPanel();
		chartCard.setLayout(new BorderLayout(0, 8));
		chartCard.add(historyChartTitle, BorderLayout.NORTH);
		JScrollPane chartScroll = new JScrollPane(
				graficoHistorico,
				JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		chartScroll.setBorder(BorderFactory.createLineBorder(BORDER));
		chartScroll.getViewport().setBackground(Color.WHITE);
		chartCard.add(chartScroll, BorderLayout.CENTER);

		JSplitPane historySplit = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT, chartCard, tableScroll(historyTable));
		historySplit.setResizeWeight(0.43);
		historySplit.setDividerLocation(230);
		historySplit.setContinuousLayout(true);
		historySplit.setBorder(null);
		historySplit.setOpaque(false);
		page.add(historySplit, BorderLayout.CENTER);
		return page;
	}

	private JPanel statCard(String title, JLabel value, Color accent) {
		JPanel card = cardPanel();
		card.setLayout(new BorderLayout(0, 12));
		JLabel label = new JLabel(title);
		label.setForeground(MUTED);
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		value.setForeground(accent);
		value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
				BorderFactory.createEmptyBorder(16, 17, 16, 16)));
		card.add(label, BorderLayout.NORTH);
		card.add(value, BorderLayout.CENTER);
		return card;
	}

	private JPanel cardPanel() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		return panel;
	}

	private JLabel sectionTitle(String text) {
		JLabel label = new JLabel(text);
		label.setForeground(NAVY);
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		return label;
	}

	private JScrollPane tableScroll(JTable table) {
		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createLineBorder(BORDER));
		scroll.getViewport().setBackground(Color.WHITE);
		return scroll;
	}

	private JTable createTable(DefaultTableModel model, int statusColumn) {
		JTable table = new JTable(model);
		table.setRowHeight(38);
		table.setShowVerticalLines(false);
		table.setGridColor(new Color(237, 241, 246));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		table.getTableHeader().setForeground(MUTED);
		table.getTableHeader().setBackground(new Color(248, 250, 252));
		table.getTableHeader().setPreferredSize(new Dimension(0, 38));
		table.setDefaultRenderer(Object.class, new StatusRenderer(statusColumn));
		return table;
	}

	private DefaultTableModel tableModel(String... columns) {
		return new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
	}

	private void styleButton(JButton button, boolean primary) {
		button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, primary ? 13 : 12));
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setForeground(NAVY);
		button.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
		button.setMargin(new Insets(4, 6, 4, 6));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void atualizarPainel() {
		if (refreshInProgress) {
			return;
		}
		refreshInProgress = true;
		runAsync(() -> {
			List<MaquinaResponseDTO> loadedMachines = api.listarMaquinas();
			List<IndicadorProducaoResponseDTO> loadedIndicators = new ArrayList<>();
			for (MaquinaResponseDTO machine : loadedMachines) {
				loadedIndicators.add(api.buscarIndicadores(machine.id()));
			}
			return new Snapshot(loadedMachines, loadedIndicators, api.listarAlertas());
		}, snapshot -> {
			refreshInProgress = false;
			machines = snapshot.machines();
			indicators = snapshot.indicators();
			alerts = snapshot.alerts();
			graficoIndicadores.setDados(machines, indicators);
			exportIndicatorsButton.setEnabled(!machines.isEmpty() && machines.size() == indicators.size());
			popularTabelas();
			atualizarResumo();
			connectionErrorShown = false;
			connectionStatus.setText("●  API conectada");
			connectionStatus.setForeground(new Color(12, 132, 91));
			lastUpdated.setText("Última atualização: " + LocalDateTime.now().format(DATE_FORMAT));
		}, exception -> {
			refreshInProgress = false;
			connectionStatus.setText("●  API indisponível");
			connectionStatus.setForeground(new Color(183, 57, 68));
			if (!connectionErrorShown) {
				connectionErrorShown = true;
				exibirErro("Não foi possível atualizar o painel.", exception);
			}
		});
	}

	private void popularTabelas() {
		Long selectedMachineId = historyMachine.getSelectedItem() instanceof MaquinaResponseDTO selected
				? selected.id() : null;
		atualizandoOpcoesDeMaquinas = true;
		try {
			historyMachine.removeAllItems();
			for (MaquinaResponseDTO machine : machines) {
				historyMachine.addItem(machine);
				if (machine.id().equals(selectedMachineId)) {
					historyMachine.setSelectedItem(machine);
				}
			}
		} finally {
			atualizandoOpcoesDeMaquinas = false;
		}
		Long maquinaSelecionada = historyMachine.getSelectedItem() instanceof MaquinaResponseDTO selected
				? selected.id() : null;
		if (!Objects.equals(selectedMachineId, maquinaSelecionada)) {
			graficoHistorico.setLeituras(List.of());
			historyModel.setRowCount(0);
			atualizarTituloGraficoHistorico();
		}
		atualizarBotaoExportarLeituras();

		overviewModel.setRowCount(0);
		machineModel.setRowCount(0);
		for (int index = 0; index < machines.size(); index++) {
			MaquinaResponseDTO machine = machines.get(index);
			IndicadorProducaoResponseDTO indicator = indicators.get(index);
			String status = statusLabel(indicator.status());
			overviewModel.addRow(new Object[] {
					machine.nome(),
					indicator.quantidadeProduzida().stripTrailingZeros().toPlainString(),
					indicator.limiteMinimo().stripTrailingZeros().toPlainString(),
					indicator.unidade(),
					status
			});
			machineModel.addRow(new Object[] {
					machine.id(),
					machine.nome(),
					machine.unidade(),
					machine.limiteMin().stripTrailingZeros().toPlainString(),
					machine.intervaloSeg() == null ? "—" : machine.intervaloSeg().stripTrailingZeros().toPlainString(),
					status
			});
		}

		alertModel.setRowCount(0);
		for (AlertaResponseDTO alert : alerts) {
			MaquinaResponseDTO machine = machines.stream()
					.filter(item -> item.id().equals(alert.maquinaId()))
					.findFirst()
					.orElse(null);
			alertModel.addRow(new Object[] {
					alert.id(),
					machine == null ? "Máquina " + alert.maquinaId() : machine.nome(),
					ruleLabel(alert.tipoRegra()),
					alertLabel(alert.estado()),
					alert.criadoEm() == null ? "—" : alert.criadoEm().format(DATE_FORMAT)
			});
		}
		atualizarBotoesAlerta();
	}

	private void carregarHistorico() {
		if (!(historyMachine.getSelectedItem() instanceof MaquinaResponseDTO selectedMachine)) {
			return;
		}
		runAsync(() -> api.listarLeituras(selectedMachine.id()), readings -> {
			if (!(historyMachine.getSelectedItem() instanceof MaquinaResponseDTO currentMachine)
					|| !currentMachine.id().equals(selectedMachine.id())) {
				return;
			}
			graficoHistorico.setLeituras(readings);
			historyChartTitle.setText("Quantidade por intervalo — " + currentMachine.nome());
			historyModel.setRowCount(0);
			for (LeituraResponseDTO reading : readings.stream()
					.sorted(java.util.Comparator.comparing(LeituraResponseDTO::inicioIntervalo).reversed())
					.toList()) {
				historyModel.addRow(new Object[] {
						reading.chaveEvento(),
						reading.inicioIntervalo().format(DATE_FORMAT),
						reading.fimIntervalo().format(DATE_FORMAT),
						reading.quantidade().stripTrailingZeros().toPlainString(),
						reading.unidade(),
						reading.criadoEm() == null ? "—" : reading.criadoEm().format(DATE_FORMAT)
				});
			}
		}, exception -> exibirErro("Não foi possível carregar o histórico de leituras.", exception));
	}

	private void exportarLeituras() {
		if (!(historyMachine.getSelectedItem() instanceof MaquinaResponseDTO maquina)) {
			exibirErro("Selecione uma máquina antes de exportar as leituras.", null);
			return;
		}
		Path destino = selecionarArquivoCsv("leituras.csv");
		if (destino == null) {
			return;
		}
		exportReadingsButton.setEnabled(false);
		runAsync(() -> {
			List<LeituraResponseDTO> leituras = api.listarLeituras(maquina.id());
			return ExportadorCsv.exportarLeituras(destino, maquina, leituras);
		}, arquivo -> {
			atualizarBotaoExportarLeituras();
			JOptionPane.showMessageDialog(this, "Leituras exportadas para:\n" + arquivo,
					"Exportação concluída", JOptionPane.INFORMATION_MESSAGE);
		}, exception -> {
			atualizarBotaoExportarLeituras();
			exibirErro("Não foi possível exportar as leituras.", exception);
		});
	}

	private void atualizarBotaoExportarLeituras() {
		exportReadingsButton.setEnabled(historyMachine.getSelectedItem() instanceof MaquinaResponseDTO);
	}

	private void atualizarTituloGraficoHistorico() {
		if (historyMachine.getSelectedItem() instanceof MaquinaResponseDTO maquina) {
			historyChartTitle.setText("Quantidade por intervalo — " + maquina.nome());
		} else {
			historyChartTitle.setText("Quantidade por intervalo");
		}
	}

	private void exportarIndicadores() {
		if (machines.isEmpty() || machines.size() != indicators.size()) {
			exibirErro("Atualize o painel antes de exportar os indicadores.", null);
			return;
		}
		Path destino = selecionarArquivoCsv("indicadores.csv");
		if (destino == null) {
			return;
		}
		List<MaquinaResponseDTO> maquinas = List.copyOf(machines);
		List<IndicadorProducaoResponseDTO> indicadores = List.copyOf(indicators);
		exportIndicatorsButton.setEnabled(false);
		runAsync(() -> ExportadorCsv.exportarIndicadores(destino, maquinas, indicadores), arquivo -> {
			exportIndicatorsButton.setEnabled(true);
			JOptionPane.showMessageDialog(this, "Indicadores exportados para:\n" + arquivo,
					"Exportação concluída", JOptionPane.INFORMATION_MESSAGE);
		}, exception -> {
			exportIndicatorsButton.setEnabled(true);
			exibirErro("Não foi possível exportar os indicadores.", exception);
		});
	}

	private Path selecionarArquivoCsv(String nomePadrao) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Exportar dados para CSV");
		chooser.setSelectedFile(Path.of(nomePadrao).toFile());
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		Path destino = chooser.getSelectedFile().toPath();
		if (!destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
			destino = destino.resolveSibling(destino.getFileName() + ".csv");
		}
		if (Files.exists(destino)
				&& JOptionPane.showConfirmDialog(this, "O arquivo já existe. Deseja substituí-lo?",
						"Confirmar substituição", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
						!= JOptionPane.YES_OPTION) {
			return null;
		}
		return destino;
	}

	private void alternarSimulacao() {
		if (simulador.isExecutando()) {
			simulador.parar();
			simulationButton.setText("Iniciar simulação");
			simulationStatus.setText("Simulação parada");
			return;
		}
		if (preparandoSimulacao) {
			return;
		}
		preparandoSimulacao = true;
		simulationButton.setEnabled(false);
		runAsync(simulador::garantirMaquinas, maquinas -> {
			preparandoSimulacao = false;
			simulationButton.setEnabled(true);
			try {
				simulador.iniciar(
						maquinas,
						mensagem -> SwingUtilities.invokeLater(() -> simulationStatus.setText(mensagem)),
						exception -> SwingUtilities.invokeLater(() -> {
							LOGGER.log(Level.WARNING, "Falha no envio periódico do simulador", exception);
							simulationStatus.setText("Falha no envio; nova tentativa em 60 segundos: "
									+ exception.getMessage());
						}));
				simulationButton.setText("Parar simulação");
				simulationStatus.setText("Simulação ativa: duas máquinas enviam a cada 60 segundos; uma fica sem envio.");
			} catch (RuntimeException exception) {
				exibirErro("Não foi possível iniciar o simulador.", exception);
			}
		}, exception -> {
			preparandoSimulacao = false;
			simulationButton.setEnabled(true);
			exibirErro("Não foi possível preparar as máquinas da simulação.", exception);
		});
	}

	private void gerarDataset() {
		if (preparandoSimulacao) {
			return;
		}
		preparandoSimulacao = true;
		generateDatasetButton.setEnabled(false);
		runAsync(() -> {
			Map<CenarioSimulacao, MaquinaResponseDTO> maquinas = simulador.garantirMaquinas();
			return simulador.exportarConjuntoDe100(maquinas, Path.of("dados", "leituras_sinteticas.csv"));
		}, arquivo -> {
			preparandoSimulacao = false;
			generateDatasetButton.setEnabled(true);
			datasetStatus.setText("100 leituras gravadas em " + arquivo);
		}, exception -> {
			preparandoSimulacao = false;
			generateDatasetButton.setEnabled(true);
			exibirErro("Não foi possível gerar o conjunto sintético.", exception);
		});
	}

	private void atualizarResumo() {
		long normal = indicators.stream().filter(item -> item.status() == StatusProducao.NORMAL).count();
		long attention = indicators.stream().filter(item -> item.status() != StatusProducao.NORMAL).count();
		long activeAlerts = alerts.stream().filter(item -> item.estado() != EstadoAlerta.RESOLVIDO).count();
		machineCount.setText(Integer.toString(machines.size()));
		normalCount.setText(Long.toString(normal));
		attentionCount.setText(Long.toString(attention));
		activeAlertCount.setText(Long.toString(activeAlerts));
	}

	private void abrirCadastroMaquina() {
		JTextField name = formField("Ex.: Prensa hidráulica 01");
		JTextField unit = formField("Ex.: peças/min");
		JTextField minimum = formField("Ex.: 25");
		JTextField interval = formField("60");
		JPanel form = formPanel(
				"Nome da máquina", name,
				"Unidade de produção", unit,
				"Limite mínimo", minimum,
				"Intervalo de leitura (segundos)", interval);

		if (JOptionPane.showConfirmDialog(this, form, "Cadastrar máquina",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			String machineName = required(name.getText(), "Informe o nome da máquina.");
			String machineUnit = required(unit.getText(), "Informe a unidade de produção.");
			var request = new MaquinaRequestDTO(
					machineName,
					machineUnit,
					new java.math.BigDecimal(required(minimum.getText(), "Informe o limite mínimo.").replace(',', '.')),
					new java.math.BigDecimal(required(interval.getText(), "Informe o intervalo.").replace(',', '.')));
			runAsync(() -> {
				api.cadastrarMaquina(request);
				return null;
			}, ignored -> {
				JOptionPane.showMessageDialog(this, "Máquina cadastrada com sucesso.", "Cadastro concluído",
						JOptionPane.INFORMATION_MESSAGE);
				atualizarPainel();
			}, exception -> exibirErro("Não foi possível cadastrar a máquina.", exception));
		} catch (IllegalArgumentException exception) {
			exibirErro("Confira os valores informados.", exception);
		}
	}

	private void abrirRegistroLeitura() {
		if (machines.isEmpty()) {
			exibirErro("Cadastre uma máquina antes de registrar leituras.", null);
			return;
		}
		JComboBox<MaquinaResponseDTO> machine = new JComboBox<>(new DefaultComboBoxModel<>(
				machines.toArray(MaquinaResponseDTO[]::new)));
		machine.setRenderer((list, value, index, selected, focused) ->
				new JLabel(value == null ? "" : value.nome() + "  ·  " + value.unidade()));
		JTextField quantity = formField("Ex.: 120");
		LocalDateTime now = LocalDateTime.now().withNano(0);
		JTextField start = formField(now.minusMinutes(1).format(REQUEST_FORMAT));
		JTextField end = formField(now.format(REQUEST_FORMAT));
		JPanel form = formPanel(
				"Máquina", machine,
				"Quantidade produzida", quantity,
				"Início do intervalo (AAAA-MM-DDTHH:MM:SS)", start,
				"Fim do intervalo (AAAA-MM-DDTHH:MM:SS)", end);

		if (JOptionPane.showConfirmDialog(this, form, "Registrar leitura",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			MaquinaResponseDTO selectedMachine = (MaquinaResponseDTO) machine.getSelectedItem();
			if (selectedMachine == null) {
				throw new IllegalArgumentException("Selecione uma máquina.");
			}
			var request = new LeituraRequestDTO(
					selectedMachine.id(),
					LocalDateTime.parse(required(start.getText(), "Informe o início do intervalo."), REQUEST_FORMAT),
					LocalDateTime.parse(required(end.getText(), "Informe o fim do intervalo."), REQUEST_FORMAT),
					new java.math.BigDecimal(required(quantity.getText(), "Informe a quantidade.").replace(',', '.')),
					selectedMachine.unidade());
			runAsync(() -> {
				api.cadastrarLeitura(request);
				return null;
			}, ignored -> {
				JOptionPane.showMessageDialog(this, "Leitura registrada com sucesso.", "Registro concluído",
						JOptionPane.INFORMATION_MESSAGE);
				atualizarPainel();
			}, exception -> exibirErro("Não foi possível registrar a leitura.", exception));
		} catch (IllegalArgumentException exception) {
			exibirErro("Confira os valores e datas informados.", exception);
		}
	}

	private JTextField formField(String placeholder) {
		JTextField field = new JTextField(placeholder, 24);
		field.setMargin(new Insets(7, 8, 7, 8));
		return field;
	}

	private JPanel formPanel(Object... labelAndFields) {
		JPanel form = new JPanel(new GridLayout(labelAndFields.length / 2, 1, 0, 5));
		form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		for (int index = 0; index < labelAndFields.length; index += 2) {
			JPanel row = new JPanel(new BorderLayout(0, 4));
			row.setOpaque(false);
			JLabel label = new JLabel((String) labelAndFields[index]);
			label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
			row.add(label, BorderLayout.NORTH);
			row.add((Component) labelAndFields[index + 1], BorderLayout.CENTER);
			form.add(row);
		}
		return form;
	}

	private String required(String value, String message) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
		return normalized;
	}

	private void executarAcaoAlerta(AcaoAlerta action) {
		int selectedRow = alertTable.getSelectedRow();
		if (selectedRow < 0 || selectedRow >= alerts.size()) {
			return;
		}
		AlertaResponseDTO alert = alerts.get(alertTable.convertRowIndexToModel(selectedRow));
		runAsync(() -> {
			api.aplicarAcaoAlerta(alert.id(), action);
			return null;
		}, ignored -> atualizarPainel(),
				exception -> exibirErro("Não foi possível atualizar o alerta.", exception));
	}

	private void atualizarBotoesAlerta() {
		int row = alertTable.getSelectedRow();
		boolean selected = row >= 0 && row < alerts.size();
		AlertaResponseDTO alert = selected ? alerts.get(alertTable.convertRowIndexToModel(row)) : null;
		acknowledgeButton.setEnabled(alert != null && alert.estado() == EstadoAlerta.ABERTO);
		resolveButton.setEnabled(alert != null && alert.estado() != EstadoAlerta.RESOLVIDO);
	}

	private <T> void runAsync(Callable<T> operation, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
		refreshButton.setEnabled(false);
		new SwingWorker<T, Void>() {
			@Override
			protected T doInBackground() throws Exception {
				return operation.call();
			}

			@Override
			protected void done() {
				refreshButton.setEnabled(true);
				try {
					onSuccess.accept(get());
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					onFailure.accept(exception);
				} catch (ExecutionException exception) {
					Throwable cause = exception.getCause();
					LOGGER.log(Level.WARNING, "Falha na comunicação com a API", cause);
					onFailure.accept(cause instanceof Exception
							? (Exception) cause : new Exception("Falha inesperada na operação", cause));
				} catch (RuntimeException exception) {
					LOGGER.log(Level.WARNING, "Falha ao atualizar a interface", exception);
					onFailure.accept(exception);
				}
			}
		}.execute();
	}

	private void exibirErro(String message, Exception exception) {
		String detail = exception == null ? "" : "\n" + exception.getMessage();
		JOptionPane.showMessageDialog(this, message + detail, "Atenção", JOptionPane.ERROR_MESSAGE);
	}

	private String statusLabel(StatusProducao status) {
		return switch (status) {
			case NORMAL -> "NORMAL";
			case BAIXA -> "PRODUÇÃO BAIXA";
			case SEM_DADOS -> "SEM DADOS";
		};
	}

	private String alertLabel(EstadoAlerta status) {
		return switch (status) {
			case ABERTO -> "ABERTO";
			case RECONHECIDO -> "RECONHECIDO";
			case RESOLVIDO -> "RESOLVIDO";
		};
	}

	private String ruleLabel(TipoRegra rule) {
		return switch (rule) {
			case LIMITE_MINIMO -> "Baixa produção";
			case AUSENCIA_DADOS -> "Sem dados";
		};
	}

	private record Snapshot(
			List<MaquinaResponseDTO> machines,
			List<IndicadorProducaoResponseDTO> indicators,
			List<AlertaResponseDTO> alerts) {}

	private static class StatusRenderer extends DefaultTableCellRenderer {
		private final int statusColumn;

		private StatusRenderer(int statusColumn) {
			this.statusColumn = statusColumn;
			setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
		}

		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean selected, boolean focused, int row, int column) {
			super.getTableCellRendererComponent(table, value, selected, focused, row, column);
			setForeground(new Color(42, 55, 74));
			if (!selected && column == statusColumn && value != null) {
				String text = value.toString();
				if (text.contains("NORMAL") || text.equals("RESOLVIDO")) {
					setForeground(new Color(12, 132, 91));
				} else if (text.contains("BAIXA") || text.contains("ABERTO")) {
					setForeground(new Color(190, 92, 12));
				} else if (text.contains("SEM DADOS") || text.contains("RECONHECIDO")) {
					setForeground(new Color(183, 57, 68));
				}
				setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
			}
			return this;
		}
	}
}
