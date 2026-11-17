package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.junit.jupiter.api.Test;

import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.model.enums.StatusProducao;
import com.industria.monitor.ui.GraficoHistorico;
import com.industria.monitor.ui.GraficoIndicadores;

class GraficosProducaoTests {

	@Test
	void desenhaBarraDeIndicadorComCorDoEstado() {
		GraficoIndicadores grafico = new GraficoIndicadores();
		MaquinaResponseDTO maquina = new MaquinaResponseDTO(
				1L, "Prensa A", "un", BigDecimal.TEN, BigDecimal.valueOf(60));
		IndicadorProducaoResponseDTO indicador = new IndicadorProducaoResponseDTO(
				1L, LocalDateTime.now().minusMinutes(5), LocalDateTime.now(),
				new BigDecimal("14"), BigDecimal.TEN, "un", StatusProducao.NORMAL);

		grafico.setDados(List.of(maquina), List.of(indicador));
		BufferedImage imagem = desenhar(grafico);

		assertTrue(contarPixels(imagem, new Color(16, 148, 103)) > 0);
	}

	@Test
	void desenhaLinhaParaHistoricoDeLeituras() {
		GraficoHistorico grafico = new GraficoHistorico();
		grafico.setLeituras(List.of(
				leitura(LocalDateTime.now().minusMinutes(2), "8"),
				leitura(LocalDateTime.now().minusMinutes(1), "12")));

		BufferedImage imagem = desenhar(grafico);

		assertTrue(contarPixels(imagem, new Color(37, 99, 235)) > 0);
	}

	@Test
	void exibeDetalheVisualAoPassarOMouseSobreOsGraficos() {
		GraficoIndicadores indicadores = new GraficoIndicadores();
		indicadores.setDados(
				List.of(new MaquinaResponseDTO(1L, "Prensa A", "un", BigDecimal.TEN, BigDecimal.valueOf(60))),
				List.of(new IndicadorProducaoResponseDTO(
						1L, LocalDateTime.now().minusMinutes(5), LocalDateTime.now(),
						new BigDecimal("14"), BigDecimal.TEN, "un", StatusProducao.NORMAL)));
		dispararMovimentoDoMouse(indicadores, 380, 100);

		GraficoHistorico historico = new GraficoHistorico();
		historico.setLeituras(List.of(leitura(LocalDateTime.now().minusMinutes(1), "8")));
		dispararMovimentoDoMouse(historico, 400, 100);

		assertTrue(contarPixels(desenhar(indicadores), new Color(190, 204, 222)) > 0);
		assertTrue(contarPixels(desenhar(historico), new Color(190, 204, 222)) > 0);
	}

	@Test
	void posicionaDetalheAcimaDoMouseQuandoEleEstaPertoDaBordaInferior() {
		GraficoIndicadores indicadores = new GraficoIndicadores();
		indicadores.setDados(
				List.of(new MaquinaResponseDTO(1L, "Prensa A", "un", BigDecimal.TEN, BigDecimal.valueOf(60))),
				List.of(new IndicadorProducaoResponseDTO(
						1L, LocalDateTime.now().minusMinutes(5), LocalDateTime.now(),
						new BigDecimal("14"), BigDecimal.TEN, "un", StatusProducao.NORMAL)));
		assertDetalheEncaixadoNaViewport(indicadores, 280, 82);

		GraficoHistorico historico = new GraficoHistorico();
		historico.setLeituras(List.of(leitura(LocalDateTime.now().minusMinutes(1), "8")));
		assertDetalheEncaixadoNaViewport(historico, 290, 68);
	}

	@Test
	void limpaFundoDosGraficosAoRepintar() {
		assertFundoLimpo(new GraficoHistorico());
		assertFundoLimpo(new GraficoIndicadores());
	}

	private void assertFundoLimpo(javax.swing.JComponent grafico) {
		grafico.setSize(740, 235);
		BufferedImage imagem = new BufferedImage(740, 235, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = imagem.createGraphics();
		try {
			graphics.setColor(Color.MAGENTA);
			graphics.fillRect(0, 0, imagem.getWidth(), imagem.getHeight());
			grafico.paint(graphics);
		} finally {
			graphics.dispose();
		}

		assertEquals(Color.WHITE.getRGB(), imagem.getRGB(0, 0));
	}

	private BufferedImage desenhar(javax.swing.JComponent grafico) {
		grafico.setSize(740, 235);
		BufferedImage imagem = new BufferedImage(740, 235, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = imagem.createGraphics();
		try {
			grafico.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return imagem;
	}

	private void dispararMovimentoDoMouse(javax.swing.JComponent grafico, int x, int y) {
		grafico.setSize(740, 235);
		MouseEvent event = new MouseEvent(grafico, MouseEvent.MOUSE_MOVED,
				System.currentTimeMillis(), 0, x, y, 0, false);
		for (java.awt.event.MouseMotionListener listener : grafico.getMouseMotionListeners()) {
			listener.mouseMoved(event);
		}
	}

	private void assertDetalheEncaixadoNaViewport(
			javax.swing.JComponent grafico, int larguraDetalhe, int alturaDetalhe) {
		JScrollPane scroll = new JScrollPane(
				grafico, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setSize(740, 180);
		scroll.doLayout();
		JViewport viewport = scroll.getViewport();
		viewport.doLayout();
		viewport.setViewSize(new Dimension(740, 235));
		grafico.setSize(740, 235);

		Rectangle areaVisivel = grafico.getVisibleRect();
		assertTrue(areaVisivel.height < grafico.getHeight());
		int mouseX = areaVisivel.x + areaVisivel.width / 2;
		int mouseY = areaVisivel.y + areaVisivel.height - 5;
		MouseEvent event = new MouseEvent(grafico, MouseEvent.MOUSE_MOVED,
				System.currentTimeMillis(), 0, mouseX, mouseY, 0, false);
		for (java.awt.event.MouseMotionListener listener : grafico.getMouseMotionListeners()) {
			listener.mouseMoved(event);
		}

		BufferedImage imagem = new BufferedImage(740, 235, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = imagem.createGraphics();
		try {
			grafico.paint(graphics);
		} finally {
			graphics.dispose();
		}
		int largura = Math.min(larguraDetalhe, areaVisivel.width - 12);
		int x = Math.min(mouseX + 12, areaVisivel.x + areaVisivel.width - largura - 6);
		x = Math.max(areaVisivel.x + 6, x);
		int y = areaVisivel.y + areaVisivel.height - alturaDetalhe - 6;
		int corBorda = new Color(190, 204, 222).getRGB() & 0x00ffffff;
		assertEquals(corBorda, imagem.getRGB(x + largura / 2, y + alturaDetalhe) & 0x00ffffff);
	}

	private long contarPixels(BufferedImage imagem, Color cor) {
		long encontrados = 0;
		for (int y = 0; y < imagem.getHeight(); y++) {
			for (int x = 0; x < imagem.getWidth(); x++) {
				if ((imagem.getRGB(x, y) & 0x00ffffff) == (cor.getRGB() & 0x00ffffff)) {
					encontrados++;
				}
			}
		}
		return encontrados;
	}

	private LeituraResponseDTO leitura(LocalDateTime inicio, String quantidade) {
		return new LeituraResponseDTO(
				null, 1L, inicio, inicio.plusSeconds(60), new BigDecimal(quantidade),
				"un", "evento-" + quantidade, inicio.plusSeconds(60));
	}
}
