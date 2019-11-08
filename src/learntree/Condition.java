package learntree;

import java.io.Serializable;
import java.util.ArrayList;

public abstract class Condition implements Serializable {
	private static final long serialVersionUID = 1L;

	public abstract boolean ask(int[] example);
	
	public static ArrayList<Condition> generateConditions1() {
		ArrayList<Condition> conds = new ArrayList<Condition>();
		
		for(int i = 0; i < 28; i+=2)
			for(int j = 0; j < 28; j+=2)
				conds.add(new PixelCondition(j, i, 128, true));

		return conds;
	}
	
	public static ArrayList<Condition> generateConditions2() {
		ArrayList<Condition> conds = generateConditions1();
		
		// add more conditions
		
		return conds;
	}
}
