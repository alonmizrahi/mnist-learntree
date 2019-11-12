package learntree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;

import predict.Predict;

public class LearnTree {
	public Node root;
	public int[][] data;
	public PriorityQueue<Node> leaves; // max heap based on leaf weighted IG
	public ArrayList<Condition> conditions;
	
	public int internalNodes;
	public double entropy; // weighted entropy
	
	public LearnTree(int[][] data, ArrayList<Condition> conditions) {
		this.data = data;
		this.conditions = conditions;
		this.internalNodes = 0;
		this.leaves = new PriorityQueue<>(
					    (Node a, Node b) -> (b.weightedIG - a.weightedIG < 0 ? -1 : 1)
						);

		// set root (leaf) node
		this.root = new Node();
		this.root.samples = data.length;
		for(int i = 0; i < data.length; i++) {
			this.root.values[data[i][0]]++;
			this.root.examples.add(i);
		}
		this.root.calcLabel();
		this.root.calcEntropy();
		this.root.calcCondition(data, conditions);
		
		this.leaves.add(root);
		this.entropy = this.root.entropy * (double)this.root.samples;
	}
	
	public void diverge() {
		Node leaf = leaves.peek();
		if(leaf == null)
			return;
		
		double leafWeightedEntropy = leaf.weightedEntropy; // save this before diverging
		if(!leaf.diverge(data, conditions)) // try to diverge (might fail for logical reasons)
			return;
		
		leaf = leaves.poll();
		entropy -= leafWeightedEntropy;
		
		internalNodes++;
		entropy += (leaf.left.weightedEntropy + leaf.right.weightedEntropy);
		
		leaves.add(leaf.left);
		leaves.add(leaf.right);
	}
	
	public String visualize() {
		return TreeVisualizer.getCode(this.root);
	}
	
	public static int largest(int[] arr) 
    {
        int max = arr[0];
        int maxi = 0;
        for (int i = 1; i < arr.length; i++) 
            if (arr[i] > max) {
                max = arr[i]; maxi = i; }
        return maxi; 
    }
	
	public static int largest(double[] arr) 
    {
		double max = arr[0];
        int maxi = 0;
        for (int i = 1; i < arr.length; i++) 
            if (arr[i] > max) {
                max = arr[i]; maxi = i; }
        return maxi; 
    } 
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		/*args = new String[5];
		args[0] = "1";
		args[1] = "10"; // validation percentage
		args[2] = "15"; // max L
		args[3] = "mnist_train.csv";
		args[4] = "output_tree.tree";
		*/
		if(args.length != 5) {
			System.out.println("learntree <1/2> <P> <L> <trainingset_filename> <outputtree_filename>");
			return;
		}
		
		ArrayList<Condition> conditions = args[0].equals("1") ?
										  Condition.generateConditions1() : Condition.generateConditions2();

		int validationPercentage = Integer.parseInt(args[1]);
		int treePower = Integer.parseInt(args[2]);
		
		ArrayList<Integer[]> csvData = CsvReader.readCsv(args[3]);
		if(csvData == null) {
			System.err.println("Could not load file \"" + args[3] + "\".");
			return;
		}
		
		// shuffle data
		Collections.shuffle(csvData);
		
		int numValidationData =
				(int)((double)validationPercentage * (double) csvData.size() / 100.0);
		
		// copy data to primitive arrays, just for convenience
		int[][] data = new int[csvData.size() - numValidationData][];
		int[][] validationData = new int[numValidationData][];
		for(int i = 0; i < data.length; i++) {
			Integer[] arr = csvData.get(i);
			data[i] = new int[arr.length];
			for(int j = 0; j < arr.length; j++) data[i][j] = arr[j];
		}
		for(int i = 0; i < validationData.length; i++) {
			Integer[] arr = csvData.get(data.length + i);
			validationData[i] = new int[arr.length];
			for(int j = 0; j < arr.length; j++) validationData[i][j] = arr[j];
		}
		
		// free csv data
		csvData.clear();
		csvData = null;
		
		LearnTree tree = new LearnTree(data, conditions);
		double[] successRates = new double[treePower+1]; // remember success rate for each tree size
		double noDivergesSuccessRate = Predict.predict(tree.root, validationData);
		
		for(int L = 0; L <= treePower; L++) {
			int T = L > 0 ? (1 << (L-1)) : 1;
			
			for(int i = 0 ; i < T; i++)
				tree.diverge();
			
			successRates[L] = Predict.predict(tree.root, validationData);
			
			/* // for debugging
				System.out.println("internal nodes: " + tree.internalNodes + " (L=" + L + ")");
				System.out.println("entropy: " + tree.entropy);
				System.out.println("success rate: " + successRates[L]);
				System.out.println("-----------------");
			*/
		}
		
		tree = null; // free current tree
		
		// get the best tree size
		int L = largest(successRates);
		int T = successRates[L] > noDivergesSuccessRate ? (1 << L) : 0; // num of diverges
		
		// merge data and validationData
		int[][] allData = new int[data.length + validationData.length][];
		for(int i = 0; i < data.length; i++)
			allData[i] = data[i]; // shallow copy is fine
		for(int i = 0; i < validationData.length; i++)
			allData[data.length + i] = validationData[i];
		
		data = null;
		validationData = null;
		
		// create the final tree
		tree = new LearnTree(allData, conditions);
		for(int i = 0; i < T; i++)
			tree.diverge();
		
		System.out.println("num: " + allData.length);
		System.out.println("error: " + (int)(100.0 - successRates[L]));
		System.out.println("size: " + tree.internalNodes);
		
		try {
			TreeReaderWriter.write(tree.root, args[4]);
		} catch(RuntimeException e) {
			System.err.println(e.getMessage());
		}
		
		PixelCondition cond = (PixelCondition)tree.root.condition;
		System.out.println("wrote tree with root x,y: " + cond.x + "," + cond.y);
		System.out.println("took " + (System.currentTimeMillis() - start) / 1000 + " sec.");
	}
}
