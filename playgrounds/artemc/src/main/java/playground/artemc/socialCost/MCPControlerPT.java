package playground.artemc.socialCost;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.scoreAnalyzer.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoreAnalyzer.DisaggregatedScoreAnalyzer;
import playground.artemc.socialCost.SocialCostControllerWithPT.InitializerPT;


public class MCPControlerPT {

private static final Logger log = Logger.getLogger(MCPControlerPT.class);
	
	static String configFile;
	
	public static void main(String[] args) {
		
		configFile = args[0];
		MCPControlerPT runner = new MCPControlerPT();
		runner.runInternalizationFlows(configFile);
	}
	
	private void runInternalizationFlows(String configFile) {
		Controler controler = new Controler(configFile);
		InitializerPT initializer = new InitializerPT();
		controler.addControlerListener(initializer);
		
		// Additional analysis
		ScenarioImpl scnearioImpl = (ScenarioImpl) controler.getScenario();
		controler.setScoringFunctionFactory(new DisaggregatedCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getNetwork()));
		controler.addControlerListener(new DisaggregatedScoreAnalyzer(scnearioImpl));
		controler.addControlerListener(new SimpleAnnealer());
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
