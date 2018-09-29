package quickdt.predictiveModels.decisionTree;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.twitter.common.stats.ReservoirSampler;
import org.javatuples.Pair;
import quickdt.Misc;
import quickdt.data.AbstractInstance;
import quickdt.predictiveModels.PredictiveModelBuilder;
import quickdt.predictiveModels.UpdatablePredictiveModelBuilder;
import quickdt.predictiveModels.decisionTree.scorers.MSEScorer;
import quickdt.predictiveModels.decisionTree.tree.*;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public final class TreeBuilder implements UpdatablePredictiveModelBuilder<Tree> {
    public static final int ORDINAL_TEST_SPLITS = 5;
    public static final int SMALL_TRAINING_SET_LIMIT = 10;
    public static final int RESERVOIR_SIZE = 1000;

    private final Scorer scorer;
    private int maxDepth = Integer.MAX_VALUE;
    private double ignoreAttributeAtNodeProbability = 0.0;
    private double minimumScore = 0.00000000000001;
    private int minCategoricalAttributeValueOccurances = 5;
    private int minLeafInstances = 0;
    private boolean updatable = false;

    public TreeBuilder() {
        this(new MSEScorer(MSEScorer.CrossValidationCorrection.TRUE));
    }

    public TreeBuilder(final Scorer scorer) {
        this.scorer = scorer;
    }

    public TreeBuilder maxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public TreeBuilder minLeafInstances(int minLeafInstances) {
        this.minLeafInstances = minLeafInstances;
        return this;
    }

    public TreeBuilder ignoreAttributeAtNodeProbability(double probability) {
        this.ignoreAttributeAtNodeProbability = probability;
        return this;
    }

    public TreeBuilder minCategoricalAttributeValueOccurances(int occurances) {
        this.minCategoricalAttributeValueOccurances = occurances;
        return this;
    }

    public TreeBuilder minimumScore(double minimumScore) {
        this.minimumScore = minimumScore;
        return this;
    }

    public TreeBuilder updatable(boolean updatable) {
        this.updatable = updatable;
        return this;
    }

    @Override
    public Tree buildPredictiveModel(final Iterable<? extends AbstractInstance> trainingData) {
        return new Tree(buildTree(null, trainingData, 0, createNumericSplits(trainingData)));
    }

    public void updatePredictiveModel(Tree tree, final Iterable<? extends AbstractInstance> newData, List<? extends AbstractInstance> trainingData, boolean splitNodes) {
        //first move all the data into the leaves
        for(AbstractInstance instance : newData) {
            addInstanceToNode(tree.node, instance);
        }
        //now split the leaves further if possible
        if (splitNodes) {
            splitNode(tree.node, trainingData);
        }
    }

    public void stripData(Tree tree) {
        stripNode(tree.node);
    }


    private double[] createNumericSplit(final Iterable<? extends AbstractInstance> trainingData, final String attribute) {
        final ReservoirSampler<Double> reservoirSampler = new ReservoirSampler<Double>(RESERVOIR_SIZE);
        for (final AbstractInstance instance : trainingData) {
            Serializable value = instance.getAttributes().get(attribute);
            if (value == null) value = 0;
            reservoirSampler.sample(((Number) value).doubleValue());
        }

        return getSplit(reservoirSampler);
    }

    private Map<String, double[]> createNumericSplits(final Iterable<? extends AbstractInstance> trainingData) {
        final Map<String, ReservoirSampler<Double>> rsm = Maps.newHashMap();
        for (final AbstractInstance instance : trainingData) {
            for (final Entry<String, Serializable> attributeEntry : instance.getAttributes().entrySet()) {
                if (attributeEntry.getValue() instanceof Number) {
                    ReservoirSampler<Double> reservoirSampler = rsm.get(attributeEntry.getKey());
                    if (reservoirSampler == null) {
                        reservoirSampler = new ReservoirSampler<Double>(RESERVOIR_SIZE);
                        rsm.put(attributeEntry.getKey(), reservoirSampler);
                    }
                    reservoirSampler.sample(((Number) attributeEntry.getValue()).doubleValue());
                }
            }
        }

        final Map<String, double[]> splits = Maps.newHashMap();

        for (final Entry<String, ReservoirSampler<Double>> e : rsm.entrySet()) {
            final double[] split = getSplit(e.getValue());
            splits.put(e.getKey(), split);
        }
        return splits;
    }

    private double[] getSplit(ReservoirSampler<Double> reservoirSampler) {
        final ArrayList<Double> splitList = Lists.newArrayList();
        for (final Double sample : reservoirSampler.getSamples()) {
            splitList.add(sample);
        }
        if (splitList.isEmpty()) {
            throw new RuntimeException("Split list empty");
        }
        Collections.sort(splitList);

        final double[] split = new double[ORDINAL_TEST_SPLITS - 1];
        final int indexMultiplier = splitList.size() / (split.length + 2);
        for (int x = 0; x < split.length; x++) {
            split[x] = splitList.get((x + 1) * indexMultiplier);
        }
        return split;
    }

    private Node buildTree(Node parent, final Iterable<? extends AbstractInstance> trainingData, final int depth,
                             final Map<String, double[]> splits) {
        Preconditions.checkArgument(!Iterables.isEmpty(trainingData), "At Depth: " + depth + ". Can't build a tree with no training data");
        final Leaf thisLeaf;
        if (updatable) {
            thisLeaf = new UpdatableLeaf(parent, trainingData, depth);
        } else {
            thisLeaf = new Leaf(parent, trainingData, depth);
        }

        if (depth >= maxDepth) {
            return thisLeaf;
        }

        Pair<? extends Branch, Double> bestPair = getBestNodePair(parent, trainingData, splits);
        Branch bestNode = bestPair != null ? bestPair.getValue0() : null;
        double bestScore = bestPair != null ? bestPair.getValue1() : 0;

        // If we were unable to find a useful branch, return the leaf
        if (bestNode == null || bestScore < minimumScore) {
            // Its a bad sign when this happens, normally something to debug
            return thisLeaf;
        }

        final LinkedList<? extends AbstractInstance> trueTrainingSet = Lists.newLinkedList(Iterables.filter(trainingData, bestNode.getInPredicate()));
        if (trueTrainingSet.size() < this.minLeafInstances) {
            return thisLeaf;
        }

        final LinkedList<? extends AbstractInstance> falseTrainingSet = Lists.newLinkedList(Iterables.filter(trainingData, bestNode.getOutPredicate()));
        if (falseTrainingSet.size() < this.minLeafInstances) {
            return thisLeaf;
        }

        double trueWeight = getTotalWeight(trueTrainingSet);
        double falseWeight = getTotalWeight(falseTrainingSet);
        if (trueWeight == 0 || falseWeight ==0) {
            return thisLeaf;
        }

        double[] oldSplit = null;
        // We want to temporarily replace the split for an attribute for
        // descendants of an numeric branch, first the true split
        if (bestNode instanceof NumericBranch) {
            final NumericBranch bestBranch = (NumericBranch) bestNode;
            oldSplit = splits.get(bestBranch.attribute);
            splits.put(bestBranch.attribute, createNumericSplit(trueTrainingSet, bestBranch.attribute));
        }

        // Recurse down the true branch
        bestNode.trueChild = buildTree(bestNode, trueTrainingSet, depth + 1, splits);

        // And now replace the old split if this is an NumericBranch
        if (bestNode instanceof NumericBranch) {
            final NumericBranch bestBranch = (NumericBranch) bestNode;
            splits.put(bestBranch.attribute, createNumericSplit(falseTrainingSet, bestBranch.attribute));
        }

        // Recurse down the false branch
        bestNode.falseChild = buildTree(bestNode, falseTrainingSet, depth + 1, splits);

        // And now replace the original split if this is an NumericBranch
        if (bestNode instanceof NumericBranch) {
            final NumericBranch bestBranch = (NumericBranch) bestNode;
            splits.put(bestBranch.attribute, oldSplit);
        }

        return bestNode;
    }

    private Pair<? extends Branch, Double> getBestNodePair(Node parent, final Iterable<? extends AbstractInstance> trainingData, final Map<String, double[]> splits) {
        //should not be doing the following operation every time we call buildTree
        Map<String, AttributeCharacteristics> attributeCharacteristics = surveyTrainingData(trainingData);

        boolean smallTrainingSet = isSmallTrainingSet(trainingData);
        Pair<? extends Branch, Double> bestPair = null;



        for (final Entry<String, AttributeCharacteristics> attributeCharacteristicsEntry : attributeCharacteristics.entrySet()) {
            if (this.ignoreAttributeAtNodeProbability > 0 && Misc.random.nextDouble() < this.ignoreAttributeAtNodeProbability) {
                continue;
            }

            Pair<? extends Branch, Double> thisPair = null;
            Pair<? extends Branch, Double> numericPair = null;
            Pair<? extends Branch, Double> categoricalPair = null;

            if (!smallTrainingSet && attributeCharacteristicsEntry.getValue().isNumber) {
                numericPair = createNumericNode(parent, attributeCharacteristicsEntry.getKey(), trainingData, splits.get(attributeCharacteristicsEntry.getKey()));
            }
            categoricalPair = createCategoricalNode(parent, attributeCharacteristicsEntry.getKey(), trainingData);

            if (numericPair == null) {
                thisPair = categoricalPair;
            }  else {
                thisPair = (numericPair.getValue1() > categoricalPair.getValue1()) ? numericPair : categoricalPair;
            }
            if (bestPair == null || thisPair.getValue1() > bestPair.getValue1()) {
                bestPair = thisPair;
            }
        }
        return bestPair;
    }

    private double getTotalWeight(List<? extends AbstractInstance> trainingSet) {
        double trueWeight = 0;
        for (AbstractInstance instance : trainingSet) {
            trueWeight += instance.getWeight();
        }
        return trueWeight;
    }

    private boolean isSmallTrainingSet(Iterable<? extends AbstractInstance> trainingData) {
        boolean smallTrainingSet = true;
        int tsCount = 0;
        for (final AbstractInstance abstractInstance : trainingData) {
            tsCount++;
            if (tsCount > SMALL_TRAINING_SET_LIMIT) {
                smallTrainingSet = false;
                break;
            }
        }
        return smallTrainingSet;
    }

    private Map<String, AttributeCharacteristics> surveyTrainingData(final Iterable<? extends AbstractInstance> trainingData) {
        //tells us if each attribute is numeric or not.
        Map<String, AttributeCharacteristics> attributeCharacteristics = Maps.newHashMap();

        for (AbstractInstance instance : trainingData) {
            for (Entry<String, Serializable> e : instance.getAttributes().entrySet()) {
                AttributeCharacteristics attributeCharacteristic = attributeCharacteristics.get(e.getKey());
                if (attributeCharacteristic == null) {
                    attributeCharacteristic = new AttributeCharacteristics();
                    attributeCharacteristics.put(e.getKey(), attributeCharacteristic);
                }
                if (!(e.getValue() instanceof Number)) {
                    attributeCharacteristic.isNumber = false;
                }
            }
        }
        return attributeCharacteristics;
    }

    private Pair<? extends Branch, Double> createCategoricalNode(Node parent, final String attribute,
                                                               final Iterable<? extends AbstractInstance> instances) {
        final Set<Serializable> values = Sets.newHashSet();
        for (final AbstractInstance instance : instances) {
            Serializable value = instance.getAttributes().get(attribute);
            if (value == null) value = Double.MIN_VALUE;
            values.add(value);
        }
        double score = 0;
        final Set<Serializable> bestSoFar = Sets.newHashSet(); //the in-set

        ClassificationCounter inCounts = new ClassificationCounter(); //the histogram of counts by classification for the in-set
        final Pair<ClassificationCounter, Map<Serializable, ClassificationCounter>> valueOutcomeCountsPair = ClassificationCounter
                .countAllByAttributeValues(instances, attribute);
        ClassificationCounter outCounts = valueOutcomeCountsPair.getValue0(); //classification counter treating all values the same
        final Map<Serializable, ClassificationCounter> valueOutcomeCounts = valueOutcomeCountsPair.getValue1(); //map of value _> classificationCounter

        while (true) {
            double bestScore = 0;
            Serializable bestVal = null;
            for (final Serializable testVal : values) {
                final ClassificationCounter testValCounts = valueOutcomeCounts.get(testVal);
                if (testValCounts == null) { // Also a kludge, figure out why
                    // this would happen
                    //  .countAllByAttributeValues has a bug...or there is an issue with negative weights
                    continue;
                }
                if (this.minCategoricalAttributeValueOccurances > 0) {
                    if (shouldWeIgnoreThisValue(testValCounts)) continue;
                }
                final ClassificationCounter testInCounts = inCounts.add(testValCounts);
                final ClassificationCounter testOutCounts = outCounts.subtract(testValCounts);

                final double thisScore = scorer.scoreSplit(testInCounts, testOutCounts);

                if (thisScore > bestScore) {
                    bestScore = thisScore;
                    bestVal = testVal;
                }
            }
            if (bestScore > score) {
                score = bestScore;
                bestSoFar.add(bestVal);
                values.remove(bestVal);
                final ClassificationCounter bestValOutcomeCounts = valueOutcomeCounts.get(bestVal);
                inCounts = inCounts.add(bestValOutcomeCounts);
                outCounts = outCounts.subtract(bestValOutcomeCounts);
            } else {
                break;
            }
        }
        return Pair.with(new CategoricalBranch(parent, attribute, bestSoFar), score);
    }

    private boolean shouldWeIgnoreThisValue(final ClassificationCounter testValCounts) {
        double lowestClassificationCount = Double.MAX_VALUE;
        for (double classificationCount : testValCounts.getCounts().values()) {
            if (classificationCount < lowestClassificationCount) {
                lowestClassificationCount = classificationCount;
            }
        }
        return lowestClassificationCount < minCategoricalAttributeValueOccurances;
    }

    private Pair<? extends Branch, Double> createNumericNode(Node parent, final String attribute,
                                                               final Iterable<? extends AbstractInstance> instances,
                                                               final double[] splits) {
        double bestScore = 0;
        double bestThreshold = 0;

        double lastThreshold = Double.MIN_VALUE;
        for (final double threshold : splits) {
            // Sometimes we can get a few thresholds the same, avoid wasted
            // effort when we do
            if (threshold == lastThreshold) {
                continue;
            }
            lastThreshold = threshold;
            final Iterable<? extends AbstractInstance> inSet = Iterables.filter(instances, new GreaterThanThresholdPredicate(attribute, threshold));
            final Iterable<? extends AbstractInstance> outSet = Iterables.filter(instances, new LessThanEqualThresholdPredicate(attribute, threshold));
            final ClassificationCounter inClassificationCounts = ClassificationCounter.countAll(inSet);
            final ClassificationCounter outClassificationCounts = ClassificationCounter.countAll(outSet);

            final double thisScore = scorer.scoreSplit(inClassificationCounts, outClassificationCounts);

            if (thisScore > bestScore) {
                bestScore = thisScore;
                bestThreshold = threshold;
            }
        }
        return Pair.with(new NumericBranch(parent, attribute, bestThreshold), bestScore);
    }

    /**
     * Iterate through tree until we get to a leaf. Using the training data indexes in the leaf and the training data
     * provided build a tree from the leaf if possible. If a branch has only leaves as direct children, this will combine the data from the leaves
     * and recreate the branch
     * @param node The node we are attempting to further split
     */
    private void splitNode(Node node, List<? extends AbstractInstance> trainingData) {
        if (node instanceof UpdatableLeaf) {
            UpdatableLeaf leaf = (UpdatableLeaf) node;
            if (leaf.parent != null) {
                Branch branch = (Branch) leaf.parent;
                Branch parent;
                Node toReplace;
                //determine if we are combining leaves and will be replacing the parent branch or if we are replacing just this leaf
                if (shouldCombineData(branch)) {
                    parent = (Branch) branch.parent;
                    toReplace = branch;
                } else {
                    parent = branch;
                    toReplace = leaf;
                }
                Collection<AbstractInstance> leafData = getData(toReplace, trainingData);
                Node newNode = buildTree(parent, leafData, leaf.depth, createNumericSplits(leafData));
                if(parent.trueChild == toReplace) {
                    parent.trueChild = newNode;
                } else {
                    parent.falseChild = newNode;
                }
            }
        } else if (node instanceof Branch) {
            Branch branch = (Branch) node;
            splitNode(branch.trueChild, trainingData);
            //only split false child if we aren't combining leaves
            if (!shouldCombineData(branch)) {
                splitNode(branch.falseChild, trainingData);
            }

        }
    }

    private boolean shouldCombineData(Branch branch) {
        return branch.trueChild instanceof UpdatableLeaf && branch.falseChild instanceof UpdatableLeaf  && branch.parent!=null;
    }

    /**
     * @param node a branch with UpdatableLeaf children or an UpdatableLeaf
     * @param trainingData full set of trainingData
     */
    private Collection<AbstractInstance> getData(Node node, List<? extends AbstractInstance> trainingData) {
        List<AbstractInstance> data = Lists.newArrayList();
        Collection<Integer> indexes = getIndexes(node);

        for(Integer index : indexes) {
            data.add(trainingData.get(index));
        }
        return data;
    }

    private Collection<Integer> getIndexes(Node node) {
        Collection<Integer> indexes = Collections.EMPTY_LIST;
        if (node instanceof UpdatableLeaf) {
            indexes = (((UpdatableLeaf) node).trainingDataIndexes);
        } else if (node instanceof Branch) {
            Branch branch = (Branch) node;
            indexes = ((UpdatableLeaf) branch.trueChild).trainingDataIndexes;
            indexes.addAll(((UpdatableLeaf) branch.falseChild).trainingDataIndexes);
        }
        return indexes;
    }

    private void addInstanceToNode(Node node, AbstractInstance instance) {
        if (node instanceof UpdatableLeaf) {
            UpdatableLeaf leaf = (UpdatableLeaf) node;
            leaf.addInstance(instance);
        } else if (node instanceof Branch) {
            Branch branch = (Branch) node;
            if (branch.getInPredicate().apply(instance)) {
                addInstanceToNode(branch.trueChild, instance);
            } else {
                addInstanceToNode(branch.falseChild, instance);
            }
        }
    }

    private void stripNode(Node node) {
        if (node instanceof UpdatableLeaf) {
            UpdatableLeaf leaf = (UpdatableLeaf) node;
            Branch branch = (Branch) leaf.parent;
            Leaf newLeaf = new Leaf(leaf.parent, leaf.classificationCounts, leaf.depth);
            if(branch.trueChild == node) {
                branch.trueChild = newLeaf;
            } else {
                branch.falseChild = newLeaf;
            }
        } else if (node instanceof Branch) {
            Branch branch = (Branch) node;
            stripNode(branch.trueChild);
            stripNode(branch.falseChild);
        }
    }

    public static class AttributeCharacteristics {
        public boolean isNumber = true;
    }

    private class GreaterThanThresholdPredicate implements Predicate<AbstractInstance> {

        private final String attribute;
        private final double threshold;

        public GreaterThanThresholdPredicate(String attribute, double threshold) {
            this.attribute = attribute;
            this.threshold = threshold;
        }

        @Override
        public boolean apply(@Nullable AbstractInstance input) {
            try {
                Serializable value = input.getAttributes().get(attribute);
                if (value == null) value = 0;
                return input != null && ((Number) value).doubleValue() > threshold;
            } catch (final ClassCastException e) { // Kludge, need to
                // handle better
                return false;
            }
        }
    }

    private class LessThanEqualThresholdPredicate implements Predicate<AbstractInstance> {

        private final String attribute;
        private final double threshold;

        public LessThanEqualThresholdPredicate(String attribute, double threshold) {
            this.attribute = attribute;
            this.threshold = threshold;
        }

        @Override
        public boolean apply(@Nullable AbstractInstance input) {
            try {
                Serializable value = input.getAttributes().get(attribute);
                if (value == null) value = 0;
                return input != null && ((Number) value).doubleValue() <= threshold;
            } catch (final ClassCastException e) { // Kludge, need to
                // handle better
                return false;
            }
        }
    }

}
