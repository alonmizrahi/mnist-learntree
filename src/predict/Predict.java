package predict;

import learntree.Node;

public class Predict {
	
	// returns label (0-9)
	public static int predict(Node root, int[] example) {
		while(!root.isLeaf()) {
			if(root.condition.ask(example))
				root = root.right;
			else
				root = root.left;
		}
		
		return root.label;
	}
	
	//returns percentage
	public static double predict(Node root, int[][] examples) {
		int succeeded = 0;
		
		for(int i = 0; i < examples.length; i++)
			if(predict(root, examples[i]) == examples[i][0])
				succeeded++;
		
		return ((double)succeeded / (double)examples.length) * 100.0;
	}
	
	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("predict <tree_filename> <testset_filename>");
			return;
		}
		
		
	}
}
