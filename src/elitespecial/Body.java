package elitespecial;

import java.util.LinkedList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class Body extends Event
{
	public double Radius;
	public String BodyName;
	public int BodyID;
	public double SemiMajorAxis;
	public double Periapsis;
	public String StarType;
	public double RotationPeriod;
	public String Atmosphere;
	public String AtmosphereType;
	public String PlanetClass;
	public String ScanType;
	public double SurfacePressure;
	public double Eccentricity;

	public double grav()
	{
		return SurfaceGravity / 9.80665;
	}

	public boolean isStar()
	{
		return StarType != null && !StarType.isEmpty();
	}
	
	public double rotationDays() { return RotationPeriod / EUnits.secToDays; }
	public double orbitalDays() { return OrbitalPeriod / EUnits.secToDays; }


	public boolean TidalLock;
	public double SurfaceTemperature;
	public double SurfaceGravity;
	public double MassEM;
	public boolean Landable;
	public String event;
	public double OrbitalPeriod;
	public String system;
	@XStreamOmitField
	public List<Ring> Rings = new LinkedList<>();
	public double DistanceFromArrivalLS;
	@XStreamOmitField
	public List<Parent> Parents = new LinkedList<>();
	public List<Body> BodyParents = new LinkedList<>();
	public List<Body> BodyChildren = new LinkedList<>();
	public String TerraformState;
	String ref;
	public double OrbitalInclination;

	String parentString()
	{
		String s = "";
		for (Parent p : Parents)
			s = s + p.id;
		return s;
	}

	String parentNString()
	{
		String s = "";
		for (Parent p : Parents)
			s = s + p.name;
		return s;
	}

	public String tLocked()
	{
		if (TidalLock)
			return "tidally locked";
		else
			return "NOT tidally locked";
	}
}