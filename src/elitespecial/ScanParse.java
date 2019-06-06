package elitespecial;

public class ScanParse
{
	int index = 0;
	int totalSize = 0;
	String type;

	public ScanParse(final String type, final int i, final Integer totalSize)
	{
		this.type = type;
		this.totalSize = totalSize;
		index = i;
	}

}
