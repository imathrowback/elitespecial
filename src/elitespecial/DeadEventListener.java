package elitespecial;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

public class DeadEventListener
{
	@Subscribe
	public void listen(final DeadEvent event)
	{
		System.err.println(event);
	}

}