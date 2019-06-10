package elitespecial;

import java.util.concurrent.LinkedBlockingQueue;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

class XStreamProvider
{
	LinkedBlockingQueue<XStream> streams = new LinkedBlockingQueue<>();

	public XStreamProvider()
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