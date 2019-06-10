package elitespecial;

import org.joda.time.DateTime;

public class CurrentSystemEvent
{
	String system;
	DateTime time;

	public CurrentSystemEvent(final DateTime time, final String system)
	{
		super();
		this.time = time;
		this.system = system;
	}

	public DateTime getTime()
	{
		return time;
	}

	public String getSystem()
	{
		return system;
	}

}
