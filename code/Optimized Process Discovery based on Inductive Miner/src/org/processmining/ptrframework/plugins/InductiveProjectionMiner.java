package org.processmining.ptrframework.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IM;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
//import org.processmining.processtree.conversion.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.processmining.ptrframework.algorithms.treereplay.ProcessTreeForReplayPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptrframework.utils.LogUtils;
import org.processmining.ptrframework.utils.ProcessTreeModifier;
import org.processmining.ptrframework.utils.ProcessTreeUtils;

import nl.tue.astar.AStarException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;
import java.util.List;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.etconformance.ETCPlugin;
import org.processmining.plugins.etconformance.ETCResults;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import software.processmining.interfacediscoveryevaluation.InterfaceModelComplexityEvaluationPlugin;
//Process Tree
//returnLabels = {"Petri net","Initial Marking","Final Marking"}
@Plugin(name = "Optimized Process Discovery based on Inductive Miner", userAccessible = true,parameterLabels = {"Log"}, returnLabels = {"Petri net","Initial Marking","Final Marking"}, returnTypes = {Petrinet.class,Marking.class,Marking.class}, categories = {PluginCategory.Discovery, PluginCategory.Enhancement}, keywords = {"Inductive Miner", "ProjectionMiner", "IM", "Precision Improvement"}, help = "Applies the inductive miner to imprecise structures within the process tree.")
public class InductiveProjectionMiner {
    ArrayList<Node> replacementCandidates;

    @PluginVariant(requiredParameterLabels = {0})
    @UITopiaVariant(affiliation = "PADS", author = "Jiaxin Yan", email = "yanjiaxinchina@163.com")
    
    
    public Object[] discover(UIPluginContext context, XLog log) throws ProcessTree2Petrinet.InvalidProcessTreeException, ProcessTree2Petrinet.NotYetImplementedException, ConnectionCannotBeObtained, AStarException {
        long startTime=System.currentTimeMillis();
    	replacementCandidates = new ArrayList<>();
		IMMiningDialog dialog = new IMMiningDialog(log);
		InteractionResult result = context.showWizard("Mine using Inductive Miner", true, true, dialog);
		if (result != InteractionResult.FINISHED || !confirmLargeLogs(context, log, dialog)) {
			context.getFutureResult(0).cancel(false);
			return null;
		}
//
		context.log("Mining...");
		

        
//        ProcessTreeForReplayPlugin plugin = new ProcessTreeForReplayPlugin();
//        plugin.wipeLastSettings();
//        plugin.computeAndStoreAlignmentPerNode(copyTree, log);
//        MiningParameters miningParametersIMf = new MiningParametersIMf();
//        miningParametersIMf.setNoiseThreshold((float)slider.getValue());
//        XLog logOfNode =  plugin.getLogOfNode(toBeReplaced);  
//        Iterator<XTrace> a=logOfNode.iterator();
//        System.out.println(a.next());
//        ProcessTree inputTree=IM.mineProcessTree(context, log, miningParametersIMf);

		ProcessTree inputTree= IMProcessTree.mineProcessTree(log, dialog.getMiningParameters(), new Canceller() {
			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		});
		long endTime=System.currentTimeMillis();
		long executionTime=endTime-startTime;
		System.out.println("***************ʱ��:"+executionTime);
        return discoverProjective(inputTree,context, log);
    }
    
	//ȫ�ִ�����,�ռ�����flower����������ϳ��������������������ļ���
	public Object[] discoverProjective(ProcessTree inputTree,UIPluginContext context, XLog log) throws InvalidProcessTreeException, NotYetImplementedException, ConnectionCannotBeObtained, AStarException {
		long startTime=System.currentTimeMillis();
		ProcessTree copyTree = inputTree;
		System.out.println("inputTree:"+copyTree);
		
        ProcessTreeModifier treeModifier = new ProcessTreeModifier();
        copyTree = treeModifier.apply(copyTree);
        findCandidatesForReplacement(copyTree);
//        ProcessTreeForReplayPlugin plugin = new ProcessTreeForReplayPlugin();
//        plugin.wipeLastSettings();
//        plugin.computeAndStoreAlignmentPerNode(copyTree, log);
		
		Map<Block,List<ProcessTree>> Tree_Map = new HashMap<>();
		if (replacementCandidates.isEmpty()) {
			System.out.println("replacementCandidates�ǿյ�");
		}else {
			System.out.println("replacementCandidates�ĳ�����:"+replacementCandidates.size()+"������:"+replacementCandidates);
		}

				
		System.out.println("Tree_Map:"+Tree_Map);
		long endTime=System.currentTimeMillis();
		long executionTime=endTime-startTime;
		System.out.println("***************ʱ��:"+executionTime);
	    // ���ع����õ�ӳ���ϵ
	    return generateAllCombinations(context,copyTree,Tree_Map,log);
	}
	

	public Object[] generateAllCombinations(UIPluginContext context,ProcessTree copyTree, Map<Block, List<ProcessTree>> processTree_Map,XLog log) throws NotYetImplementedException, InvalidProcessTreeException, ConnectionCannotBeObtained, AStarException {
		long startTime=System.currentTimeMillis();
		List<ProcessTree> allNewTrees = new ArrayList<>();
	    Map<Block, ProcessTree> currentSubstitutions=new HashMap<>();
	    // ʹ�õݹ�������ÿ��flower�ڵ�������������///////////////////////////////
	    generateCombination(log,context,replacementCandidates,copyTree, allNewTrees);
	    long endTime=System.currentTimeMillis();
		long executionTime=endTime-startTime;
		System.out.println("***************ʱ��:"+executionTime);
	    System.out.println("allNewTrees:"+allNewTrees);
	    if(allNewTrees.isEmpty()) {
//	    System.out.println("�յ�");
	    PetrinetWithMarkings result = ProcessTree2Petrinet.convert(copyTree);
		context.addConnection(new InitialMarkingConnection(result.petrinet, result.initialMarking));
		context.addConnection(new FinalMarkingConnection(result.petrinet, result.finalMarking));
		return new Object[] { result.petrinet, result.initialMarking, result.finalMarking };
	    }else {
	    System.out.print("����");
	    System.out.println(allNewTrees.size());
	    ProcessTree finaltree=allNewTrees.get(0);
	    PetrinetWithMarkings  result666 = ProcessTree2Petrinet.convert(finaltree);
	    double CardosoValue1=InterfaceModelComplexityEvaluationPlugin.computeCardoso(context, result666.petrinet, result666.initialMarking);
		double CyclomaticValue1=InterfaceModelComplexityEvaluationPlugin.computeCyclomatic(context, result666.petrinet, result666.initialMarking);
		double complexity1 = (2*CardosoValue1*CyclomaticValue1)/(CardosoValue1+CyclomaticValue1);
		System.out.println("���ƶ�"+complexity1);
	    double F_Score=0.0;
	    double finalprecision=0.0;
	    double finalfitness=0.0;
//	    PetrinetWithMarkings result = ProcessTree2Petrinet.convert(allNewTrees.get(0));
	    for (ProcessTree tree : allNewTrees) {
	    	System.out.println("������:"+tree);
	    	PetrinetWithMarkings  result = ProcessTree2Petrinet.convert(tree);
			context.addConnection(new InitialMarkingConnection(result.petrinet, result.initialMarking));
			context.addConnection(new FinalMarkingConnection(result.petrinet, result.finalMarking));
			//Fitnessֵ
			PNLogReplayer replayer = new PNLogReplayer();

			PNRepResult result1 = replayer.replayLog(context, result.petrinet, log);
	    	// �� Collection ת��Ϊ List
	    	List<Object> valuesList = new ArrayList<>(result1.getInfo().values());
	    	System.out.println("fitness��Ϣ"+valuesList);
	    	// ��ȡָ��λ�õ�Ԫ�أ�����Ҫ��ȡ������λ�õ�Ԫ�أ�����Ϊ 2,��FITNESSֵ
	    	Object Fitness = valuesList.get(2);
	    	// ��ȡfitnessֵ
	    	System.out.println("Fitness: " + ((double)Fitness)+0);
//	    	System.out.println(result1.getInfo().values().getClass());

	    	//Precisionֵ
	    	ETCPlugin etcplugin = new ETCPlugin();
	    	Object[] result2=etcplugin.doETC(context, log, result.petrinet);
	    	ETCResults Precision = (ETCResults) result2[0];
	        
	        // ��ȡ etcp �ֶε�ֵ
	        double etcpValue = Precision.getEtcp();
	    	System.out.println("Preision:"+etcpValue+1);
 	        if ((2 * (double)Fitness*etcpValue / ((double)Fitness + etcpValue)) >F_Score){
 	        	finalprecision=etcpValue;
 	        	finalfitness=(double)Fitness;
 	        	finaltree=tree;
 	        	F_Score=2 * (double)Fitness*etcpValue / ((double)Fitness + etcpValue);
 	        }
	    }
	    
	    PetrinetWithMarkings finalresult = ProcessTree2Petrinet.convert(finaltree);
		context.addConnection(new InitialMarkingConnection(finalresult.petrinet, finalresult.initialMarking));
		context.addConnection(new FinalMarkingConnection(finalresult.petrinet, finalresult.finalMarking));
		double CardosoValue=InterfaceModelComplexityEvaluationPlugin.computeCardoso(context, finalresult.petrinet, finalresult.initialMarking);
		double CyclomaticValue=InterfaceModelComplexityEvaluationPlugin.computeCyclomatic(context, finalresult.petrinet, finalresult.initialMarking);
		double complexity = (2*CardosoValue*CyclomaticValue)/(CardosoValue+CyclomaticValue);
		System.out.println("Fֵ:"+F_Score+"---Fitness:"+finalfitness+"---Precision:"+finalprecision+"---Complexity:"+complexity+"---�ִ�:"+allNewTrees.size());
		JFrame frame = new JFrame("Model Information");
		frame.setSize(300, 159);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null); // ����������Ϊ��Ļ����
		DecimalFormat decimalFormat = new DecimalFormat("#.#####");
		// ����һ����ά�������洢����
		String[][] data = {
		    {"Fitness", decimalFormat.format(finalfitness)},
		    {"Precision", decimalFormat.format(finalprecision)},
		    {"F-Score", decimalFormat.format(F_Score)},
		    {"Complexity", decimalFormat.format(complexity)},
		    {"Number of screenings", decimalFormat.format(allNewTrees.size())}
		};

		// ����һ���ַ������飬���ڶ����������
		String[] columnNames = {"Property", "Value"};

		// �������
		JTable table = new JTable(data, columnNames);

		// ���ñ�ͷ����
		Font headerFont = new Font("Arial", Font.BOLD, 18);
		table.getTableHeader().setFont(headerFont);

		// ���ñ����������
		Font contentFont = new Font("Arial", Font.PLAIN, 15);
		table.setFont(contentFont);


		// ����ѡ����������ɫ
		table.setSelectionForeground(Color.WHITE);

		// ������������ɫ
		table.setGridColor(Color.GRAY);
		// ���ñ���е�ˮƽ��
		table.setShowHorizontalLines(false);

		// ���ñ��ı߿�
		table.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // ģ��ˮƽ��

		// �������ӵ�һ�����������
		JScrollPane scrollPane = new JScrollPane(table);
		frame.add(scrollPane);
		// ��ȡ��Ļ��С
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		// ���� JFrame ������
		int xCoordinate = screenSize.width - frame.getWidth();
		int yCoordinate = 130;

		// ���� JFrame ��λ��
		frame.setLocation(xCoordinate, yCoordinate);
		// ���� JFrame �Ĵ�С




		// ���� JFrame �Ŀɼ���
		frame.setVisible(true);


		return new Object[] { finalresult.petrinet, finalresult.initialMarking, finalresult.finalMarking };
	    }
	}


		
		//�ݹ麯�������������
	private void generateCombination(XLog log, UIPluginContext context, ArrayList<Node> replacementCandidates, ProcessTree currentCopy, List<ProcessTree> resultList) throws NotYetImplementedException, InvalidProcessTreeException {
		long startTime=System.currentTimeMillis();
		System.out.println("currentCopy:" + currentCopy);
	    if (replacementCandidates.isEmpty()) {
	        // ����flower�ڵ㶼�Ѿ��滻�ˣ���ӵ�����б���
	        resultList.add(currentCopy);
	        System.out.println("���н���滻���");
	        return;
	    }
	    System.out.println("�����滻������");
	    // ��ȡ��Ҫ�滻��flower�ڵ��б�ĵ�����
	    ListIterator<Node> iterator = replacementCandidates.listIterator();
//	    while (iterator.hasNext()) {
	        Node currentFlower = iterator.next();
	        System.out.println("flower:" + currentFlower);
	        
	        // �Ե�ǰflower�ڵ�����滻����
	        for (float noise = 0.2f; noise <= 0.6f; noise += 0.1f) {
	            ProcessTree treeCopy = new ProcessTreeImpl(currentCopy);
	            treeCopy = ProcessTreeUtils.replaceSubProcessTreeBySubProcessTree(currentCopy, log, context, treeCopy, currentFlower, noise);
	            System.out.println("treeCopy:" + treeCopy);
	            
	            // �ݹ�����Դ���ʣ���flower�ڵ�
	            ArrayList<Node> remainingCandidates = new ArrayList<>(replacementCandidates); // ���������Ա��Ⲣ���޸��쳣
	            remainingCandidates.remove(currentFlower); // �Ƴ���ǰ�����flower�ڵ�
	            generateCombination(log, context, remainingCandidates, treeCopy, resultList);
	        }
	        // �ڵ���������ɾ����ǰ����Ľڵ�
	        iterator.remove();
//	    }
	    
	    // �������б�
	    System.out.println("RESULTLIST:"+resultList.size());
	    for (ProcessTree tree : resultList) {
	        System.out.println("*******" + tree);
	    }
	    long endTime=System.currentTimeMillis();
		long executionTime=endTime-startTime;
		System.out.println("***************ʱ��:"+executionTime);
	}

	

	// add element into "replacementCandidates"
    private void findCandidatesForReplacement(ProcessTree tree) {
    	long startTime=System.currentTimeMillis();
    	tree.getNodes().stream().filter(node -> node.getName().equals(ProcessTreeModifier.getIdentifierName())).forEach(replacementCandidates::add);
        System.out.println("replacementCandidates����:"+replacementCandidates.size());
        long endTime=System.currentTimeMillis();
		long executionTime=endTime-startTime;
		System.out.println("***************ʱ��:"+executionTime);
    }
    
	public static boolean confirmLargeLogs(final UIPluginContext context, XLog log, IMMiningDialog dialog) {
		if (dialog.getVariant().getWarningThreshold() > 0) {
			XEventClassifier classifier = dialog.getMiningParameters().getClassifier();
			XLogInfo xLogInfo = XLogInfoFactory.createLogInfo(log, classifier);
			int numberOfActivities = xLogInfo.getEventClasses().size();
			if (numberOfActivities > dialog.getVariant().getWarningThreshold()) {
				int cResult = JOptionPane.showConfirmDialog(null,
						dialog.getVariant().toString() + " might take a long time, as the event log contains "
								+ numberOfActivities
								+ " activities.\nThe chosen variant of Inductive Miner is exponential in the number of activities.\nAre you sure you want to continue?",
						"Inductive Miner might take a while", JOptionPane.YES_NO_OPTION);

				return cResult == JOptionPane.YES_OPTION;
			}
		}
		return true;
	}
	


}
