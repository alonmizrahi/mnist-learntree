package learntree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Node implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final double LOG_OF_2 = Math.log(2.0); // calculate only once
	
	public int samples;
	public int[] values;
	public Condition condition; // the condition with the highest IG
	
	// not leaf
	public Node left, right;
	
	// leaf
	public List<Integer> examples;
	public int label;
	public double entropy;
	public double weightedEntropy;
	public double IG;
	public double weightedIG; // will decide which leaf to diverge next
	
	// initiates as leaf node
	public Node() {
		// all nodes
		this.samples = 0;
		this.values = new int[10];
		this.condition = null; // needs to be calculated after examples are added
		
		// interior node
		this.left = null;
		this.right = null;

		// leaf node
		this.examples = new LinkedList<Integer>();
		this.label = 0;
		this.entropy = 0.0;
		this.weightedEntropy = 0.0;
		this.IG = 0.0;
		this.weightedIG = 0.0;
	}
	
	public boolean isLeaf() {
		return this.left == null && this.right == null;
	}
	
	public boolean diverge(int[][] data, ArrayList<Condition> conditions) {
		if(this.entropy == 0.0 || this.weightedIG == 0.0 || this.condition == null) // no need to diverge
			return false;
		
		left = new Node();
		right = new Node();
		
		// iterate over examples that go through this leaf
		for(Integer exampleIndex : examples) {
			if( condition.ask(data[exampleIndex]) ){
				right.samples++;
				right.values[data[exampleIndex][0]]++;
				right.examples.add(exampleIndex);
			}
			else {
				left.samples++;
				left.values[data[exampleIndex][0]]++;
				left.examples.add(exampleIndex);
			}
		}
		
		left.calcEntropy();
		left.calcLabel();
		left.calcCondition(data, conditions);
		right.calcEntropy();
		right.calcLabel();
		right.calcCondition(data, conditions);
		
		// set leaf parameters to invalid values
		examples = null;
		label = -1;
		entropy = -1.0;
		weightedEntropy = -1.0;
		IG = -1.0;
		weightedIG = -1.0;
		
		return true;
	}
	
	public void calcCondition(int[][] data, ArrayList<Condition> conditions) {
		if(this.entropy == 0.0) // no need to diverge this leaf anymore
			return;
		
		Node tempLeft = new Node();
		Node tempRight = new Node();
		
		this.condition = null; // the condition with the highest IG
		this.IG = 0.0;
		
		for(Condition cond : conditions) {
			tempLeft.samples = 0;
			tempLeft.values = new int[10];
			tempRight.samples = 0;
			tempRight.values = new int[10];
			
			// iterate over examples that go through this leaf
			for(Integer exampleIndex : this.examples) {
				if( cond.ask(data[exampleIndex]) ){
					tempRight.samples++;
					tempRight.values[data[exampleIndex][0]]++;
				}
				else {
					tempLeft.samples++;
					tempLeft.values[data[exampleIndex][0]]++;
				}
			}
			
			// bad condition? (did not diverge at all)
			if(tempLeft.samples == 0 || tempRight.samples == 0)
				continue;
			
			tempLeft.calcEntropy();
			tempRight.calcEntropy();
			
			double newLeavesWeightedEntropy = 
					( tempLeft.weightedEntropy + tempRight.weightedEntropy) 
					  / (double)this.samples;
			double newIG = this.entropy - newLeavesWeightedEntropy;
			
			if(newIG > this.IG) {
				this.IG = newIG;
				this.condition = cond;
			}
		}
		
		this.weightedIG = this.IG  * (double)this.samples;
	}
	
	public void calcEntropy() {
		entropy = 0.0;
		if(samples == 0) {
			weightedEntropy = 0.0;
			return;
		}
		
		for(int i = 0; i < 10; i++) {
			if(values[i] == 0)
				continue;
			
			double quotient = (double)values[i] / (double)samples;
			entropy += quotient * log(1.0 / quotient);
		}
		
		weightedEntropy = entropy * (double)samples;
	}
	
	public void calcLabel() {
		this.label = LearnTree.largest(this.values);
	}
	
	private static double log(double x) { // base 2 log
		return Math.log(x) / LOG_OF_2;
	}
}
