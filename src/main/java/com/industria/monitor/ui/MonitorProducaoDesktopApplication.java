package com.industria.monitor.ui;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.industria.monitor.client.MonitorApiClient;

public class MonitorProducaoDesktopApplication {

	private static final Logger LOGGER = Logger.getLogger(MonitorProducaoDesktopApplication.class.getName());

	private MonitorProducaoDesktopApplication() {}

	public static void main(String[] args) {
		String apiUrl = System.getProperty(
				"monitor.api.url",
				System.getenv().getOrDefault("MONITOR_API_URL", "http://localhost:8080/"));
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				UIManager.put("Button.disabledText", new Color(20, 33, 54));
				new MonitorDashboard(new MonitorApiClient(apiUrl)).setVisible(true);
			} catch (Exception exception) {
				LOGGER.log(Level.SEVERE, "Não foi possível iniciar o painel de produção", exception);
				JOptionPane.showMessageDialog(
						null,
						"Não foi possível iniciar a interface: " + exception.getMessage(),
						"Falha ao iniciar",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}
