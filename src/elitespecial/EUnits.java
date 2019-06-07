package elitespecial;

import static javax.measure.unit.SI.METRE;

import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

public class EUnits {
	public static Unit<Length> LIGHT_SECOND = METRE.times(299792458);
	public static UnitConverter m2KM = SI.METER.getConverterTo(SI.KILOMETER);
	public static UnitConverter m2LS = SI.METER.getConverterTo(LIGHT_SECOND);
	public static UnitConverter K2C = SI.KELVIN.getConverterTo(SI.CELSIUS);
	public static UnitConverter kPaToAtmo = SI.PASCAL.getConverterTo(NonSI.ATMOSPHERE);
	public static double secToDays = 60 * 60 * 24;

}
