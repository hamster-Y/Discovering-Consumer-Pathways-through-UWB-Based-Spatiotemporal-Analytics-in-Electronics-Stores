package org.processmining.ptrframework.utils;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

import software.processmining.interfacediscoveryevaluation.InterfaceModelComplexityEvaluationPlugin;


@Plugin(
		name = "111111111test Petri Net Complexity Plugin",// plugin name
		
		returnLabels = {"complexValue"}, //return labels
		returnTypes = {String.class},//return class
		
		//input parameter labels, corresponding with the second parameter of main function
		parameterLabels = { "petri", "inital marking" },
		userAccessible = true,
		help = "This plugin aims to sample an input large-scale example log and returns a small sample log by measuring the significance of traces." 
		)
public class testComplexityPetri {

	@UITopiaVariant(
	        affiliation = "TU/e", 
	        author = "Cong liu", 
	        email = "c.liu.3@tue.nl"
	        )
	@PluginVariant(
			variantLabel = "Merge two Event Log, default",
			// the number of required parameters, {0} means one input parameter
			requiredParameterLabels = {0,1}
			)
	public static String SimRankSamplingTechnique(UIPluginContext context,Petrinet net, Marking marking)
	{
		double CardosoValue=InterfaceModelComplexityEvaluationPlugin.computeCardoso(context, net, marking);
		double CyclomaticValue=InterfaceModelComplexityEvaluationPlugin.computeCyclomatic(context, net, marking);
		
		double complexity = (2*CardosoValue*CyclomaticValue)/(CardosoValue+CyclomaticValue);
		System.out.println("*********************");
		System.out.println("CardosoValue:"+CardosoValue);
		System.out.println("CyclomaticValue:"+CyclomaticValue);
		System.out.println("Complexity:"+complexity);
		System.out.println("*********************");
		String show="CardosoValue:"+CardosoValue+";     \n"+"CyclomaticValue:"+CyclomaticValue+";\n		"+"Complexity:"+complexity;
		return show;
	
	}
}
