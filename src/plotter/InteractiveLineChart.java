package plotter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class InteractiveLineChart extends LineChart <Number, Number> {

	private double xm, xLo, xHi, ym, yLo, yHi;
	private boolean isPanning;
	private static final double ZOOM_FACTOR = 1.1;
	private static final double LEGEND_LINE_LENGTH = 10.0;
	private Map <Node, Boolean> legendEntryMap = new HashMap <> ();
	
	public InteractiveLineChart(ValueAxis <Number> xAxis, ValueAxis <Number> yAxis) {
		super(xAxis, yAxis);
		setAlternativeRowFillVisible(true);
		setAlternativeColumnFillVisible(false);
		setAxisSortingPolicy(SortingPolicy.NONE);
		legendSideProperty().addListener(inv -> updateLegend());
		
		Stream.of(getPlotArea(), xAxis, yAxis).forEach(node -> node.setOnMousePressed(mouseEvent -> {
			if (mouseEvent.getButton() == MouseButton.PRIMARY) {
				startPan(mouseEvent, xAxis, yAxis);
			}
		}));
		
		getPlotArea().setOnMouseDragged(mouseEvent -> {
			if (isPanning) {
				pan(xAxis, mouseEvent.getX(), xm, xLo, xHi);
				pan(yAxis, mouseEvent.getY(), ym, yLo, yHi);
			}
		});
		xAxis.setOnMouseDragged(mouseEvent -> {
			if (isPanning) {
				pan(xAxis, mouseEvent.getX(), xm, xLo, xHi);
			}
		});
		yAxis.setOnMouseDragged(mouseEvent -> {
			if (isPanning) {
				pan(yAxis, mouseEvent.getY(), ym, yLo, yHi);
			}
		});
		
		getPlotArea().setOnScroll(scrollEvent -> {
			double f = scrollEvent.getDeltaY() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
			xAxis.setAutoRanging(false);
			yAxis.setAutoRanging(false);
			zoom(xAxis, scrollEvent.getX(), f);
			zoom(yAxis, scrollEvent.getY(), f);
		});
		xAxis.setOnScroll(scrollEvent -> {
			if (scrollEvent.getX() > 0 && scrollEvent.getX() < xAxis.getWidth()) {
				double f = scrollEvent.getDeltaY() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
				xAxis.setAutoRanging(false);
				zoom(xAxis, scrollEvent.getX(), f);
			}
		});
		yAxis.setOnScroll(scrollEvent -> {
			if (scrollEvent.getY() > 0 && scrollEvent.getY() < yAxis.getHeight()) {
				double f = scrollEvent.getDeltaY() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
				yAxis.setAutoRanging(false);
				zoom(yAxis, scrollEvent.getY(), f);
			}
		});
		
		setOnMouseReleased(mouseEvent -> {
			isPanning = false;
		});
		
		setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getClickCount() == 2) {
				xAxis.setAutoRanging(true);
				yAxis.setAutoRanging(true);
			}
		});

		getPlotArea().widthProperty().addListener((obs, oldVal, newVal) -> {
			if (xAxis.isAutoRanging() == false)	zoom(xAxis, newVal.doubleValue() / 2.0, newVal.doubleValue() / oldVal.doubleValue());
		});
		getPlotArea().heightProperty().addListener((obs, oldVal, newVal) -> {
			if (yAxis.isAutoRanging() == false)	zoom(yAxis, newVal.doubleValue() / 2.0, newVal.doubleValue() / oldVal.doubleValue());
		});
	}
	
	public void setAxesEqual() {
		getXAxis().setAutoRanging(false);
		getYAxis().setAutoRanging(false);
		double ratio = Math.abs(getXAxis().getScale()) / Math.abs(getYAxis().getScale());		//scale ratio x : y
		if (ratio < 1) zoom(getYAxis(), getPlotArea().getHeight() / 2.0, 1 / ratio);
		if (ratio > 1) zoom(getXAxis(), getPlotArea().getWidth() / 2.0, ratio);
	}
	
	public void putLegendEntry(Node seriesNode, Boolean hasLegendEntry) {
		legendEntryMap.put(seriesNode, hasLegendEntry);
	}
	
	private Region getPlotArea() {
		return (Region) lookup(".chart-plot-background");
	}
	
	private void zoom(ValueAxis <Number> axis, double mousePos, double f) {
		axis.setAutoRanging(false);
		double mid = axis.getValueForDisplay(mousePos).doubleValue();
		double lo = axis.getLowerBound(), hi = axis.getUpperBound();
		axis.setLowerBound(mid - (mid - lo) * f);
		axis.setUpperBound(mid + (hi - mid) * f);
	}
	
	private void pan(ValueAxis <Number> axis, double mousePos, double xm, double lo, double hi) {
		axis.setAutoRanging(false);
		double delta = (xm - mousePos) / axis.getScale();
		axis.setLowerBound(lo + delta);
		axis.setUpperBound(hi + delta);
	}
	
	private void startPan(MouseEvent mouseEvent, ValueAxis <Number> xAxis, ValueAxis <Number> yAxis) {
		isPanning = true;
		xm = mouseEvent.getX();
		xLo = xAxis.getLowerBound();
		xHi = xAxis.getUpperBound();
		ym = mouseEvent.getY();
		yLo = yAxis.getLowerBound();
		yHi = yAxis.getUpperBound();
	}
	
	@Override
	public ValueAxis <Number> getXAxis() {
		return (ValueAxis <Number>) (super.getXAxis());
	}
	
	@Override
	public ValueAxis <Number> getYAxis() {
		return (ValueAxis <Number>) (super.getYAxis());
	}
	
	@Override
	protected void updateLegend() {
//		System.out.println("updateLegend");
		Pane legend = getLegendSide().isHorizontal() ? new HBox(15) : new VBox(5);
		legend.getStyleClass().add("chart-legend");
		for (Series <Number, Number> series : getData()) {
			Shape seriesShape = (Shape) series.getNode();
			Boolean hasLegendEntry = legendEntryMap.getOrDefault(seriesShape, true);
			if (hasLegendEntry == null || hasLegendEntry == true) {
				Label item = new Label();
				item.textProperty().bind(series.nameProperty());
				Line line = new Line(0, 0, LEGEND_LINE_LENGTH, 0);
				line.strokeProperty().bind(seriesShape.strokeProperty());
				line.strokeWidthProperty().bind(seriesShape.strokeWidthProperty());
				item.setGraphic(line);
				item.getStyleClass().add("chart-legend-item");
				legend.getChildren().add(item);
			}
		}
		setLegend(legend.getChildren().size() > 0 ? legend : null);
	}
}
