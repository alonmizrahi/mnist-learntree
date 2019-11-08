package learntree;

public class PixelCondition extends Condition {
	private static final long serialVersionUID = 1L;
	
	public int x, y;
	public int threshold;
	public boolean biggerThan;
	
	public PixelCondition(int x, int y, int threshold, boolean biggerThan) {
		this.x = x;
		this.y = y;
		this.threshold = threshold;
		this.biggerThan = biggerThan;
	}
	
	public boolean ask(int[] example) {
		int val = example[1 + x + y*28];
		if(biggerThan)
			return val > threshold;
		return val <= threshold;
	}
}
