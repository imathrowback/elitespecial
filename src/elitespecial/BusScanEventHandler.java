package elitespecial;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;

public class BusScanEventHandler implements EventSubscriber<EventData>
{
	MainForm es;

	public BusScanEventHandler(final MainForm es)
	{
		EventBus.subscribe(EventData.class, this);
		this.es = es;
	}

	@Override
	public void onEvent(final EventData event)
	{
		es.addEvent(event);

	}

}
