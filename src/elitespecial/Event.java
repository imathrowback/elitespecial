package elitespecial;

import java.util.Comparator;
import org.joda.time.DateTime;

class Event
{
	static Comparator<Event> comp = Comparator.comparing(Event::getTimestamp).thenComparing(Event::getID);

	public Event()
	{

	}

	static long gid = 90;

	long id = gid++;

	public long getID()
	{
		if (id == 0)
			id = gid++;
		return id;
	}

	public DateTime timestamp;

	public DateTime getTimestamp()
	{
		return timestamp;
	}

}