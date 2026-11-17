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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.model.enums.StatusProducao;

public class GraficoIndicadores extends JComponent {

	private static final Color GRID = new Color(229, 234, 241);
	private static final Color TEXT = new Color(91, 105, 124);
	private static final Color NORMAL = new Color(16, 148, 103);
	private static final Color BAIXA = new Color(217, 119, 6);
	private static final Color SEM_DADOS = new Color(183, 57, 68);
	private static final Color LIMITE = new Color(91, 111, 139);
	private static final DecimalFormat NUMERO = new DecimalFormat("#,##0.##");

	private List<Ponto> pontos = List.of();
	private int indiceHover = -1;
	private boolean hoverLimite;
	private Point posicaoHover;

	public GraficoIndicadores() {
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

	public void setDados(
			List<MaquinaResponseDTO> maquinas,
			List<IndicadorProducaoResponseDTO> indicadores) {
		Map<Long, IndicadorProducaoResponseDTO> indicadoresPorMaquina = new HashMap<>();
		for (IndicadorProducaoResponseDTO indicador : indicadores) {
			indicadoresPorMaquina.put(indicador.maquinaId(), indicador);
		}

		List<Ponto> novosPontos = new ArrayList<>();
		for (MaquinaResponseDTO maquina : maquinas) {
			IndicadorProducaoResponseDTO indicador = indicadoresPorMaquina.get(maquina.id());
			if (indicador != null) {
				novosPontos.add(new Ponto(
						maquina.nome(),
						indicador.unidade(),
						valor(indicador.quantidadeProduzida()),
						valor(indicador.limiteMinimo()),
						indicador.status()));
			}
		}
		pontos = List.copyOf(novosPontos);
		limparHover();
		setPreferredSize(new Dimension(Math.max(740, pontos.size() * 105 + 90), 235));
		revalidate();
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
			if (pontos.isEmpty()) {
				desenharMensagem(g, "Aguardando indicadores de produção");
				return;
			}

			int left = 62;
			int right = 22;
			int top = 40;
			int bottom = 54;
			int larguraGrafico = Math.max(1, getWidth() - left - right);
			int alturaGrafico = Math.max(1, getHeight() - top - bottom);
			int baseY = top + alturaGrafico;
			double maximo = pontos.stream()
					.mapToDouble(ponto -> Math.max(ponto.producao(), ponto.limite()))
					.max()
					.orElse(1);
			maximo = maximo > 0 ? maximo * 1.15 : 1;

			g.setFont(getFont());
			int legendaX = desenharLegenda(g, NORMAL, "Produção normal", 10);
			legendaX = desenharLegenda(g, BAIXA, "Baixa produção", legendaX);
			legendaX = desenharLegenda(g, SEM_DADOS, "Sem dados", legendaX);
			desenharLegenda(g, LIMITE, "Limite mínimo", legendaX);

			for (int tick = 0; tick <= 4; tick++) {
				int y = baseY - alturaGrafico * tick / 4;
				double valorTick = maximo * tick / 4;
				g.setColor(GRID);
				g.drawLine(left, y, left + larguraGrafico, y);
				g.setColor(TEXT);
				String texto = NUMERO.format(valorTick);
				FontMetrics metrics = g.getFontMetrics();
				g.drawString(texto, left - metrics.stringWidth(texto) - 8, y + 4);
			}

			double larguraGrupo = (double) larguraGrafico / pontos.size();
			int larguraBarra = (int) Math.max(5, Math.min(24, larguraGrupo * 0.24));
			for (int indice = 0; indice < pontos.size(); indice++) {
				Ponto ponto = pontos.get(indice);
				int centro = left + (int) Math.round((indice + 0.5) * larguraGrupo);
				int producaoAltura = (int) Math.round(ponto.producao() / maximo * alturaGrafico);
				int limiteAltura = (int) Math.round(ponto.limite() / maximo * alturaGrafico);
				int producaoX = centro - larguraBarra - 2;
				int limiteX = centro + 2;
				g.setColor(corStatus(ponto.status()));
				g.fillRoundRect(producaoX, baseY - producaoAltura,
						larguraBarra, producaoAltura, 5, 5);
				g.setColor(LIMITE);
				g.fillRoundRect(limiteX, baseY - limiteAltura,
						larguraBarra, limiteAltura, 5, 5);

				String nome = abreviar(ponto.nome(), Math.max(8, (int) (larguraGrupo / 7)));
				FontMetrics metrics = g.getFontMetrics();
				g.setColor(TEXT);
				g.drawString(nome, centro - metrics.stringWidth(nome) / 2, baseY + 19);
			}
			desenharDetalheHover(g);
			g.setColor(GRID);
			g.setStroke(new BasicStroke(1f));
			g.drawLine(left, top, left, baseY);
		} finally {
			g.dispose();
		}
	}

	private int desenharLegenda(Graphics2D g, Color cor, String texto, int x) {
		g.setColor(cor);
		g.fillRect(x, 12, 10, 10);
		g.setColor(TEXT);
		g.drawString(texto, x + 15, 21);
		return x + 19 + g.getFontMetrics().stringWidth(texto) + 12;
	}

	private void atualizarHover(MouseEvent event) {
		int left = 62;
		int right = 22;
		int top = 40;
		int bottom = 54;
		int larguraGrafico = Math.max(1, getWidth() - left - right);
		int baseY = getHeight() - bottom;
		if (pontos.isEmpty() || event.getX() < left || event.getX() > left + larguraGrafico
				|| event.getY() < top || event.getY() > baseY) {
			limparHover();
			return;
		}

		indiceHover = Math.min(pontos.size() - 1,
				(event.getX() - left) * pontos.size() / larguraGrafico);
		int centro = left + (int) Math.round((indiceHover + 0.5) * larguraGrafico / pontos.size());
		hoverLimite = event.getX() >= centro;
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
		if (indiceHover < 0 || posicaoHover == null || indiceHover >= pontos.size()) {
			return;
		}
		Ponto ponto = pontos.get(indiceHover);
		int altura = 82;
		Rectangle areaVisivel = areaVisivel();
		int largura = Math.min(280, areaVisivel.width - 12);
		int x = Math.min(posicaoHover.x + 12, areaVisivel.x + areaVisivel.width - largura - 6);
		int y = Math.min(posicaoHover.y + 12, areaVisivel.y + areaVisivel.height - altura - 6);
		x = Math.max(areaVisivel.x + 6, x);
		y = Math.max(areaVisivel.y + 6, y);

		g.setColor(new Color(255, 255, 255, 248));
		g.fillRoundRect(x, y, largura, altura, 10, 10);
		g.setColor(new Color(190, 204, 222));
		g.drawRoundRect(x, y, largura, altura, 10, 10);
		g.setColor(TEXT);
		g.setFont(getFont().deriveFont(Font.BOLD, 11f));
		g.drawString(abreviar(ponto.nome(), 34), x + 10, y + 18);
		g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
		String barra = hoverLimite ? "Limite mínimo" : "Produção na janela";
		double valorBarra = hoverLimite ? ponto.limite() : ponto.producao();
		g.drawString(barra + ": " + NUMERO.format(valorBarra) + " " + ponto.unidade(), x + 10, y + 36);
		g.drawString("Limite mínimo: " + NUMERO.format(ponto.limite()) + " " + ponto.unidade(),
				x + 10, y + 52);
		g.drawString("Situação: " + rotuloStatus(ponto.status()), x + 10, y + 68);
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

	private static Color corStatus(StatusProducao status) {
		if (status == StatusProducao.NORMAL) {
			return NORMAL;
		}
		if (status == StatusProducao.BAIXA) {
			return BAIXA;
		}
		return SEM_DADOS;
	}

	private static String rotuloStatus(StatusProducao status) {
		if (status == StatusProducao.NORMAL) {
			return "Normal";
		}
		if (status == StatusProducao.BAIXA) {
			return "Baixa produção";
		}
		return "Sem dados";
	}

	private static double valor(BigDecimal valor) {
		if (valor == null || valor.signum() <= 0) {
			return 0;
		}
		double convertido = valor.doubleValue();
		return Double.isFinite(convertido) ? convertido : 0;
	}

	private static String abreviar(String texto, int tamanhoMaximo) {
		if (texto == null || texto.length() <= tamanhoMaximo) {
			return texto == null ? "" : texto;
		}
		return texto.substring(0, Math.max(1, tamanhoMaximo - 1)) + "…";
	}

	private record Ponto(String nome, String unidade, double producao, double limite, StatusProducao status) {}
}
