package elitespecial;

import org.joda.time.DateTime;

public class EventData
{
	public EventData(final DateTime timestamp, final String bodyName, final String bodyType, final String out,
			final boolean landable)
	{
		dateTime = timestamp;
		body = bodyName;
		text = out;
		this.landable = landable;
		this.bodyType = bodyType;
	}

	public String bodyType;
	public boolean landable;
	public DateTime dateTime;
	public String body;
	public String text;
}
