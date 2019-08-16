package plotter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;

public class DataPlotter {

//	private final ValueAxis <Number> axisX = new NumberAxis();
	private final ValueAxis <Number> axisX = new CustomNumberAxis();
//	private final ValueAxis <Number> axisY = new NumberAxis();
	private final ValueAxis <Number> axisY = new CustomNumberAxis();
	private final InteractiveLineChart chart = new InteractiveLineChart(axisX, axisY);
//	private final LineChart <Number, Number> chart = new InteractiveLineChart(axisX, axisY);
	private final CheckBox cbSymbols = new CheckBox("symbols");
	private final BoundsEditor boundsEditor;
	private final VBox vbox;
	private Stage stage;
	private int plotCount;
	
	public DataPlotter() {
		CheckBox cbGrid = new CheckBox("grid");
		CheckBox cbAxesAuto = new CheckBox("auto");
		Stream.of(cbSymbols, cbGrid, cbAxesAuto).forEach(cb -> cb.setSelected(true));
		chart.horizontalGridLinesVisibleProperty().bindBidirectional(cbGrid.selectedProperty());
		chart.verticalGridLinesVisibleProperty().bindBidirectional(cbGrid.selectedProperty());
		chart.setAnimated(false);
		axisX.autoRangingProperty().bindBidirectional(cbAxesAuto.selectedProperty());
		axisY.autoRangingProperty().bindBidirectional(cbAxesAuto.selectedProperty());
		
		Button btnAxesEqual = new Button("equ");
		btnAxesEqual.setTooltip(new Tooltip("set equal scale to both axes"));
		btnAxesEqual.setOnAction(event -> chart.setAxesEqual());
		
		Button btnAxesBounds = new Button("bds");
		btnAxesBounds.setTooltip(new Tooltip("manually set axes limits"));
		boundsEditor = new BoundsEditor();
		btnAxesBounds.setOnAction(event -> {
			boundsEditor.editBounds();
		});
		
		String csvInfo = "save data to CSV file";
		Button btnCsvSave = new Button("csv");
		btnCsvSave.setTooltip(new Tooltip(csvInfo));
		btnCsvSave.setOnAction(event -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new ExtensionFilter("csv file", "*.csv"));
			fc.setTitle(csvInfo);
			File file = fc.showSaveDialog(chart.getScene().getWindow());
			if (file != null) saveTable(file);
		});
		
		String snapshotInfo = "save screenshot to bitmap file";
		Button snapshot = new Button("pic");
		snapshot.setTooltip(new Tooltip(snapshotInfo));
		snapshot.setOnAction(event -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new ExtensionFilter("png image", "*.png"));
			fc.setTitle(snapshotInfo);
			File file = fc.showSaveDialog(chart.getScene().getWindow()); 
			if (file != null) saveBitmap(file);
		});
		
		Callback <ListView <Side>, ListCell <Side>> callback = lv -> {
			return new ListCell <Side> () {
				
				@Override
				public void updateItem(Side content, boolean empty) {
					super.updateItem(content, empty);
					if (lv == null && content != null) setText(content.toString().substring(0, 1));
				}
			};
		};
		ComboBox <Side> legendSide = new ComboBox <> ();
		legendSide.setItems(FXCollections.observableArrayList(Side.values()));
		legendSide.valueProperty().bindBidirectional(chart.legendSideProperty());
		legendSide.setPrefWidth(50);
		legendSide.setButtonCell(callback.call(null));
		
		CheckBox legendVisible = new CheckBox("legend");
		legendVisible.selectedProperty().bindBidirectional(chart.legendVisibleProperty());
		
		HBox hbox = new HBox(10, 
				cbSymbols, cbGrid, cbAxesAuto, legendVisible, legendSide, 
				new Separator(Orientation.VERTICAL), 
				btnAxesEqual, btnAxesBounds, btnCsvSave, snapshot);
		hbox.setAlignment(Pos.CENTER_LEFT);
		hbox.setPadding(new Insets(5, 10, 5, 10));
		hbox.setMinWidth(500);
		vbox = new VBox(hbox, chart);
		vbox.setMinHeight(400);
		VBox.setVgrow(chart, Priority.ALWAYS);
		
		vbox.getStylesheets().add(getClass().getResource("../res/DataPlotter.css").toExternalForm());
	}
	
	public boolean saveTable(File file) {
		boolean ok = true;
		try (BufferedWriter bw = Files.newBufferedWriter(file.toPath())) {
			bw.write("name;idx;x;y");
			bw.newLine();
			for (int i = 0; i < chart.getData().size(); i++) {
				Series <Number, Number> series = chart.getData().get(i);
				for (int k = 0; k < series.getData().size(); k++) {
					Data <Number, Number> data = series.getData().get(k);
					bw.write(String.format("%s;%d;%f;%f", series.getName(), k, data.getXValue(), data.getYValue()));
					bw.newLine();
				}
			}
			
		} catch (IOException e) {
			fileAlert(file, e);
			ok = false;
		}
		return ok;
	}
	
	public boolean saveBitmap(File file) {
		boolean ok = true;
		Image im = chart.snapshot(null, null);
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(im, null), "png", file);
			
		} catch (IOException e) {
			fileAlert(file, e);
			ok = false;
		}
		return ok;
	}
	
	public void setAxesEqual() {
		chart.setAxesEqual();
	}
	
	public Pane getPane() {
		return vbox;
	}
	
	public LineChart <Number, Number> getChart() {
		return chart;
	}
	
	public void showExternal() {
		stage = new Stage();
		Scene scene = new Scene(vbox, 800, 600);
		stage.setScene(scene);
		stage.show();
	}
	
	public void closeExternal() {
		if (stage != null) {
			stage.close();
			stage = null;
		}
	}
	
	public Series <Number, Number> addPlot(double[] x, double[] y) {
		return builder().setX(x).setY(y).plot();
	}
	
	public Series <Number, Number> addPlot(double[] y) {
		return builder().setY(y).plot();
	}
	
	public void clearPlots() {
		chart.getData().clear();
		plotCount = 0;
	}
	
	public boolean remove(Series <Number, Number> series) {
		return chart.getData().remove(series);
	}
	
	public boolean remove(String seriesName) {
		return chart.getData().removeIf(series -> series.getName().equals(seriesName));
	}
	
	public Builder builder() {
		return new Builder();
	}
	
	public static DoubleStream spacing(double start, double end, int count) {
		return IntStream.range(0, count).mapToDouble(i -> (end - start) / (count - 1) * i + start);
	}
	
	public static List <Double> spacingList(double start, double end, int count) {
		return spacing(start, end, count).boxed().collect(Collectors.toList());
	}
	
	private void fileAlert(File file, Exception e) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setHeaderText(null);
		alert.setContentText("error writing file\n" + file + "\n" + e.getLocalizedMessage());
		alert.showAndWait();
	}
	
	
	public class Builder {
		
		private Builder() {}
		
		/**
		 * set Values for x-axis
		 * @param series of individual double values or array of double
		 * @return Builder object
		 */
		public Builder setX(double... x) 		{return setValues(x, 0);}
		
		/**
		 * set Values for y-axis
		 * @param series of individual double values or array of double
		 * @return Builder object
		 */
		public Builder setY(double... y) 		{return setValues(y, 1);}
		
		/**
		 * set Values for x-axis
		 * @param collection of Double values, must not be null, must not contain null values
		 * @return Builder object
		 */
		public Builder setX(Collection <Double> x)		{return setValues(x, 0);}

		/**
		 * set Values for y-axis
		 * @param collection of Double values, must not be null, must not contain null values
		 * @return Builder object
		 */
		public Builder setY(Collection <Double> y)		{return setValues(y, 1);}

		/**
		 * provide a collection of arbitrary objects and an extractor function to get values for x-axis
		 * @param <E>
		 * @param items
		 * @param mapper
		 * @return Builder object
		 */
		public <E> Builder setX(Collection <E> items, ToDoubleFunction <E> mapper)	{return setValues(items, mapper, 0);}
		
		/**
		 * provide a collection of arbitrary objects and an extractor function to get values for y-axis
		 * @param <E>
		 * @param items
		 * @param mapper
		 * @return Builder object
		 */
		public <E> Builder setY(Collection <E> items, ToDoubleFunction <E> mapper)	{return setValues(items, mapper, 1);}
		
		/**
		 * provide an interval and a count to generate values for x-axis,
		 * provide a function to generate values for y-axis from each x value
		 * @param x0 start of interval for x-axis
		 * @param x1 end of interval for x-axis
		 * @param countX number of values on x
		 * @param functionXtoY generator to calculate y for x
		 * @return
		 */
		public Builder setDiscreteFunction(double x0, double x1, int countX, DoubleUnaryOperator functionXtoY) {
			double[] x = spacing(x0, x1, countX).toArray();
			return setX(x).setY(Arrays.stream(x).map(functionXtoY).toArray());
		}
		
		/**
		 * data points currently set
		 * @return data for x-axis
		 */
		public double[] getX()	{return dataX;}
		
		/**
		 * data points currently set
		 * @return data for y-axis
		 */
		public double[] getY()	{return dataY;}
		
		
		private double[] dataX, dataY;
		private XYChart.Series <Number, Number> series = new Series <> ();
		private String name;
		private Color color;
		private Double lineWidth;
		private DataSymbol symbol;
		private boolean isFilled = false;
		private Boolean hasLegendEntry = true;
		
		/**
		 * add data to the chart
		 * @return generated Series
		 */
		public Series <Number, Number> plot() {
			if (countX() != countY()) throw new RuntimeException("unequal number of data elements, x=" + countX() + ", y=" + countY());
			if (isFilled && color == null) throw new RuntimeException("setting fill requires setting color");
			
			StringBuilder cssSymbol = new StringBuilder(""), cssLine = new StringBuilder("");
			if (symbol != null) {
				cssSymbol.append(symbol.path);
			}
			if (color != null) {
				cssLine.append("-fx-stroke: " + hexColor(color) + "; ");
				cssSymbol.append("-fx-background-color: " + hexColor(color));
				if (symbol == null && isFilled == false) cssSymbol.append(", white");
				if (symbol != null && symbol.isFillable && isFilled == false) cssSymbol.append(", white");
				cssSymbol.append(" ;");
			}
			if (lineWidth != null) {
				cssLine.append("-fx-stroke-width: " + lineWidth + "; ");
			}
			if (name == null) {
				name = "data " + plotCount;
			}
			
			ObservableList <Data <Number, Number>> dataList = FXCollections.observableArrayList();
			for (int i = 0; i < countX(); i++) dataList.add(new Data <Number, Number> (dataX[i], dataY[i]));
			series.setData(dataList);
			chart.getData().add(series);
			series.setName(name);
			series.getNode().setStyle(cssLine.toString());

			//set properties AFTER series has been added to chart
			series.getData().forEach(d -> symbolSettings(d, cssSymbol.toString()));
			series.getData().addListener((ListChangeListener <Data <Number, Number>>) change -> {
				while (change.next()) {
					for (Data <Number, Number> d : change.getAddedSubList()) symbolSettings(d, cssSymbol.toString());
				}
			});
			chart.putLegendEntry(series.getNode(), hasLegendEntry);
			plotCount++;
			return series;
		}
		
		public Builder setName(String name) {
			this.name = name;
			return this;
		}
		
		public Builder setColor(Color color) {
			this.color = color;
			return this;
		}
		
		public Builder setLineWidth(double width) {
			this.lineWidth = width;
			return this;
		}
		
		public Builder setSymbol(DataSymbol symbol) {
			this.symbol = symbol;
			return this;
		}
		
		public Builder setSymbolFilled(boolean isFilled) {
			this.isFilled = isFilled;
			return this;
		}
		
		public Builder setLegendEntry(boolean hasLegendEntry) {
			this.hasLegendEntry = hasLegendEntry;
			return this;
		}
		
		/*
		 * ------------------------- private members
		 */
		
		private void symbolSettings(Data <Number, Number> d, String cssSymbol) {
			Tooltip tt = new Tooltip(String.format("x=%1.4f\ny=%1.4f", d.getXValue(), d.getYValue()));
//			tt.setShowDelay(Duration.millis(250)); // Java 11
			Node node = d.getNode();
			Tooltip.install(node, tt);
			node.setStyle(cssSymbol);
			node.visibleProperty().bind(cbSymbols.selectedProperty());
		}
		
		private <E> Builder setValues(Collection <E> elements, ToDoubleFunction <E> mapper, int axisIdx) {
			return setValues(elements.stream().mapToDouble(mapper).toArray(), axisIdx);
		}
		
		private Builder setValues(Collection <Double> val, int axisIdx) {
			return setValues(val.stream().mapToDouble(d -> d).toArray(), axisIdx);
		}
		
		private Builder setValues(double[] values, int axisIdx) {
			if (axisIdx == 0) {
				dataX = values;
				
			} else if (axisIdx == 1) {
				dataY = values;
				if (dataX == null) dataX = spacing(0, countY() - 1, countY()).toArray();
				
			} else {
				throw new RuntimeException("internal error");
			}
			return this;
		}
		
		private int countX() {
			return countArray(dataX);
		}
		
		private int countY() {
			return countArray(dataY);
		}
		
		private int countArray(double[] array) {
			return array == null ? -1 : array.length;
		}
		
		private String hexColor(Color color) {
			return "#" + hex(color.getRed()) + hex(color.getGreen()) + hex(color.getBlue()) + hex(color.getOpacity());
		}
		
		private String hex(double colorValue) {
			return String.format("%02x", (int) (colorValue * 255));
		}
	}

	
	public enum DataSymbol {
		DIAMOND	("-fx-background-radius: 0; -fx-background-insets: 0, 2.5; "
				+ "-fx-padding: 6px 5px 6px 5px; -fx-shape: \"M 5,0 L 10,9 L 5,18 L 0,9 Z\"; ", true),
		CIRCLE ("-fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px; ", true),
		
		SQUARE ("-fx-background-radius: 0; -fx-padding: 4.5px; ", true),
		
		TRIANGLE ("-fx-background-radius: 0; -fx-background-insets: 0 0 1 0, 2.7 2.2 3 2.2; -fx-shape: \"M5,0 L10,8 L0,8 Z\"; ", true),
		
		CROSS ("-fx-background-radius: 0; -fx-background-insets: 0; "
				+ "-fx-shape: \"M2,0 L5,4 L8,0 L10,0 L10,2 L6,5 L10,8 L10,10 L8,10 L5,6 L2, 10 L0,10 L0,8 L4,5 L0,2 L0,0 Z\"; ", false),
		PLUS ("-fx-background-radius: 0; -fx-background-insets: 0; "
				+ "-fx-shape: \"M0,0 h 4 v -4 h 2 v 4 h 4 v 2 h -4 v 4 h -2 v -4 h -4 v -2 Z\"; ", false),
		NONE ("-fx-padding: 0px; ", false),
		;

		private String path;
		private boolean isFillable;
		
		private DataSymbol(String path, boolean isFillable) {
			this.path = path;
			this.isFillable = isFillable;
		}
	}
	
	private class BoundsEditor extends Stage {

		Button btnOK = new Button("OK");
		Button btnCancel = new Button("Cancel");
		DoubleProperty[] values = new DoubleProperty[4];
		
		BoundsEditor() {
	        setTitle("Axis Bounds");
	        initModality(Modality.APPLICATION_MODAL);
	        initStyle(StageStyle.DECORATED);
	        setResizable(false);
	        
			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(10, 10, 10, 20));
			
			TextField [] fields = new TextField[4];
			for (int i = 0; i < 4; i++) {
				fields[i] = new TextField();
				fields[i].setPrefWidth(140);
				TextFormatter <Double> formatter = new TextFormatter <> (new DoubleStringConverter());
				fields[i].setTextFormatter(formatter);
				values[i] = DoubleProperty.doubleProperty(formatter.valueProperty());
			}
			
			grid.add(new Label("X lower bound :"), 0, 0);
			grid.add(fields[0], 1, 0);
			grid.add(new Label("X upper bound :"), 0, 1);
			grid.add(fields[1], 1, 1);
			grid.add(new Label("Y lower bound :"), 0, 3);
			grid.add(fields[2], 1, 3);
			grid.add(new Label("Y upper bound :"), 0, 4);
			grid.add(fields[3], 1, 4);
			
			btnOK.setPrefWidth(80);
			btnCancel.setPrefWidth(80);
			grid.add(btnOK, 0, 6);
			grid.add(btnCancel, 1, 6);

			btnOK.defaultButtonProperty().bind(btnOK.focusedProperty());
			btnOK.setOnAction(event -> {
				axisX.setAutoRanging(false);
				axisX.setLowerBound(values[0].get());
				axisX.setUpperBound(values[1].get());
				axisY.setAutoRanging(false);
				axisY.setLowerBound(values[2].get());
				axisY.setUpperBound(values[3].get());
				close();
			});
			btnOK.disableProperty().bind(Bindings.lessThanOrEqual(values[1], values[0]).or(Bindings.lessThanOrEqual(values[3], values[2])));
			
			btnCancel.defaultButtonProperty().bind(btnCancel.focusedProperty());
			btnCancel.setCancelButton(true);
			btnCancel.setOnAction(event -> close());
			
	        Scene scene = new Scene(grid);
	        setScene(scene);
		}
		
		public void editBounds() {
			values[0].setValue(axisX.getLowerBound());
			values[1].setValue(axisX.getUpperBound());
			values[2].setValue(axisY.getLowerBound());
			values[3].setValue(axisY.getUpperBound());
			btnCancel.requestFocus();
			showAndWait();
		}
	}

}
