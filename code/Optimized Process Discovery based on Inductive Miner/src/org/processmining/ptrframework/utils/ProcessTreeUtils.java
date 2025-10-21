package org.processmining.ptrframework.utils;

import org.apache.tools.ant.util.ProcessUtil;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IM;
import org.processmining.processtree.*;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.processmining.ptrframework.algorithms.treereplay.ProcessTreeForReplayPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.processmining.ptrframework.utils.LogUtils;
public class ProcessTreeUtils {

    public static boolean isAllFlowerTree(ProcessTree tree) {
        boolean rerun = true;
        ProcessTree copyTree = tree;

        if (tree.getRoot() instanceof Block) {
            while (rerun) {
                rerun = false;

                // Bring into normal form
                copyTree = new ProcessTreeImpl(copyTree);
                if (copyTree.getRoot() instanceof Block) {
                    Block copyRoot = (Block) copyTree.getRoot();
                    for (Node child : (copyRoot).getChildren()) {
                        if (canBeRolledUp(copyRoot, child)) {
                            rollUpBlock(copyTree, copyRoot, child);
                            rerun = true;
                        }
                    }
                }
            }
        }

        Node currNode = copyTree.getRoot();

        if (currNode instanceof Block.Xor && ((Block.Xor) currNode).numChildren() == 2 && ((Block.Xor) currNode).getChildren().get(0) instanceof Task.Automatic) {
            currNode = ((Block.Xor) currNode).getChildren().get(1);
        }

        if (currNode instanceof Block.And) {

            List<Node> children = ((Block.And) currNode).getChildren();

            for (Node child : children) {
                if (!(child instanceof Block.Xor)||!(child instanceof Block.XorLoop)) {//认证并发结点的子结点类型，并发节点的子节点只可以是选择∪循环结构
                    return false;
                }

                List<Node> childChildren = ((Block.Xor) child).getChildren();
//                if (childChildren.size() != 2 || !(childChildren.get(0) instanceof Task.Automatic) || !(childChildren.get(1) instanceof Block.XorLoop)) {
                if (childChildren.size() != 2 || !(childChildren.get(0) instanceof Task.Automatic) ) {
                    return false;
                }

//                List<Node> loopingChildren = ((Block.XorLoop) childChildren.get(1)).getChildren();
                List<Node> loopingChildren = ((Block.XorLoop) child).getChildren();
                if (!(loopingChildren.get(0) instanceof Task.Manual && loopingChildren.get(1) instanceof Task.Automatic && loopingChildren.get(2) instanceof Task.Automatic)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public static void cutSubProcessTreeByNode(ProcessTree tree, Node node) {
        ArrayList<Node> unvisitedNodes = new ArrayList<>();
        unvisitedNodes.add(node);

        while (!unvisitedNodes.isEmpty()) {
            ArrayList<Node> newUnvisitedNodes = new ArrayList<>();

            for (Node unvisitedNode : unvisitedNodes) {
                for (Edge incomingEdge : unvisitedNode.getIncomingEdges()) {
                    tree.removeEdge(incomingEdge);
                }

                if (unvisitedNode instanceof Block) {
                    newUnvisitedNodes.addAll(((Block) unvisitedNode).getChildren());
                }

                tree.removeNode(unvisitedNode);
            }

            unvisitedNodes = newUnvisitedNodes;
        }
//        return tree;
    }

    public static ProcessTree replaceSubProcessTreeBySubProcessTree(ProcessTree copyTree,XLog log,UIPluginContext context,ProcessTree tree, Node toBeReplaced,float noise) throws NotYetImplementedException, InvalidProcessTreeException {
//    	toBeReplaced=ProcessTreeUtils.cutSubProcessTreeByNode(tree, toBeReplaced).getRoot();
    	System.out.println("内层阈值:"+noise);
    	toBeReplaced=tree.getNode(toBeReplaced.getID());
        ProcessTreeForReplayPlugin plugin = new ProcessTreeForReplayPlugin();
        plugin.wipeLastSettings();
//        Map<List<String>, Long> traceToCount = log.stream().collect(Collectors.groupingBy(TraceUtils::traceToStringList, Collectors.counting()));
//        System.out.println(traceToCount);
        
        plugin.computeAndStoreAlignmentPerNode(copyTree, log);
        MiningParameters miningParametersIMf = new MiningParametersIMf();
        miningParametersIMf.setNoiseThreshold(noise);
        XLog logOfNode=null;
        if (tree.getRoot()==toBeReplaced) {
            logOfNode=plugin.getLogOfNode(tree.getRoot());
        }else {
        	logOfNode =  plugin.getLogOfNode(toBeReplaced);  
        }
        
        
//        PetrinetWithMarkings result = ProcessTree2Petrinet.convert(toBeReplaced.getProcessTree());
//		context.addConnection(new InitialMarkingConnection(result.petrinet, result.initialMarking));
//		context.addConnection(new FinalMarkingConnection(result.petrinet, result.finalMarking));
//		System.out.println(result.petrinet.getTransitions());
        
//        XLog logOfNode1=plugin.getLogOfNode(toBeReplaced);///////***************************
        LogUtils logUtils = new LogUtils(logOfNode);

        // 调用 generateVariant2Frequency 方法
        logUtils.generateVariant2Frequency();
        

        // 现在你可以使用 logUtils.getVariant2Frequency() 获取变体到频率的映射
        System.out.println("getVariant2Frequency"+logUtils.getVariant2Frequency());
//        System.out.println("getVariantSequence:"+logUtils.);
        
        System.out.println("getxLog"+logUtils.getxLog());
        ProcessTree replacingTree=IM.mineProcessTree(context, logOfNode, miningParametersIMf);
        System.out.println(logOfNode.size());
            
        if (tree.getRoot()==toBeReplaced) {
            return replacingTree;
        }

        for (Node node : replacingTree.getNodes()) {
            tree.addNode(node);
            node.setProcessTree(tree);
        }

        for (Edge edge : replacingTree.getEdges()) {
            tree.addEdge(edge);
        }

        ArrayList<Edge> edgesToBeRemoved = new ArrayList<>();
        for (Edge incomingEdge : toBeReplaced.getIncomingEdges()) {
        	// incomingEdge != replacingTree.getRoot() 导致的报错
            incomingEdge.setTarget(replacingTree.getRoot());
            edgesToBeRemoved.add(incomingEdge);
        }

        for (Edge edge : edgesToBeRemoved) {
            toBeReplaced.removeIncomingEdge(edge);
        }

        cutSubProcessTreeByNode(tree, toBeReplaced);

        Node currNode = replacingTree.getRoot();
        Block parent = getParent(currNode);
        while (canBeRolledUp(parent, currNode)) {
            rollUpBlock(tree, parent, currNode);

            currNode = parent;

            parent = getParent(currNode);
        }

        return tree;
    }

    public static Block getParent(Node node) {
        if (node.getIncomingEdges().isEmpty()) {
            return null;
        }

        return node.getParents().iterator().next();
    }

    public static boolean canBeRolledUp(Block parent, Node nodeToBeChecked) {
        if (parent == null) {
            return false;
        }

        if (!(parent instanceof Block.XorLoop) && parent.numChildren() == 1) {
            return true;
        }

        if (nodeToBeChecked instanceof AbstractBlock.XorLoop) {
            return false;
        }

        return parent.getClass() == nodeToBeChecked.getClass();
    }

    public static void rollUpBlock(ProcessTree tree, Block parent, Node nodeForRollUp) {
        int pos = 0;
        for (; pos < parent.numChildren(); pos++) {
            if (parent.getChildren().get(pos).equals(nodeForRollUp)) {
                break;
            }
        }

        if (nodeForRollUp instanceof Block) {
            Block blockForRollUp = (Block) nodeForRollUp;
            for (int i = 0; i < blockForRollUp.numChildren(); i++) {
                Edge newEdge = parent.addChildAt(blockForRollUp.getChildren().get(i), pos + i);
                tree.addEdge(newEdge);
                blockForRollUp.getChildren().get(i).addIncomingEdge(newEdge);
            }
        }

        removeNode(tree, nodeForRollUp);
    }

    public static void removeNode(ProcessTree tree, Node node) {
        node.getIncomingEdges().forEach(edge -> {
            tree.removeEdge(edge);
            edge.getSource().removeOutgoingEdge(edge);
        });

        if (node instanceof Block) {
            ((Block) node).getOutgoingEdges().forEach(edge -> {
                tree.removeEdge(edge);
                edge.getTarget().removeIncomingEdge(edge);
            });
        }

        tree.removeNode(node);
    }

}