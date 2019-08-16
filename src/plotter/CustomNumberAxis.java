package plotter;

import java.util.*;

import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.scene.chart.ValueAxis;
import javafx.util.StringConverter;

public class CustomNumberAxis extends ValueAxis <Number> {

	private static final List <Integer> DELTA_LIST = Arrays.asList(1, 2, 5);	//possible base delta values between major ticks
	private static final double MARGIN = 0.01;		//distance between plot area and edge of graph
	private static final double TICK_GAP = 10.0;	//minimal gap between tick labels
	private TreeMap <Number, String> majorTicksMap = new TreeMap <> ();
	private double majorTickDeltaPixel = 50.0;
	private double majorTickDelta;
	private TickMarkLabelSizeCalculator labelSizeCalculator = new TickMarkLabelSizeCalculator();
	
	{
		scaleProperty().addListener(inv -> Platform.runLater(() -> requestAxisLayout()));
	}
	
	@Override
	protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
//		System.out.println("autoRange");
		double margin = (maxValue - minValue) * MARGIN;
		double lo = minValue - margin, hi = maxValue + margin;
		double scale = calculateNewScale(length, lo, hi);		//setzt auch den offset, siehe ValueAxis
		return new double[] {lo, hi, scale, length};
	}
	
	@Override
	protected void setRange(Object rangeObj, boolean animate) {
		double[] rng = (double[]) rangeObj;
//		System.out.println(getSide() + ", setRange " + Arrays.toString(rng));
		setLowerBound(rng[0]);
		currentLowerBound.set(rng[0]); 				//siehe ValueAxis
		setUpperBound(rng[1]);
		setScale(rng[2]);
	}

	@Override
	protected Object getRange() {
		double[] rng = new double[] {getLowerBound(), getUpperBound(), getScale()};
//		System.out.println(getSide() + ", getRange: " + Arrays.toString(rng));
		return rng;
	}
	
	@Override
	protected List <Number> calculateTickValues(double length, Object rangeObj) {
		double[] rng = (double[]) rangeObj;
//		if (getSide() == javafx.geometry.Side.LEFT) System.out.println(getSide() + ", calculateTickValues, " + Arrays.toString(rng));
		double lo = rng[0], hi = rng[1], scale = Math.abs(rng[2]), deltaFinal = 0.0;
		boolean retry = true;
		Set <Double> deltaPixelTried = new HashSet <> ();
		while (retry) {
			majorTicksMap.clear();
			deltaPixelTried.add(majorTickDeltaPixel);
			double tickDeltaStart = majorTickDeltaPixel / scale;			//initial delta in data units
			TickDelta tickDeltaGood = DELTA_LIST.stream()
					.map(deltaBase -> new TickDelta(tickDeltaStart, deltaBase))
					.min(Comparator.comparingDouble(td -> td.delta))
					.get();
			deltaFinal = tickDeltaGood.delta;								//chosen delta in data units

			double majorTick = Math.floor(lo / deltaFinal) * deltaFinal;	//lowest major tick, below visible axis limit
			while (majorTick < hi && majorTicksMap.size() < length / 2) {	//as long as below upper limit
				putTickMarkLabel(majorTick, tickDeltaGood.magnitude);			//list of major ticks to display
				majorTick += deltaFinal;
			}

			retry = false;
			labelSizeCalculator.calculate();
			double maxLabelLength = labelSizeCalculator.getAlongAxis();
			double deltaFinalPixel = deltaFinal * scale; 
			if (deltaFinalPixel < maxLabelLength + TICK_GAP) {				//largest label does not fit, try again
				majorTickDeltaPixel += TICK_GAP;
				retry = true;
			}
			if (deltaFinalPixel > maxLabelLength + 1.2 * TICK_GAP) {		//maybe we can shrink distance of ticks
				majorTickDeltaPixel -= TICK_GAP;
				retry = !deltaPixelTried.contains(majorTickDeltaPixel);		//try again if that values has not been tried before
			}
//			System.out.println(getSide() + ", " + deltaFinal + ", " + deltaFinalPixel + ", " + majorTicksMap);
		}
		majorTickDelta = deltaFinal;
		return new ArrayList <> (majorTicksMap.keySet());
	}
	
	@Override
	protected List <Number> calculateMinorTickMarks() {
//		System.out.println("calculateMinorTickMarks");
		List <Number> minorTicks = new ArrayList <> ();
		double deltaTick = majorTickDelta / getMinorTickCount();
		
		majorTicksMap.navigableKeySet().forEach(major -> {
			for (int i = 1; i < getMinorTickCount(); i++) minorTicks.add(major.doubleValue() + deltaTick * i);
		});
		return minorTicks;
	}

	@Override
	protected String getTickMarkLabel(Number value) {
		StringConverter <Number> formatter = getTickLabelFormatter();
		if (formatter != null) return formatter.toString(value);
		else return majorTicksMap.get(value);
	}
	
	private void putTickMarkLabel(double value, int scale) {
		String str;
		if (scale > 4) str = String.format("%1.0f E%d", value / Math.pow(10, scale), scale);
		else if (scale >= 0) str = String.format("%,1.0f", value);
		else str = String.format("%,1." + (-scale) + "f", value);
		majorTicksMap.put(value, str);
	}
	
	private class TickMarkLabelSizeCalculator {
		
		double labelWidth, labelHeight;
		
		void calculate() {
			labelWidth = 0.0;
			labelHeight = 0.0;
			for (String str : majorTicksMap.values()) {
				Dimension2D dim = measureTickMarkLabelSize(str, getTickLabelRotation());
				labelWidth = Math.max(labelWidth, dim.getWidth());
				labelHeight = Math.max(labelHeight, dim.getHeight());
			}
		}
		
		double getAlongAxis() {
			return getSide().isHorizontal() ? labelWidth : labelHeight;
		}
	}
	
	/**
	 * find the smallest magnitude to which the value of baseDelta will just be larger than tickDelta
	 * example: tickDelta = 37
	 * baseDelta = 1, magnitude will be 2, because 1*10^2 = 100 > 37
	 * baseDelta = 2, magnitude will be 2, because 2*10^2 = 200 > 37
	 * baseDelta = 5, magnitude will be 1, because 5*10^1 = 50 > 37 
	 */
	private class TickDelta {
		
		final int magnitude;
		final double delta;
		
		TickDelta(double tickDelta, int baseDelta) {
			double d = tickDelta / baseDelta;
			int scale = 1;
			while (d > 1.0) {
				d /= 10.0;
				scale++;
			}
			while (d < 1.0) {
				d *= 10.0;
				scale--;
			}
			this.magnitude = scale;
			this.delta = baseDelta * Math.pow(10, scale);
//			System.out.println("input = " + tickDelta + ", baseDelta = " + baseDelta + ", delta = " + delta);
		}
	}
}