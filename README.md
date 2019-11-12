# mnist-learntree

A small machine learning project coded in Java.

The algorithm builds a decision tree from the MNIST database (could be any other .csv database).

In each diversion of the tree, there is a greedy algorithm which finds the best <L,X> where L is a leaf and X is a diversion-condition, to maximize the information-gain in that specific diversion. Finally, we export the tree as a .tree file.

We then use *predict* to test the exported tree on an un-labeled database.
