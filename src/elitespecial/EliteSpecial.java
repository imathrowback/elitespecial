package elitespecial;

import static javax.measure.unit.SI.METRE;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.eventbus.Subscribe;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

public class EliteSpecial
{
	static Unit<Length> LIGHT_SECOND = METRE.times(299792458);
	public boolean shouldStop = false;

	public EliteSpecial()
	{
		SpecialEventBus.eventBus.register(this);
	}

	public void stop()
	{
		shouldStop = true;
	}

	public void goSystemMode(final String journalDirectory, final Consumer<EventData> eventConsumer)
	{

	}

	public void goHistoryMode(final String journalDirectory, final int days, final Consumer<EventData> eventConsumer)
			throws Exception
	{
		LocalDate startDate = new LocalDate().minusDays(days);
		//startDate = new LocalDate().minusYears(10);
		Date lastEntry = startDate.toDateMidnight().toDate();

		WatchService watcher = FileSystems.getDefault().newWatchService();
		Path journalPath = new File(journalDirectory).toPath();
		WatchKey key = journalPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_CREATE);
		System.out.println("Scan journals");
		eventConsumer.accept(new EventData(new DateTime(), "INFO", "", "Scanning journals...", false));
		Set<String> collectedEvents = collectEventsFromJournalDirectory(journalDirectory, lastEntry);
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
					if (kind == StandardWatchEventKinds.ENTRY_MODIFY)
					{
						WatchEvent<Path> ev = (WatchEvent<Path>) we;
						Path filename = journalPath.resolve(ev.context());
						System.out.println("entry modify:" + filename);
						if (filename.getFileName().toString().endsWith("log"))
						{
							collectedEvents.addAll(collectEventsFromFile(filename.toFile(), lastEntry));
							System.out.println("got events from file:" + collectedEvents.size());
							if (!collectedEvents.isEmpty())
								collectBodies = true;
						}
					} else
						System.err.println("unhandled kind:" + kind);
				}
			}
			if (collectBodies)
			{
				TreeSet<Event> sortedEvents = new TreeSet<>(Event.comp);
				if (collectedEvents.size() > 0)
				{
					System.out.println("existing current system events:" + currentSystemEvents.size());
					collectedEvents.addAll(currentSystemEvents);
					mutateJournalsIntoEvents(collectedEvents, sortedEvents);
					if (sortedEvents.size() > 0)
					{
						updateSystemForEvents(sortedEvents);

						List<Body> newCurrentSystemEvents = sortedEvents.stream().filter(e -> e instanceof Body)
								.map(e -> (Body) e)
								.filter(e -> e.system.equals(currentSystem))
								.collect(Collectors.toList());
						System.out.println("new current system events:" + newCurrentSystemEvents);
						if (!newCurrentSystemEvents.isEmpty())
						{
							newCurrentSystemEvents.forEach(e -> currentSystemEvents.add(e.ref));
							//currentSystemEvents.addAll(newCurrentSystemEvents);
							// re-run the rules for the "current system events"
							//runRulesForEvents(currentSystemEvents, eventConsumer);
						}

						runRulesForEvents(sortedEvents, eventConsumer);
						lastEntry = sortedEvents.first().timestamp.toDate();
						System.out.println("new last entry =" + lastEntry);
					}
				}
				collectedEvents.clear();
				//new Date();
				collectBodies = false;
				SpecialEventBus.eventBus.post(new ScanParse("Done...watching for changes", 0, 0));
			}
			Thread.sleep(100);
		}

	}

	private void updateSystemForEvents(final Set<Event> sortedEvents)
	{
		Stream<Body> bodies = sortedEvents.stream().filter(e -> e instanceof Body).map(e -> (Body) e);
		bodies.forEach(b -> {
			b.system = systemStateChangeEvents.lowerEntry(b.timestamp).getValue();
		});

	}

	TreeMap<DateTime, String> systemStateChangeEvents = new TreeMap<>();
	String currentSystem;
	DateTime currentSystemTime;
	Set<String> currentSystemEvents = new TreeSet<>();

	@Subscribe
	public void onSystemChange(final CurrentSystemEvent evt)
	{
		systemStateChangeEvents.put(evt.time, evt.system);
		if (currentSystemTime == null || evt.time.isAfter(currentSystemTime) && !evt.system.equals(currentSystem))
		{
			System.out.println("change system from [" + currentSystem + "] to [" + evt.system + "]");
			currentSystemEvents.clear();
			currentSystem = evt.system;
			currentSystemTime = evt.time;
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

	public static void main(final String args[]) throws Exception
	{
		final String dir = "c:\\ftemp";
		final String file = "c:\\ftemp\\Journalx.log";

		PrintWriter f = new PrintWriter(new File(file));
		f.flush();
		f.close();

		Thread r = new Thread(() -> {
			try
			{
				new EliteSpecial().goHistoryMode(dir, 100, (e) -> {
				});
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		r.start();

		Thread.sleep(2000);
		out(file,
				"{ \"timestamp\":\"2019-06-09T12:57:48Z\", \"event\":\"FSDJump\", \"StarSystem\":\"Swoilz RL-B d14-14\", \"SystemAddress\":491563944443, \"StarPos\":[199.62500,-458.81250,1437.31250], \"SystemAllegiance\":\"\", \"SystemEconomy\":\"$economy_None;\", \"SystemEconomy_Localised\":\"None\", \"SystemSecondEconomy\":\"$economy_None;\", \"SystemSecondEconomy_Localised\":\"None\", \"SystemGovernment\":\"$government_None;\", \"SystemGovernment_Localised\":\"None\", \"SystemSecurity\":\"$GAlAXY_MAP_INFO_state_anarchy;\", \"SystemSecurity_Localised\":\"Anarchy\", \"Population\":0, \"Body\":\"Swoilz RL-B d14-14 A\", \"BodyID\":1, \"BodyType\":\"Star\", \"JumpDist\":51.612, \"FuelUsed\":4.718585, \"FuelLevel\":63.497074 }\r\n");
		Thread.sleep(2000);
		out(file,
				"{ \"timestamp\":\"2019-06-09T13:00:56Z\", \"event\":\"Scan\", \"ScanType\":\"Detailed\", \"BodyName\":\"Swoilz RL-B d14-14 A 2 c a\", \"BodyID\":17, \"Parents\":[ {\"Planet\":16}, {\"Planet\":11}, {\"Star\":1}, {\"Null\":0} ], \"DistanceFromArrivalLS\":1170.508789, \"TidalLock\":true, \"TerraformState\":\"\", \"PlanetClass\":\"Rocky body\", \"Atmosphere\":\"\", \"AtmosphereType\":\"None\", \"Volcanism\":\"major silicate vapour geysers volcanism\", \"MassEM\":0.000174, \"Radius\":399503.406250, \"SurfaceGravity\":0.434358, \"SurfaceTemperature\":234.227661, \"SurfacePressure\":0.000000, \"Landable\":true, \"Materials\":[ { \"Name\":\"sulphur\", \"Percent\":20.021624 }, { \"Name\":\"iron\", \"Percent\":17.916983 }, { \"Name\":\"carbon\", \"Percent\":16.836115 }, { \"Name\":\"nickel\", \"Percent\":13.551654 }, { \"Name\":\"phosphorus\", \"Percent\":10.778768 }, { \"Name\":\"chromium\", \"Percent\":8.057861 }, { \"Name\":\"manganese\", \"Percent\":7.399531 }, { \"Name\":\"zirconium\", \"Percent\":2.080531 }, { \"Name\":\"antimony\", \"Percent\":1.249991 }, { \"Name\":\"molybdenum\", \"Percent\":1.169968 }, { \"Name\":\"tin\", \"Percent\":0.936971 } ], \"Composition\":{ \"Ice\":0.000000, \"Rock\":0.968041, \"Metal\":0.031959 }, \"SemiMajorAxis\":4579699.500000, \"Eccentricity\":0.000000, \"OrbitalInclination\":49.649097, \"Periapsis\":222.797897, \"OrbitalPeriod\":58734.988281, \"RotationPeriod\":60679.679688, \"AxialTilt\":0.123455, \"WasDiscovered\":false, \"WasMapped\":false }\r\n"
						+
						"");

		Thread.sleep(2000);
		out(file,
				"{ \"timestamp\":\"2019-06-09T13:00:58Z\", \"event\":\"Scan\", \"ScanType\":\"Detailed\", \"BodyName\":\"Swoilz RL-B d14-14 A 2 c\", \"BodyID\":16, \"Parents\":[ {\"Planet\":11}, {\"Star\":1}, {\"Null\":0} ], \"DistanceFromArrivalLS\":1170.500610, \"TidalLock\":true, \"TerraformState\":\"\", \"PlanetClass\":\"Rocky body\", \"Atmosphere\":\"\", \"AtmosphereType\":\"None\", \"Volcanism\":\"\", \"MassEM\":0.002584, \"Radius\":961852.562500, \"SurfaceGravity\":1.113161, \"SurfaceTemperature\":234.227661, \"SurfacePressure\":0.000000, \"Landable\":true, \"Materials\":[ { \"Name\":\"iron\", \"Percent\":20.155159 }, { \"Name\":\"sulphur\", \"Percent\":19.342897 }, { \"Name\":\"carbon\", \"Percent\":16.265375 }, { \"Name\":\"nickel\", \"Percent\":15.244516 }, { \"Name\":\"phosphorus\", \"Percent\":10.413372 }, { \"Name\":\"chromium\", \"Percent\":9.064444 }, { \"Name\":\"selenium\", \"Percent\":3.027327 }, { \"Name\":\"zirconium\", \"Percent\":2.340429 }, { \"Name\":\"cadmium\", \"Percent\":1.565139 }, { \"Name\":\"niobium\", \"Percent\":1.377498 }, { \"Name\":\"yttrium\", \"Percent\":1.203843 } ], \"Composition\":{ \"Ice\":0.000000, \"Rock\":0.905138, \"Metal\":0.094862 }, \"SemiMajorAxis\":3096050944.000000, \"Eccentricity\":0.000278, \"OrbitalInclination\":-0.041463, \"Periapsis\":12.174594, \"OrbitalPeriod\":1322055.500000, \"RotationPeriod\":1322066.875000, \"AxialTilt\":-1.239440, \"WasDiscovered\":false, \"WasMapped\":false }\r\n");

		Thread.sleep(2000);
		r.interrupt();
		/*
		Preferences prefs = Preferences.userNodeForPackage(MainForm.class);
		
		String journalDirectory = prefs.get("journalDirectory", MainForm.getFrontierSavedGAmesDirectory());
		int days = prefs.getInt("days", 365);
		
		new EliteSpecial().goHistoryMode(journalDirectory, days, (e) -> {
		});
		*/
	}

	private static void out(final String file, final String string) throws Exception
	{
		try (FileWriter fw = new FileWriter(file, true))
		{
			fw.append(string);
		}

	}

	public static void runRulesForEvents(final Set<Event> sortedEvents, final Consumer<EventData> eventConsumer)
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
					if (m2LS.convert(axis) < 0.3)
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
						if (m2KM.convert(r.OuterRad - r.InnerRad) < 300)
							writer.println("\t rings are small in width "
									+ f.format(m2KM.convert(r.OuterRad - r.InnerRad)));
					}
				}

				//System.out.println("[" + b.BodyName + "] binary pair?" + binaryPair);
				if (binaryPair)
				{
					Set<Body> siblings = getBinarySiblings(b);
					//if (siblings.size() > 3)
					//writer.println("\t Multi orbiting x" + siblings.size());
					String name = "Binary pair";
					if (siblings.size() == 3)
						name = "Trinary triplet";
					else if (siblings.size() > 3)
						name = "Multi[" + siblings.size() + "] orbiting";
					name = "";
					//System.out.println("Found siblings[" + siblings + "] of body " + b.BodyName);
					Body xa = null;
					Body xb = null;
					double min = Double.MAX_VALUE;
					Body aa = b;
					//for (Body aa : siblings)
					for (Body bb : siblings)

						if (aa != bb && b.parentString().equals(bb.parentString()))
						{

							min = Math.min(min, Math.abs(aa.SemiMajorAxis + bb.SemiMajorAxis));
							xa = aa;
							xb = bb;
						}

					double km = m2KM.convert(min);
					if (km < 25000)
						writer.println("\t" + name + " really close to " + xb.BodyName + ", " + f.format(km) + " km");
					//System.out.println(f.format(km));

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

				if (Math.abs(Math.toDegrees(b.AxialTilt)) > 70 && b.Landable)
					writer.println("\t has high axial tilt " + f.format(Math.toDegrees(b.AxialTilt)));

				// Fast orbit planets
				if (!b.BodyName.contains(" Ring") && isEmpty(b.StarType))
				{
					record("orbit", orbitalDays, b);
					if (Math.abs(orbitalDays) < 0.30)
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

	private static Set<Body> getBinarySiblings(final Body b)
	{
		Set<Body> siblings = new TreeSet<>(Event.comp);
		Set<Integer> nullIDs = b.getParents("Null").parallelStream().mapToInt(x -> (int) x.id).distinct().boxed()
				.collect(Collectors.toSet());

		for (Body bodyParent : b.BodyParents)
		{
			for (Body child : bodyParent.BodyChildren)
			{
				boolean match = child.getParents("Null").stream().mapToInt(x -> (int) x.id)
						.anyMatch(x -> nullIDs.contains(x));
				if (match)
					siblings.add(child);
			}
		}

		return siblings;

		/*
		
		List<Parent> nullParents = b.getParents("Null");
		List<Parent> starParents = b.getParents("Star");
		
		Body bodyParent = b.getBodyParent(starParent.id);
		System.out.println(
				"body parent for body[" + b.BodyName + "], star id[" + starParent.id + "] = " + bodyParent.BodyName);
		return bodyParent.BodyChildren.stream().filter(x -> {
			Parent pp = x.getParent("Null");
			if (pp != null)
				return x.getParent("Null").id == nullParent.id && x != b;
			return false;
		})
				.collect(Collectors.toList());
				*/
	}

	private static String getPeriod(final double orbitalPeriod)
	{
		DateTime s = new DateTime();
		DateTime next = s.withFieldAdded(DurationFieldType.days(), (int) orbitalPeriod);

		Period p = new Period(s, next);

		return PeriodFormat.getDefault().print(p);

	}

	static class XStreamPool
	{
		LinkedBlockingQueue<XStream> streams = new LinkedBlockingQueue<>();

		public XStreamPool()
		{
			for (int i = 0; i < 8; i++)
				streams.add(make());
		}

		public XStream checkout() throws InterruptedException
		{
			return streams.take();
		}

		public void checkin(final XStream str) throws InterruptedException
		{
			streams.put(str);
		}

		private XStream make()
		{
			XStream xstream = new XStream(new JettisonMappedXmlDriver());
			xstream.ignoreUnknownElements();
			xstream.autodetectAnnotations(true);
			xstream.alias("bodyc", Body.class);
			xstream.alias("Parents", Parent.class);
			xstream.alias("Ring", Ring.class);
			xstream.alias("jump", FSDJump.class);
			return xstream;
		}
	}

	private static void mutateJournalsIntoEvents(final Set<String> collectedEvents, final Set<Event> sortedEvents)
	{
		Map<String, Body> parsed = mutateJournalStringsIntoBodies(collectedEvents);
		sortedEvents.addAll(parsed.values());
	}

	static Map<String, Body> mutateJournalStringsIntoBodies(final Collection<String> collectedScans)
	{
		XStreamPool xstrProvider = new XStreamPool();
		Map<String, Body> bodies = Collections.synchronizedMap(new TreeMap<>());
		//for (String s : collectedScans)
		Integer totalSize = (collectedScans.size());
		AtomicInteger scanIndex = new AtomicInteger(0);
		collectedScans.parallelStream().forEach(s -> {
			try
			{
				SpecialEventBus.eventBus.post(new ScanParse("Scan", scanIndex.addAndGet(1), totalSize));
				if (s.contains("\"event\":\"Location") || s.contains("\"event\":\"FSDJump"))
				{
					JSONObject obj = new JSONObject(s);
					DateTime time = DateTime.parse(obj.getString("timestamp"));
					String system = obj.getString("StarSystem");
					//System.out.println("post new system: " + system);
					SpecialEventBus.eventBus.post(new CurrentSystemEvent(time, system));

					return;
				}
				if (!s.contains("\"event\":\"Scan"))
					return;
				if (!s.contains("BodyName"))
					return;

				Body body;
				XStream xstream = xstrProvider.checkout();
				body = (Body) xstream.fromXML("{\"bodyc\": " + s + "}");
				xstrProvider.checkin(xstream);
				if (body == null || body.BodyName == null || bodies.containsKey(body.BodyName))
				{
					//System.err.println("XML parse failed " + s);
					return;
				}
				JSONObject obj = new JSONObject(s);

				body.timestamp = new DateTime(obj.get("timestamp"));
				body.ref = s;
				body.Parents = new LinkedList<>();
				if (s.contains("Ring") || s.contains("Parents"))
				{
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

	private static Set<String> collectEventsFromJournalDirectory(final String journalDirectory, final Date lastEntry)
			throws Exception
	{
		Set<String> list = Collections.synchronizedSet(new TreeSet<>());

		File[] files = new File(journalDirectory).listFiles();
		if (files == null || files.length == 0)
		{
			SpecialEventBus.eventBus
					.post(new EventData(new DateTime(), "ERROR", "", "Invalid directory specified", false));
			return list;
		}
		Integer totalSize = files.length;
		AtomicInteger scanIndex = new AtomicInteger(0);

		Stream.of(files).parallel().forEach(f -> {
			try
			{
				SpecialEventBus.eventBus.post(new ScanParse("Collect", scanIndex.addAndGet(1), totalSize));
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
			//System.out.println(fileName);
			if (fileName.startsWith("Journal") && fileName.endsWith(".log"))
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'z");
				List<String> lines = Files.readAllLines(f.toPath());
				for (String s : lines)
				{
					//	System.out.println(s);
					if (!s.contains("Belt Cluster"))
					{
						JSONObject obj = new JSONObject(s);
						try
						{
							String time = obj.getString("timestamp") + "GMT";
							Date date = sdf.parse(time);
							//System.out.println("date:" + date + " ==>" + lastEntry);
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
