package plotter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import plotter.DataPlotter.DataSymbol;

/**
 * JavaFx Application to show examples of DataPlotter and InteractiveLineChart
 */
public class Examples extends Application {

	int idx;
	int value;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		DataPlotter plotter = new DataPlotter();
		Button previous = new Button("previous demo"), next = new Button("next demo"), trigger = new Button("trigger");
		Label label = new Label();
		HBox head = new HBox(15, previous, next, trigger, label);
		head.setAlignment(Pos.CENTER_LEFT);
		head.setPadding(new Insets(10));
		head.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, null, null)));
		Node node = plotter.getPane();
		VBox vbox = new VBox(head, new Separator(), node);
		VBox.setVgrow(node, Priority.ALWAYS);
		Scene scene = new Scene(vbox);
		primaryStage.setScene(scene);

		double[] tt = DataPlotter.spacing(0, 2*Math.PI, 200).toArray();
		List <Runnable> examples = Arrays.asList(
				() -> {
					plotter.builder()
						.setDiscreteFunction(0, 2*Math.PI, 100, x -> Math.sin(x))
						.setSymbol(DataSymbol.DIAMOND)
						.setColor(Color.ORANGE)
						.setSymbolFilled(true)
						.setName("sine")
						.plot();
					plotter.builder()
						.setX(0, 8)
						.setY(-1, 1)
						.setLineWidth(1.5)
						.setColor(Color.DEEPSKYBLUE)
						.setSymbol(DataSymbol.SQUARE)
						.setSymbolFilled(true)
						.setName("line")
						.plot();
					plotter.builder()
						.setDiscreteFunction(0, 2*Math.PI, 100, x -> Math.cos(x))
						.setColor(Color.DARKOLIVEGREEN)
						.setSymbolFilled(false)
						.setName("cosine")
						.plot();
					label.setText("curves");
					trigger.setDisable(true);
				},
				
				() -> {
					plotter.builder()
						.setY(new double[] {-1.5, 3, 0.75, 5.0, 6, 2.334})
						.setColor(Color.CORNFLOWERBLUE)
						.setSymbol(DataSymbol.CROSS)
						.plot();
					plotter.builder()
						.setY(new double[] {4, 2, 7, 5.0, 3, 4})
						.setColor(Color.BLACK)
						.setLineWidth(1)
						.setSymbol(DataSymbol.PLUS)
						.plot();
					label.setText("lines");
					trigger.setDisable(true);
				},
				
				() -> {
					double[] data = {-1857, 500_000, 240_000, 20_130_568};
					plotter.addPlot(data);
					trigger.setOnAction(event -> {
						value += 1_000_000;
						plotter.builder()
							.setY(Arrays.stream(data).map(d -> d + value).toArray())
							.plot();
					});
					label.setText("trigger: add line");
					trigger.setDisable(false);
				},
				
				() -> {
					double[] x = DoubleStream.of(tt).map(t -> Math.cos(t)).toArray();
					double[] y = DoubleStream.of(tt).map(t -> Math.sin(t)).toArray();
					plotter.builder()
						.setX(x)
						.setY(y)
						.setColor(Color.BLUEVIOLET)
						.setSymbol(DataSymbol.TRIANGLE)
						.setLineWidth(1.5)
						.plot();
					label.setText("circle");
					trigger.setDisable(true);
				},
				
				() -> {
					double r = 500_000, dx = 133_500, dy = -80_437;
					double[] x = DoubleStream.of(tt).map(t -> Math.cos(t) * r + dx).toArray();
					double[] y = DoubleStream.of(tt).map(t -> Math.sin(t) * r + dy).toArray();
					for (int i = 0; i < tt.length; i += 5) {
						plotter.builder()
							.setX(dx, x[i])
							.setY(dy, y[i])
							.setColor(Color.BLACK)
							.setLineWidth(1)
							.setSymbol(DataSymbol.NONE)
							.setLegendEntry(false)
							.plot();
					}
					plotter.builder()
						.setX(x)
						.setY(y)
						.setColor(Color.RED)
						.setLineWidth(3.0)
						.plot();
					label.setText("circle, spokes without legend entry");
					trigger.setDisable(true);
				},
				
				() -> {
					Series <Number, Number> sine = plotter.builder()
							.setX(0)
							.setY(0)
							.setName("sine wave")
							.setColor(Color.DARKCYAN)
							.plot();
					trigger.setOnAction(event -> {
						for (int i = 0; i < 4; i++) {
							value++;
							double t = 2 * Math.PI / 100 * value;
							sine.getData().add(new Data <Number, Number> (t, Math.sin(t)));
						}
					});
					label.setText("trigger: extend sine curve");
					trigger.setDisable(false);
				},
				
				() -> {
					List <Double> list = DataPlotter.spacingList(0, 2*Math.PI, 300);
					plotter.builder()
						.setX(list, t -> 16 * Math.pow(Math.sin(t), 3))
						.setY(list, t -> 13 * Math.cos(t) - 5 * Math.cos(2*t) - 2 * Math.cos(3*t) - Math.cos(4*t))
						.setColor(Color.GREEN)
						.setSymbol(DataSymbol.NONE)
						.setLineWidth(1.5)
						.setName("Heart")
						.plot();
					label.setText("nice heart curve");
					trigger.setDisable(true);
				}
				
				);
		
		Runnable runner = () -> {
			value = 0;
			trigger.setOnAction(event -> {});
			plotter.clearPlots();
			examples.get(idx).run();
		};
		previous.setOnAction(event -> {
			idx = (idx + examples.size() - 1) % examples.size();
			runner.run();
		});
		next.setOnAction(event -> {
			idx = (idx + 1) % examples.size();
			runner.run();
		});
		examples.get(0).run();
		
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
