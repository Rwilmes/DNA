package dna.series.data.distr;

public class IntDistr extends Distr<Integer> {

	public IntDistr(String name) {
		super(name);
	}

	public IntDistr(String name, long denominator, long[] values) {
		super(name, denominator, values);
	}

	@Override
	protected int getIndex(Integer value) {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof IntDistr && super.equals(obj);
	}

}
