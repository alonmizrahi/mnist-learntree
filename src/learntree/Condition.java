package learntree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class Condition implements Serializable {
	private static final long serialVersionUID = 1L;

	public static boolean[][] conditionsAnswers; // examples X conditions
	public static int[] exampleToLabel;

	public int index; // every condition is referred to by its index

	public abstract boolean ask(int[] example);
	
	public final static ArrayList<Condition> generateConditions1() {
		ArrayList<Condition> conds = new ArrayList<>();

		conds.addAll(PixelThresholdCondition.generateConditions(128));

		// add index to each condition
		for(int i = 0; i < conds.size(); i++)
			conds.get(i).index = i;

		return conds;
	}
	
	public final static ArrayList<Condition> generateConditions2(int p1, int p2) {
		ArrayList<Condition> conds = new ArrayList<>();

		conds.addAll(PixelThresholdCondition.generateConditions(25)); // a better threshold
		conds.addAll(PixelRectangleCondition.generateConditions(2,6,4,25));
		for(int k = 1; k <= 10; k++)
			conds.addAll(LineColumnCondition.generateConditions(k, 25)); // same as rectangle condition
		// we use LineColumnCondition and not PixelRectangleCondition to differ the two as we build the tree

		// add index to each condition
		for(int i = 0; i < conds.size(); i++)
			conds.get(i).index = i;

		return conds;
	}

	public static void calcConditionsAnswers(ArrayList<Condition> conds, int[][] data, int[][] validationData) {
		int n = data.length + validationData.length;
		conditionsAnswers = new boolean[n][conds.size()];
		exampleToLabel = new int[n];

		for(int i = 0; i < data.length; i++) {
			boolean[] answers = conditionsAnswers[i];
			for(int j = 0; j < conds.size(); j++) {
				answers[j] = conds.get(j).ask(data[i]);
				if(answers[j])
					answers[j] = answers[j];
			}
			exampleToLabel[i] =  data[i][0];
		}

		for(int i = 0; i < validationData.length; i++) {
			boolean[] answers = conditionsAnswers[data.length + i];
			for(int j = 0; j < conds.size(); j++)
				answers[j] = conds.get(j).ask(validationData[i]);
			exampleToLabel[data.length + i] = validationData[i][0];
		}
	}

	public boolean answersYesTo(int exampleIndex) {
		return conditionsAnswers[exampleIndex][this.index];
	}
}
