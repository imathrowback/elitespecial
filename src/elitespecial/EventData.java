package elitespecial;

import java.util.Date;

public class EventData
{
	public EventData(final Date timestamp, final String bodyName, final String bodyType, final String out,
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
	public Date dateTime;
	public String body;
	public String text;
}
