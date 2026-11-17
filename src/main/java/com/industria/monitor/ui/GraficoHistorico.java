package com.industria.monitor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;

import com.industria.monitor.dto.leitura.LeituraResponseDTO;

public class GraficoHistorico extends JComponent {

	private static final Color BLUE = new Color(37, 99, 235);
	private static final Color GRID = new Color(229, 234, 241);
	private static final Color TEXT = new Color(91, 105, 124);
	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
	private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private List<LeituraResponseDTO> leituras = List.of();
	private int indiceHover = -1;
	private Point posicaoHover;

	public GraficoHistorico() {
		setOpaque(true);
		setBackground(Color.WHITE);
		setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		setPreferredSize(new Dimension(740, 235));
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent event) {
				atualizarHover(event);
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent event) {
				limparHover();
			}
		});
	}

	public void setLeituras(List<LeituraResponseDTO> leituras) {
		this.leituras = leituras.stream()
				.sorted(Comparator.comparing(LeituraResponseDTO::inicioIntervalo))
				.toList();
		limparHover();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			if (leituras.isEmpty()) {
				desenharMensagem(g, "Sem leituras para exibir");
				return;
			}

			int left = 64;
			int right = 24;
			int top = 38;
			int bottom = 48;
			int larguraGrafico = Math.max(1, getWidth() - left - right);
			int alturaGrafico = Math.max(1, getHeight() - top - bottom);
			int baseY = top + alturaGrafico;
			double maximo = leituras.stream()
					.mapToDouble(leitura -> valor(leitura.quantidade()))
					.max()
					.orElse(1);
			maximo = maximo > 0 ? maximo * 1.15 : 1;
			g.setColor(BLUE);
			g.setStroke(new BasicStroke(2f));
			g.drawLine(12, 18, 26, 18);
			g.fillOval(17, 15, 6, 6);
			g.setColor(TEXT);
			g.drawString("Quantidade por intervalo", 32, 22);

			for (int tick = 0; tick <= 4; tick++) {
				int y = baseY - alturaGrafico * tick / 4;
				double valorTick = maximo * tick / 4;
				g.setColor(GRID);
				g.drawLine(left, y, left + larguraGrafico, y);
				String texto = String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", valorTick);
				g.setColor(TEXT);
				FontMetrics metrics = g.getFontMetrics();
				g.drawString(texto, left - metrics.stringWidth(texto) - 8, y + 4);
			}

			Path2D linha = new Path2D.Double();
			Path2D area = new Path2D.Double();
			for (int indice = 0; indice < leituras.size(); indice++) {
				double x = leituras.size() == 1
						? left + larguraGrafico / 2.0
						: left + (double) indice / (leituras.size() - 1) * larguraGrafico;
				double y = baseY - valor(leituras.get(indice).quantidade()) / maximo * alturaGrafico;
				if (indice == 0) {
					linha.moveTo(x, y);
					area.moveTo(x, baseY);
					area.lineTo(x, y);
				} else {
					linha.lineTo(x, y);
					area.lineTo(x, y);
				}
			}
			double ultimoX = leituras.size() == 1
					? left + larguraGrafico / 2.0
					: left + larguraGrafico;
			area.lineTo(ultimoX, baseY);
			area.closePath();
			g.setColor(new Color(37, 99, 235, 32));
			g.fill(area);
			g.setColor(BLUE);
			g.setStroke(new BasicStroke(2f));
			g.draw(linha);
			for (int indice = 0; indice < leituras.size(); indice++) {
				if (leituras.size() > 80 && indice % Math.max(1, leituras.size() / 40) != 0) {
					continue;
				}
				double x = leituras.size() == 1
						? left + larguraGrafico / 2.0
						: left + (double) indice / (leituras.size() - 1) * larguraGrafico;
				double y = baseY - valor(leituras.get(indice).quantidade()) / maximo * alturaGrafico;
				g.fillOval((int) x - 3, (int) y - 3, 6, 6);
			}

			g.setColor(TEXT);
			int quantidadeRotulos = Math.min(5, leituras.size());
			for (int rotulo = 0; rotulo < quantidadeRotulos; rotulo++) {
				int indice = quantidadeRotulos == 1 ? 0
						: (int) Math.round((double) rotulo * (leituras.size() - 1) / (quantidadeRotulos - 1));
				LocalDateTime inicio = leituras.get(indice).inicioIntervalo();
				String texto = inicio == null ? "" : HORA.format(inicio);
				double x = leituras.size() == 1
						? left + larguraGrafico / 2.0
						: left + (double) indice / (leituras.size() - 1) * larguraGrafico;
				FontMetrics metrics = g.getFontMetrics();
				g.drawString(texto, (int) x - metrics.stringWidth(texto) / 2, baseY + 20);
			}
			desenharDetalheHover(g);
			g.setColor(GRID);
			g.drawLine(left, top, left, baseY);
		} finally {
			g.dispose();
		}
	}

	private void atualizarHover(MouseEvent event) {
		int left = 64;
		int right = 24;
		int top = 38;
		int bottom = 48;
		int larguraGrafico = Math.max(1, getWidth() - left - right);
		int baseY = getHeight() - bottom;
		if (leituras.isEmpty() || event.getX() < left || event.getX() > left + larguraGrafico
				|| event.getY() < top || event.getY() > baseY) {
			limparHover();
			return;
		}

		indiceHover = leituras.size() == 1 ? 0
				: (int) Math.round((double) (event.getX() - left) * (leituras.size() - 1) / larguraGrafico);
		indiceHover = Math.max(0, Math.min(leituras.size() - 1, indiceHover));
		posicaoHover = event.getPoint();
		repaint();
	}

	private void limparHover() {
		if (indiceHover != -1) {
			indiceHover = -1;
			posicaoHover = null;
			repaint();
		}
	}

	private void desenharDetalheHover(Graphics2D g) {
		if (indiceHover < 0 || posicaoHover == null || indiceHover >= leituras.size()) {
			return;
		}
		LeituraResponseDTO leitura = leituras.get(indiceHover);
		int altura = 68;
		Rectangle areaVisivel = areaVisivel();
		int largura = Math.min(290, areaVisivel.width - 12);
		int x = Math.min(posicaoHover.x + 12, areaVisivel.x + areaVisivel.width - largura - 6);
		int y = Math.min(posicaoHover.y + 12, areaVisivel.y + areaVisivel.height - altura - 6);
		x = Math.max(areaVisivel.x + 6, x);
		y = Math.max(areaVisivel.y + 6, y);

		g.setColor(new Color(255, 255, 255, 248));
		g.fillRoundRect(x, y, largura, altura, 10, 10);
		g.setColor(new Color(190, 204, 222));
		g.drawRoundRect(x, y, largura, altura, 10, 10);
		g.setColor(TEXT);
		g.setFont(getFont().deriveFont(Font.BOLD, 10f));
		String intervalo = formatar(leitura.inicioIntervalo()) + " – " + formatar(leitura.fimIntervalo());
		g.drawString(abreviar(intervalo, 40), x + 10, y + 18);
		g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
		g.drawString("Quantidade: " + texto(leitura.quantidade()) + " "
				+ abreviar(leitura.unidade(), 20), x + 10, y + 36);
		g.drawString("Evento: " + abreviar(leitura.chaveEvento(), 32), x + 10, y + 54);
	}

	private Rectangle areaVisivel() {
		Rectangle area = getVisibleRect();
		return area.isEmpty() ? new Rectangle(0, 0, getWidth(), getHeight()) : area;
	}

	private void desenharMensagem(Graphics2D g, String mensagem) {
		g.setColor(TEXT);
		g.setFont(getFont());
		FontMetrics metrics = g.getFontMetrics();
		g.drawString(mensagem, Math.max(12, (getWidth() - metrics.stringWidth(mensagem)) / 2),
				getHeight() / 2);
	}

	private static double valor(BigDecimal valor) {
		if (valor == null || valor.signum() <= 0) {
			return 0;
		}
		double convertido = valor.doubleValue();
		return Double.isFinite(convertido) ? convertido : 0;
	}

	private static String formatar(LocalDateTime data) {
		return data == null ? "—" : DATA_HORA.format(data);
	}

	private static String texto(BigDecimal valor) {
		return valor == null ? "—" : valor.toPlainString();
	}

	private static String abreviar(String texto, int tamanhoMaximo) {
		if (texto == null || texto.length() <= tamanhoMaximo) {
			return texto == null ? "" : texto;
		}
		return texto.substring(0, tamanhoMaximo - 1) + "…";
	}

}
