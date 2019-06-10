package elitespecial;

class RecordEntry
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
		return "" + EliteSpecial.f.format(min) + "[" + bmin.BodyName + "], " + EliteSpecial.f.format(max) + "[" + bmax.BodyName + "]";
	}

}