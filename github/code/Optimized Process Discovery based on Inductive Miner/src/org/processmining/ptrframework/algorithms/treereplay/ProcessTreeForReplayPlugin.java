package org.processmining.ptrframework.algorithms.treereplay;

import org.deckfour.xes.model.XLog;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptrframework.algorithms.treereplay.NodeType.ProcessTreeNode;
import org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach.ProcessTreeParser;
import org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach.ReplayProcessTree;
import org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach.SGASearchResult;
import org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach.SearchGraphPT;
import org.processmining.ptrframework.utils.Pair;
import org.processmining.ptrframework.utils.TraceUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessTreeForReplayPlugin {
    private static lastSettings lastSettings;

    public void computeAndStoreAlignmentPerNode(ProcessTree inputTree, XLog inputLog) {
        if (lastSettings == null || lastSettings.tree != inputTree) {
            lastSettings = new lastSettings();
        }

        if (lastSettings.tree == null) {
            ProcessTree copy = preProcessTree(inputTree);
//            ProcessTreeImpl copy = new ProcessTreeImpl(inputTree);

            Map<List<String>, Long> traceToCount = inputLog.stream().collect(Collectors.groupingBy(TraceUtils::traceToStringList, Collectors.counting()));
            SearchGraphPT searchGraphPT = new SearchGraphPT();
            ProcessTreeParser processTreeParser = new ProcessTreeParser();
            ReplayProcessTree pt = processTreeParser.parseAndTranslateProcessTree(copy);
            
            System.out.println("inputTree:"+inputTree);
            System.out.println("ReplayProcessTree:"+pt);
            HashMap<ReplayProcessTree, Node> replayProcessTreeToOriginalNode = processTreeParser.getReplayProcessTreeToOriginalNode();

            HashMap<List<String>, SGASearchResult> listSGASearchResultHashMap = searchGraphPT.applyToVariantStrings(traceToCount.keySet(), pt);

System.out.println("listSGASearchResultHashMap:"+listSGASearchResultHashMap);
            ProcessTreeForReplayParser processTreeForReplayParser = new ProcessTreeForReplayParser();
            Pair<ProcessTreeNode, HashMap<Node, ProcessTreeNode>> pair = processTreeForReplayParser.translate(copy);
            System.out.println("pair:"+pair);
            ProcessTreeNode rootForReplay = pair.getKey();
            HashMap<Node, ProcessTreeNode> replayTranslation = pair.getValue();
            System.out.println("replayTranslation:"+replayTranslation);
            
            System.out.println(listSGASearchResultHashMap.keySet());
            System.out.println(listSGASearchResultHashMap.values().toArray());
            System.out.println(listSGASearchResultHashMap.values().toString());
            for (SGASearchResult value : listSGASearchResultHashMap.values()) {
//            	System.out.println("value:"+value);
                for (Pair<Object, Object> objectObjectPair : value.getAlignment()) {
                	System.out.println("速度快你发咯工科男设定"+objectObjectPair);
                    Object node = objectObjectPair.getValue();
                    if (node instanceof ReplayProcessTree) {
                    	System.out.println("node:"+node);
//                    	ProcessTreeNode nodeToFire=replayTranslation.get(replayProcessTreeToOriginalNode.get(node));
                        ProcessTreeNode nodeToFire = replayTranslation.get(replayProcessTreeToOriginalNode.get(node));
//                        System.out.println("nodeToFire:"+nodeToFire.getLocalLog());
                        nodeToFire.updateParent(null, null, false, false, false);
                    }
                }

                rootForReplay.closeAll();
            }

            lastSettings.tree = inputTree;
            lastSettings.replayTranslation = replayTranslation;
        }
    }

    public XLog getLogOfNode(Node chosenNode) {
        if (lastSettings == null || lastSettings.replayTranslation == null || !lastSettings.replayTranslation.containsKey(chosenNode)) {
            return null;
        }
//打印lastSettings.replayTranslation.get(chosenNode)
//        System.out.println(lastSettings.replayTranslation.get(chosenNode));
        return lastSettings.replayTranslation.get(chosenNode).getLocalLog();
    }

    private ProcessTree preProcessTree(ProcessTree tree) {
        ProcessTreeImpl copy = new ProcessTreeImpl(tree);

        LinkedList<LinkedList<Node>> nodesPerLevel = new LinkedList<>();

        LinkedList<Node> currLevel = new LinkedList<>();
        currLevel.add(copy.getRoot());
        while (!currLevel.isEmpty()) {
            nodesPerLevel.add(currLevel);
            currLevel = new LinkedList<>();

            for (Node node : nodesPerLevel.getLast()) {
                if (node instanceof Block) {
                    currLevel.addAll(((Block) node).getChildren());
                }
            }
        }

        while (!nodesPerLevel.isEmpty()) {
            for (Node node : nodesPerLevel.removeLast()) {
                if (node instanceof Block) {
                    List<String> name = new LinkedList<>();
                    ((Block) node).getChildren().forEach(node1 -> {
                        if (node1 instanceof Task.Automatic) {
                            name.add("tau");
                        } else {
                            name.add(node1.getName());
                        }
                    });

                    node.setName(name.toString());
                }
            }
        }

        return copy;
    }

    public void wipeLastSettings() {
        if (lastSettings != null) {
            lastSettings = null;
        }
    }

    private static class lastSettings {
        private ProcessTree tree;
        private HashMap<Node, ProcessTreeNode> replayTranslation;

        public lastSettings() {
        }
    }
}
