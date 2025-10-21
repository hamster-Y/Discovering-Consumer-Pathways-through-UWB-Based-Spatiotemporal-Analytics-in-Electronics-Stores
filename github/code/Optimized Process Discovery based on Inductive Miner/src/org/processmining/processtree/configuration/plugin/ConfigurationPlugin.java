package org.processmining.processtree.configuration.plugin;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.configuration.Configuration;
import org.processmining.processtree.configuration.ConfigurationInstantiater;
import org.processmining.processtree.configuration.impl.ConfigurationInstantiaterImpl;

public class ConfigurationPlugin {

	@Plugin(
		name = "Instantiate Process Tree with a Configuration",
		parameterLabels = {"Process Tree", "Configuration"},
		returnLabels = { "Instantiated Process Tree" },
		returnTypes = { ProcessTree.class },
		userAccessible = true,
		help = "Instantiates a Process Tree with a Configuration"
	)
	@UITopiaVariant(
		affiliation = UITopiaVariant.EHV,
		author = "D.M.M. Schunselaar",
		email = "D.M.M.Schunselaar@tue.nl"
	)
	public ProcessTree instantiate(PluginContext context, ProcessTree tree, Configuration configuration){
		ConfigurationInstantiater confInst = new ConfigurationInstantiaterImpl();
		return confInst.instantiate(tree, configuration);
	}
}
