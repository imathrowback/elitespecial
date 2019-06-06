package elitespecial;

import static javax.measure.unit.SI.METRE;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.bushe.swing.event.EventBus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

class Ring
{
	public String Name;
	public String RingClass;
	double MassMT;
	double InnerRad;
	double OuterRad;
}

class Parent
{
	String name;
	long id;
}

class Event
{
	public Date timestamp;

}

class FSDJump extends Event
{
	String StarSystem;
}

class Body extends Event
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

public class EliteSpecial
{
	static Unit<Length> LIGHT_SECOND = METRE.times(299792458);
	public boolean shouldStop = false;

	public void stop()
	{
		shouldStop = true;
	}

	public void go(final String journalDirectory, final int days, final Consumer<EventData> eventConsumer)
			throws Exception
	{
		LocalDate startDate = new LocalDate().minusDays(days);
		//startDate = new LocalDate().minusYears(10);
		Date lastEntry = startDate.toDateMidnight().toDate();

		WatchService watcher = FileSystems.getDefault().newWatchService();
		Path journalPath = new File(journalDirectory).toPath();
		WatchKey key = journalPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
		System.out.println("Scan journals");
		eventConsumer.accept(new EventData(new Date(), "INFO", "", "Scanning journals...", false));
		Set<String> collectedEvents = collectEvents(journalDirectory, lastEntry);
		boolean collectBodies = true;
		System.out.println("Begin watch");
		while (!shouldStop)
		{
			List<WatchEvent<?>> events = key.pollEvents();
			if (events != null && events.size() > 0)
			{

				for (WatchEvent<?> we : events)
				{
					Kind<?> kind = we.kind();
					//					System.out.println("event kind:" + kind);
					if (kind == StandardWatchEventKinds.ENTRY_MODIFY)
					{
						WatchEvent<Path> ev = (WatchEvent<Path>) we;
						Path filename = journalPath.resolve(ev.context());
						if (filename.getFileName().toString().endsWith("log"))
						{
							//System.out.println(filename + ": modified");
							//System.out.println("Journal modified: " + filename);
							collectedEvents.addAll(collectEventsFromFile(filename.toFile(), lastEntry));
							collectBodies = true;
						}
					}
				}
				//			System.out.println(collectBodies);
			}
			if (collectBodies)
			{
				//	System.out.println("Review bodies");
				//Map<String, Body> bodies = collectBodies(collectedEvents);

				Set<Event> sortedEvents = new TreeSet<>(new Comparator<Event>() {
					@Override
					public int compare(final Event o1, final Event o2)
					{
						if (o1 == null || o2 == null || o1.timestamp == null || o2.timestamp == null)
						{
							System.err.println("Event or timestamp was null!:" + o1 + "," + o2);
						}
						return o1.timestamp.compareTo(o2.timestamp);
					}
				});

				parseEvents(collectedEvents, sortedEvents);

				check(sortedEvents, eventConsumer);
				collectedEvents.clear();
				lastEntry = new Date();
				collectBodies = false;
				EventBus.publish(new ScanParse("Done...watching for changes", 0, 0));
			}
			Thread.sleep(100);
		}

	}

	static class RecordEntry
	{
		public double min;
		public Body bmin;
		public double max;
		public Body bmax;

		public RecordEntry(final double x, final Body b)
		{
			min = max = x;
			bmin = bmax = b;
		}

		public void update(final double x, final Body b)
		{
			if (x < min)
			{
				min = x;
				bmin = b;
			}
			if (x > max)
			{
				max = x;
				bmax = b;
			}
		}

		@Override
		public String toString()
		{
			return "" + f.format(min) + "[" + bmin.BodyName + "], " + f.format(max) + "[" + bmax.BodyName + "]";
		}

	}

	static Map<String, RecordEntry> records = new TreeMap<>();
	static NumberFormat f = NumberFormat.getNumberInstance();

	public static String record(final String key, final double x, final Body b)
	{
		if (true)
			return f.format(x);
		if (records.containsKey(key))
		{
			records.get(key).update(x, b);
		} else
			records.put(key, new RecordEntry(x, b));

		return f.format(x) + "(" + records.get(key) + ")";
	}

	public static void check(final Set<Event> sortedEvents, final Consumer<EventData> eventConsumer)
	{
		Unit<Length> LIGHT_SECOND = METRE.times(299792458);
		UnitConverter m2KM = SI.METER.getConverterTo(SI.KILOMETER);
		UnitConverter m2LS = SI.METER.getConverterTo(LIGHT_SECOND);
		UnitConverter K2C = SI.KELVIN.getConverterTo(SI.CELSIUS);
		TreeSet<String> alreadyDone = new TreeSet<>();
		sortedEvents.forEach((evt) -> {
			if (evt instanceof FSDJump)
			{
				FSDJump jump = (FSDJump) evt;
				return;
				//System.out.println(evt.timestamp + ": JUMP====> " + jump.StarSystem);
			} else if (evt instanceof Body)
			{
				Body b = (Body) evt;
				if (alreadyDone.contains(b.BodyName))
					return;

				StringWriter sw = new StringWriter();
				PrintWriter writer = new PrintWriter(sw);

				boolean binaryPair = false;
				if (b.Parents.size() > 0)
					binaryPair = b.Parents.get(0).name.equals("Null");

				alreadyDone.add(b.BodyName);
				//UnitConverter LS2KM = NonSI.LIGHT_YEAR.se.METER.getConverterTo(SI.KILOMETER);

				double secToDays = 60 * 60 * 24;
				double rotationDays = b.RotationPeriod / secToDays;
				double orbitalDays = b.OrbitalPeriod / secToDays;

				// landables with atmosphere
				if (b.Landable && (!b.Atmosphere.equals("None") && !b.AtmosphereType.equals("None")))
				{
					writer.println("\t has atmosphere AND is landable");
				}

				if (b.TerraformState != null && !b.TerraformState.isEmpty() && b.Landable)
				{
					writer.println("\t is landable and " + b.TerraformState);
				}

				//high gravity landables
				if (b.grav() > 3 && b.Landable)
				{
					writer.println("\t is landable and has high gravity: " + record("grav", b.grav(), b));
				}

				// planets close to their parent
				if (!b.BodyParents.isEmpty() && !binaryPair)
				{
					double radius = b.Radius;
					double axis = b.SemiMajorAxis;
					double dist = m2KM.convert(axis - radius);
					record("dist", dist, b);
					double per = (radius / axis) * 100.0;
					if (m2LS.convert(axis) < 0.5)
						//if (per > 16)
						writer.println(
								"\t is 'pretty close' to it's parent: dist(" + dist/*record("dist", dist, b)*/
										+ " km), radius:"
										+ f.format(Math.ceil(m2KM.convert(b.Radius)))
										+ "km, axis:" + f.format(Math.ceil(m2KM.convert(b.SemiMajorAxis))) + "km ("
										+ f.format(per) + "%)");

				}
				if (b.Rings != null)
				{
					for (Ring r : b.Rings)
					{

						double ils = Math.round(m2LS.convert(r.InnerRad) * 100.0) / 100.0;
						double ols = Math.round(m2LS.convert(r.OuterRad) * 100.0) / 100.0;
						int v = 3;

						if (ils > v || ols > v)
							writer.println("\t rings are greater than 1 light second " + ils + "-" + ols);
					}
				}

				// Planets within rings
				if (!b.BodyName.contains(" Ring") && isEmpty(b.StarType))
				{
					String body = b.BodyName;
					try
					{
						int lastSpace = body.lastIndexOf(' ');
						if (lastSpace >= 0)
						{
							String parentStr = body.substring(0, lastSpace);
							// get it's parent

							// if the first parent is "Null" then this is part of a binary(or more) pair.
							// there is insufficient information in the journal to tell how far away a binary pair is from the parent
							double semiMajor = b.SemiMajorAxis;

							if (!binaryPair)
							{
								if (!b.BodyParents.isEmpty())
								{
									// Try to cheat and guess our semimajor based on the difference of our LS and our parents LS
									/*
									if (binaryPair)
									{
										double parentLS = b.BodyParents.get(0).DistanceFromArrivalLS;
										double bodyLS = b.DistanceFromArrivalLS;
										double diffLS = bodyLS - parentLS;
										semiMajor = LIGHT_SECOND.getConverterTo(SI.METER).convert(diffLS);

									}
									*/
									Body parent = b.BodyParents.get(0);
									if (parent.Rings != null && parent.Rings.size() > 0)
									{
										double d1 = b.SemiMajorAxis + b.Radius;
										double d2 = b.SemiMajorAxis - b.Radius;

										for (Ring r : parent.Rings)
										{
											double aa = Math.abs(r.OuterRad - d1);
											double bb = Math.abs(r.OuterRad - d2);
											double aaa = Math.abs(r.InnerRad - d1);
											double bbb = Math.abs(r.InnerRad - d2);
											double ccA = Math.min(aa, bb);
											double ccB = Math.min(aaa, bbb);
											double cc = Math.min(ccA, ccB);

											if (d1 > r.OuterRad && d2 < r.OuterRad
													|| (d1 < r.OuterRad && d1 > r.InnerRad && d2 < r.InnerRad))
											{
												writer.println(
														"\t PART WITHIN rings of '" + parentStr + "'. Body major axis  "
																+ f.format(m2KM.convert(semiMajor))
																+ "km, rings: "
																+ f.format(m2KM.convert(r.InnerRad)) + "km - "
																+ f.format(m2KM.convert(r.OuterRad)) + "km");
											}

											else if (semiMajor > r.InnerRad && semiMajor < r.OuterRad)
											{
												writer.println(
														"\t WITHIN rings of '" + parentStr + "'. Body major axis  "
																+ f.format(m2KM.convert(semiMajor))
																+ "km, rings: "
																+ f.format(m2KM.convert(r.InnerRad)) + "km - "
																+ f.format(m2KM.convert(r.OuterRad)) + "km");
											} else if (semiMajor < r.InnerRad)
											{
												writer.println("\t IN GAP before the inner ring edge of '" + parentStr
														+ "'. Body major axis "
														+ f.format(m2KM.convert(semiMajor)) + "km, rings "
														+ f.format(m2KM.convert(r.InnerRad)) + "km - "
														+ f.format(m2KM.convert(r.OuterRad)) + "km");
												break;
											} else if (m2KM.convert(cc) < 300)
											{

												writer.println("\t near ring edge of '" + parentStr
														+ "'. Body major axis "
														+ f.format(m2KM.convert(semiMajor)) + "km, rings "
														+ f.format(m2KM.convert(r.InnerRad)) + "km - "
														+ f.format(m2KM.convert(r.OuterRad)) + "km - "
														+ "DIST:" + f.format(m2KM.convert(cc))

												);

											}
										}

									}
								}
							}
						}
					} catch (Exception ex)
					{
						System.out.println(body);
						ex.printStackTrace();
					}
				}
				int planetParents = countMatches(b.parentNString(), "Planet");
				if (planetParents > 2)
					writer.println("\t is a moon of a moon (x" + (planetParents - 1) + ")");

				if (b.PlanetClass != null && b.PlanetClass.contains("gas giant") && b.parentString().contains("Planet"))
					writer.println("\t is gas giant moon of a planet class " + b.BodyParents.get(0).PlanetClass);

				UnitConverter kPaToAtmo = SI.PASCAL.getConverterTo(NonSI.ATMOSPHERE);
				double atmo = kPaToAtmo.convert(b.SurfacePressure);
				record("atmo", atmo, b);
				// high pressure
				if (atmo > 10_000_000)
					writer.println("\t has high surface pressure "
							+ record("atmo", atmo, b));
				// Backwards rotating planets
				if (false && rotationDays < 0)
					writer.println("\t rotates backwards " + rotationDays);
				record("surfacetemp", K2C.convert(b.SurfaceTemperature), b);
				if (b.Landable && K2C.convert(b.SurfaceTemperature) > 1200)
					writer.println("\t is HOT landable :" + record("surfacetemp", K2C.convert(b.SurfaceTemperature), b)
							+ " C");

				// Fast rotating planets
				record("rotspedd", Math.abs(rotationDays), b);
				if (!b.BodyName.contains(" Ring") && isEmpty(b.StarType)
						&& Math.abs(rotationDays) < 0.3 && !b.TidalLock)
					writer.println("\t has a fast rotating speed " + record("rotspedd", Math.abs(rotationDays), b)
							+ " and is "
							+ b.tLocked());
				if (b.Landable)
				{
					record("radius", m2KM.convert(b.Radius), b);
					if (m2KM.convert(b.Radius) < 300)
						writer.println("\t body is a landable and small: " + record("radius", m2KM.convert(b.Radius), b)
								+ " km radius");
				}

				String periodString = getPeriod(orbitalDays);
				if (b.Eccentricity > 0.5 && !b.isStar() && b.Landable && orbitalDays < 7)
				{
					writer.println("\t is landable and has high Eccentricity " + record("ecc", b.Eccentricity, b)
							+ " and low orbital period of " + periodString + "");
				} else if (b.Eccentricity > 0.9 && !b.isStar())
				{

					writer.println("\t has high Eccentricity " + record("ecc", b.Eccentricity, b)
							+ " and an orbital period of " + periodString + "(" + f.format(orbitalDays) + " days)");
				}

				// Fast orbit planets
				if (!b.BodyName.contains(" Ring") && isEmpty(b.StarType))
				{
					record("orbit", orbitalDays, b);
					if (Math.abs(orbitalDays) < 0.20)
						writer.println("\t has a fast orbital period: " + record("orbit", orbitalDays, b) + " and is "
								+ b.tLocked());
				}
				writer.flush();
				String out = sw.getBuffer().toString();
				if (!out.isEmpty())
				{
					if (!out.contains("landable") && b.Landable)
						out += ("\t is landable");
					System.out.println(b.timestamp + ":" + "Body '" + b.BodyName + "'");
					System.out.println(out);
					out = out.replaceAll("\n", ":");
					eventConsumer.accept(new EventData(b.timestamp, b.BodyName, b.PlanetClass, out, b.Landable));

				}
			}
		});

	}

	private static String getPeriod(final double orbitalPeriod)
	{
		DateTime s = new DateTime();
		DateTime next = s.withFieldAdded(DurationFieldType.days(), (int) orbitalPeriod);

		Period p = new Period(s, next);

		return PeriodFormat.getDefault().print(p);

	}

	private static void parseEvents(final Set<String> collectedEvents, final Set<Event> sortedEvents)
	{
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.ignoreUnknownElements();
		xstream.autodetectAnnotations(true);
		xstream.alias("bodyc", Body.class);
		xstream.alias("Parents", Parent.class);
		xstream.alias("Ring", Ring.class);
		xstream.alias("jump", FSDJump.class);

		sortedEvents.addAll(parseScans(collectedEvents, xstream).values());
		/*
		collectedEvents.parallelStream().forEach(s -> {
			if (!s.contains("\"event\":\"FSDJump"))
				return;
			FSDJump jump;

			synchronized (xstream)
			{
				jump = (FSDJump) xstream.fromXML("{\"jump\": " + s + "}");
			}
			sortedEvents.add(jump);
		});
		*/

	}

	static Map<String, Body> parseScans(final Collection<String> collectedScans, final XStream xstream)
	{
		Map<String, Body> bodies = Collections.synchronizedMap(new TreeMap<>());
		//for (String s : collectedScans)
		Integer totalSize = (collectedScans.size());
		AtomicInteger scanIndex = new AtomicInteger(0);
		collectedScans.parallelStream().forEach(s -> {
			try
			{
				EventBus.publish(new ScanParse("Scan", scanIndex.addAndGet(1), totalSize));
				if (!s.contains("\"event\":\"Scan"))
					return;
				if (!s.contains("BodyName"))
					return;

				Body body;
				synchronized (xstream)
				{
					body = (Body) xstream.fromXML("{\"bodyc\": " + s + "}");
				}
				if (body == null || body.BodyName == null || bodies.containsKey(body.BodyName))
				{
					//System.err.println("XML parse failed " + s);
					return;
				}
				body.ref = s;
				body.Parents = new LinkedList<>();
				if (s.contains("Ring") || s.contains("Parents"))
				{
					JSONObject obj = new JSONObject(s);
					if (obj.has("Rings"))
					{
						body.Rings = new LinkedList<>();
						JSONArray rings = obj.getJSONArray("Rings");
						synchronized (xstream)
						{
							for (int i = 0; i < rings.length(); i++)
							{
								JSONObject robj = rings.getJSONObject(i);
								if (!robj.getString("Name").contains(" Belt"))
									body.Rings.add((Ring) xstream.fromXML("{\"Ring\": " + robj.toString() + "}"));
							}

						}
					}
					if (obj.has("Parents"))
					{
						JSONArray parents = obj.getJSONArray("Parents");
						for (int i = 0; i < parents.length(); i++)
						{
							Parent p = new Parent();
							JSONObject po = parents.getJSONObject(i);
							p.name = po.names().getString(0);
							p.id = po.getLong(p.name);

							body.Parents.add(p);

						}
					}
				}
				bodies.put(body.BodyName, body);
			} catch (Exception ex)
			{
				ex.printStackTrace();
			}
		});

		bodies.forEach((a, b) -> {
			String body = b.BodyName;
			if (b.BodyParents == null)
				b.BodyParents = new LinkedList<>();

			int lastSpace = body.lastIndexOf(' ');
			if (lastSpace >= 0)
			{
				String parentStr = body.substring(0, lastSpace);
				if (bodies.containsKey(parentStr))
				{
					Body parent = bodies.get(parentStr);
					b.BodyParents.add(parent);
					if (parent.BodyChildren == null)
						parent.BodyChildren = new LinkedList<>();
					parent.BodyChildren.add(b);
				}
			}
		});

		return bodies;
	}

	private static Set<String> collectEvents(final String journalDirectory, final Date lastEntry) throws Exception
	{
		Set<String> list = Collections.synchronizedSet(new TreeSet<>());

		File[] files = new File(journalDirectory).listFiles();
		if (files == null || files.length == 0)
		{
			EventBus.publish(new EventData(new Date(), "ERROR", "", "Invalid directory specified", false));
			return list;
		}
		Integer totalSize = files.length;
		AtomicInteger scanIndex = new AtomicInteger(0);

		Stream.of(files).parallel().forEach(f -> {
			try
			{
				EventBus.publish(new ScanParse("Collect", scanIndex.addAndGet(1), totalSize));
				list.addAll(collectEventsFromFile(f, lastEntry));
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		return list;

	}

	private static Set<String> collectEventsFromFile(final File f, final Date lastEntry) throws Exception
	{
		Set<String> list = new TreeSet<>();
		try
		{
			String fileName = f.getName();
			if (fileName.startsWith("Journal") && fileName.endsWith(".log"))
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'z");
				List<String> lines = Files.readAllLines(f.toPath());
				for (String s : lines)
				{
					if (!s.contains("Belt Cluster"))
					{
						JSONObject obj = new JSONObject(s);
						try
						{
							String time = obj.getString("timestamp") + "GMT";
							Date date = sdf.parse(time);
							if (date.after(lastEntry))
							{
								String evt = obj.getString("event");
								//if (evt.equals("Scan"))
								list.add(s);
							}
						} catch (Exception ex)
						{
							System.out.println("ERR LINE:" + s);
							ex.printStackTrace();
						}

					}
				}

			}
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return list;

	}

	/** copied from apache commons to reduce file size */
	public static final int INDEX_NOT_FOUND = -1;

	public static boolean isEmpty(final CharSequence cs)
	{
		return cs == null || cs.length() == 0;
	}

	public static int countMatches(final CharSequence str, final CharSequence sub)
	{
		if (isEmpty(str) || isEmpty(sub))
		{
			return 0;
		}
		int count = 0;
		int idx = 0;
		while ((idx = indexOf(str, sub, idx)) != INDEX_NOT_FOUND)
		{
			count++;
			idx += sub.length();
		}
		return count;
	}

	static int indexOf(final CharSequence cs, final CharSequence searchChar, final int start)
	{
		return cs.toString().indexOf(searchChar.toString(), start);
	}
}
