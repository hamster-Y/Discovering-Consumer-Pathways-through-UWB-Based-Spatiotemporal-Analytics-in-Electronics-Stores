package org.processmining.ptrframework.algorithms.treereplay.NodeType;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.processtree.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public abstract class ProcessTreeNode {
    ProcessTreeNode parent;
    Collection<ProcessTreeNode> openChildren;
    LinkedList<ProcessTreeNode> children;
    XFactory xFactory;
    XLog localLog;
    XTrace localTrace;
    Node correspondingNode;


    public ProcessTreeNode(ProcessTreeNode parent, XFactory xFactory, Node correspondingNode) {
        this.parent = parent;
        openChildren = new HashSet<>();
        children = new LinkedList<>();
        this.xFactory = xFactory;
        localLog = xFactory.createLog();
        localTrace = xFactory.createTrace();
        this.correspondingNode = correspondingNode;
    }

    public abstract void updateParent(ProcessTreeNode childFired, XEvent eventCreated, boolean closed, boolean includeSilent, boolean isSilent);

    public void addChild(ProcessTreeNode child) {
        children.add(child);
    }

    public void closeAll() {
        if (!localTrace.isEmpty()) {
            localLog.add(localTrace);
            localTrace = xFactory.createTrace();
        }
        children.forEach(ProcessTreeNode::closeAll);
    }

    public XLog getLocalLog() {
//        HashMap<List<String>, Integer> variant2Frequency = new HashMap<>();
//        List<List<String>> tmpTraceSequence = new ArrayList<>(localLog.size());
//        for (XTrace trace : localLog) {
//            List<String> eventList = new ArrayList<>(trace.size());
//            for (XEvent event : trace) {
//                String eventName = event.getAttributes().get("concept:name").toString();
//                eventList.add(eventName);
//            }
//            // record trace
//            tmpTraceSequence.add(eventList);
//
//            if (variant2Frequency.containsKey(eventList)) {
//                variant2Frequency.put(eventList, variant2Frequency.get(eventList) + 1);
//            } else {
//                variant2Frequency.put(eventList, 1);
//            }
//        }
//
////        variant2Frequency = variant2Frequency;
////        tmpTraceSequence = tmpTraceSequence;
//        System.out.print("子日志轨迹及频率:"+variant2Frequency);
//        System.out.println(correspondingNode);
        
        return localLog;
    }
}
