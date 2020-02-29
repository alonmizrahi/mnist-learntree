package learntree;

import java.util.*;

import predict.Predict;

public class LearnTree {
	public Node root;
	public PriorityQueue<Node> leaves; // max heap based on leaf weighted IG
	public ArrayList<Condition> conditions;

	// information fields
	public int internalNodes;
	public double entropy; // weighted entropy
	HashMap<Class<? extends Condition>, Integer> conditionsCounter;

	public LearnTree(int[][] data, ArrayList<Condition> conditions) {
		this.conditions = conditions;
		this.internalNodes = 0;
		this.conditionsCounter = new HashMap<>();
		this.leaves = new PriorityQueue<>( (Node a, Node b) -> (b.weightedIG - a.weightedIG < 0 ? -1 : 1) );

		// set root (leaf) node
		this.root = new Node();
		this.root.samples = data.length;
		this.root.examples = new int[data.length];
		for(int i = 0; i < data.length; i++) {
			this.root.values[Condition.exampleToLabel[i]]++;
			this.root.examples[i] = i;
		}
		this.root.calcLabel();
		this.root.calcEntropy();
		this.root.calcCondition(conditions);
		
		this.leaves.add(root);
		this.entropy = this.root.entropy * (double)this.root.samples;
	}
	
	public void diverge() {
		Node leaf = leaves.peek();
		if(leaf == null)
			return;

		double leafWeightedEntropy = leaf.weightedEntropy; // save this before diverging
		if(!leaf.diverge(conditions)) // try to diverge (might fail for logical reasons)
			return;
		
		leaf = leaves.poll();
		if(leaf == null) return; //should never happen

		entropy -= leafWeightedEntropy;

		//update information fields
		Class<? extends Condition> chosenCondition = leaf.condition.getClass();
		Integer conditionCount = conditionsCounter.get(chosenCondition);
		if(conditionCount == null)
			conditionCount = 0;
		conditionsCounter.put(chosenCondition, conditionCount+1);
		internalNodes++;
		entropy += (leaf.left.weightedEntropy + leaf.right.weightedEntropy);

		// add new leaves to heap
		leaves.add(leaf.left);
		leaves.add(leaf.right);
	}
	
	public String visualize() {
		return TreeVisualizer.getCode(this.root);
	}

	public String getConditionsUsage() {
		StringBuilder sb = new StringBuilder();

		for(Class<? extends Condition> cond : conditionsCounter.keySet()) {
			String name = cond.getSimpleName();
			int count = conditionsCounter.get(cond);
			sb.append(name + " : " + count + " / " + this.internalNodes + "\n");
		}

		return sb.toString();
	}

	public static int largest(int[] arr)
    {
        int max = arr[0];
        int maxi = 0;
        for (int i = 1; i < arr.length; i++)
            if (arr[i] > max) {
                max = arr[i];
                maxi = i;
            }
        return maxi;
    }
	
	public static int largest(double[] arr) 
    {
		double max = arr[0];
        int maxi = 0;
        for (int i = 1; i < arr.length; i++) 
            if (arr[i] > max) {
                max = arr[i];
                maxi = i;
            }
        return maxi; 
    }

	private static int[][] mergeArray(int[][] data, int[][] validationData) {
		int[][] allData = new int[data.length + validationData.length][];
		for(int i = 0; i < data.length; i++)
			allData[i] = data[i]; // shallow copy is fine
		for(int i = 0; i < validationData.length; i++)
			allData[data.length + i] = validationData[i];
		return allData;
	}

	public static void main(String[] args) {
		if(args.length != 5) {
			System.err.println("learntree <1/2> <P> <L> <trainingset_filename> <outputtree_filename>");
			return;
		}

		int validationPercentage = Integer.parseInt(args[1]);
		int treePower = Integer.parseInt(args[2]);

		ArrayList<Integer[]> csvData = CsvReader.readCsv(args[3]);
		if(csvData == null) {
			System.err.println("Could not load file \"" + args[3] + "\".");
			return;
		}
		
		// shuffle data
		Collections.shuffle(csvData, new Random());
		
		int numValidationData =
				(int)((double)validationPercentage * (double) csvData.size() / 100.0);
		
		// copy data to primitive arrays, just for convenience
		int[][] data = new int[csvData.size() - numValidationData][];
		int[][] validationData = new int[numValidationData][];
		for(int i = 0; i < data.length; i++) {
			Integer[] arr = csvData.get(i);
			data[i] = new int[arr.length];
			for(int j = 0; j < arr.length; j++){
				data[i][j] = arr[j];
			}
		}
		for(int i = 0; i < validationData.length; i++) {
			Integer[] arr = csvData.get(data.length + i);
			validationData[i] = new int[arr.length];
			for(int j = 0; j < arr.length; j++) {
				validationData[i][j] = arr[j];
			}
		}
		
		// free csv data
		csvData.clear();
		csvData = null;

		// generate conditions and pre-calculate answers
		ArrayList<Condition> conditions = args[0].equals("1") ?
				Condition.generateConditions1() : Condition.generateConditions2(-1, -1);
		Condition.calcConditionsAnswers(conditions, data, validationData);

		LearnTree tree = new LearnTree(data, conditions);
		double[] successRates = new double[treePower+1]; // remember success rate for each tree size

		for(int L = 0; L <= treePower; L++) {
			int T = L > 0 ? (1 << (L-1)) : 1;

			for(int i = 0 ; i < T; i++)
				tree.diverge();

			successRates[L] = Predict.predict(tree.root, validationData);
		}

		tree = null; // free current tree

		// get the best tree size
		int L = largest(successRates);
		int T = (1 << L); // num of diverges
		
		// merge data and validationData
		int[][] allData = mergeArray(data, validationData);

		// invalidate objects
		data = null;
		validationData = null;

		// create the final tree
		tree = new LearnTree(allData, conditions);
		for(int i = 0; i < T; i++)
			tree.diverge();

		// clear condition data before writing to disk
		Condition.conditionsAnswers = null;
		Condition.exampleToLabel = null;

		// write tree to disk
		try {
			TreeReaderWriter.write(tree.root, args[4]);
		} catch(RuntimeException e) {
			System.err.println(e.getMessage());
		}

		// print output
		System.out.println("num: " + allData.length);
		System.out.println("error: " + (100 - (int)Predict.predict(tree.root, allData)));
		System.out.println("size: " + tree.internalNodes);
	}
}
