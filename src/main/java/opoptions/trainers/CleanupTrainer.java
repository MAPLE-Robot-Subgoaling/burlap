package opoptions.trainers;

import java.util.List;

import javax.swing.JFrame;

import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.MDPSolverInterface;
import burlap.behavior.singleagent.auxiliary.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.mdp.auxiliary.common.GoalConditionTF;
import burlap.mdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.oo.propositional.PropositionalFunction;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.common.GoalBasedRF;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.statehashing.HashableStateFactory;
import burlap.visualizer.Visualizer;
import cleanup.Cleanup;
import cleanup.CleanupGoal;
import cleanup.CleanupGoalDescription;
import cleanup.CleanupRF;
import cleanup.CleanupVisualizer;
import cleanup.state.CleanupRandomStateGenerator;
import cleanup.state.CleanupState;
import opoptions.OPODriver;
import opoptions.OPOTrainer;

public class CleanupTrainer extends OPOTrainer {

    // specified by data file
    public Cleanup domainGenerator;
    public int numGoals;
    public int numBlocksTraining;
    public int numBlocksEvaluation;
    public String nameTrainingGoalPF;
    public String nameTrainingStateType;
    public String nameEvaluationStateType;
    public double rewardDefault;
    public double rewardGoal;
    public double rewardNoop;
    public double rewardPull;
//    private MDPSolver optionPlanner;

    // generated in code
    private CleanupGoal goal;
    private CleanupGoalDescription[] goalDescriptions;

    public CleanupTrainer() {
        // use SimulationConfig to load the trainer, not this constructor
    }

    @Override
    public OOState setupStateTraining() {
        CleanupRandomStateGenerator.setDebugMode(true);
        CleanupRandomStateGenerator randomCleanup = new CleanupRandomStateGenerator();
        randomCleanup.setWidth(domainGenerator.getWidth());
        randomCleanup.setHeight(domainGenerator.getHeight());
        initialState = (OOState) randomCleanup.getStateFor(nameTrainingStateType, numBlocksTraining);  //randomCleanup.generateOneRoomOneDoor(); // generateTaxiInCleanup(1);//.generateCentralRoomWithClosets(1); //cw.getRandomState(domain, rng, numBlocks);
        return (OOState) initialState;
    }

    @Override
    public OOState setupStateEvaluation() {
        CleanupRandomStateGenerator.setDebugMode(true);
        CleanupRandomStateGenerator randomCleanup = new CleanupRandomStateGenerator();
        randomCleanup.setWidth(domainGenerator.getWidth());
        randomCleanup.setHeight(domainGenerator.getHeight());
        initialState = (OOState) randomCleanup.getStateFor(nameEvaluationStateType, numBlocksEvaluation);  // randomCleanup.generateCentralRoomWithFourDoors(0); //generateTaxiInCleanup(1);//.generateCentralRoomWithClosets(1); //cw.getRandomState(domain, rng, numBlocks);
        return (OOState) initialState;
    }

    @Override
    public OOSADomain setupDomain() {
        goal = new CleanupGoal();
        rf = new CleanupRF(goal, rewardGoal, rewardDefault, rewardNoop, rewardPull);
        tf = new GoalConditionTF(goal);
//		tf = new NullTermination();
        domainGenerator.setRf(rf);
        domainGenerator.setTf(tf);
        domain = (OOSADomain) domainGenerator.generateDomain();

        // setup the goal
        PropositionalFunction trainingGoalPF = getTrainingGoalPF();
        goalDescriptions = CleanupRandomStateGenerator.getRandomGoalDescription((CleanupState) initialState, numGoals, trainingGoalPF);
        goal.setGoals(goalDescriptions);
        OPODriver.log("Goal is: " + goalDescriptions[0]);

        return (OOSADomain) domain;
    }

    public PropositionalFunction getTrainingGoalPF() {
        PropositionalFunction goalPF = ((OOSADomain) domain).propFunction(nameTrainingGoalPF);
        return goalPF;
    }

    @Override
    public void runEpisodeVisualizer(String filePrefix) {
        Visualizer v = CleanupVisualizer.getVisualizer(domainGenerator.getWidth(), domainGenerator.getHeight());
        EpisodeSequenceVisualizer esv = new EpisodeSequenceVisualizer(v, domain, episodeOutputPath + "/" + filePrefix);
        esv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void runEpisodeVisualizer(List<Episode> episodes) {
        Visualizer v = CleanupVisualizer.getVisualizer(domainGenerator.getWidth(), domainGenerator.getHeight());
        EpisodeSequenceVisualizer esv = new EpisodeSequenceVisualizer(v, domain, episodes);
        esv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

//    public MDPSolver getOptionPlanner() {
//        return optionPlanner;
//    }
//
//    public void setOptionPlanner(MDPSolver optionPlanner) {
//        this.optionPlanner = optionPlanner;
//    }

    @Override
    public MDPSolverInterface initializeOptionPlanner(StateConditionTest specificGoal) {
        RewardFunction originalRF = domainGenerator.getRf();
        TerminalFunction originalTF = domainGenerator.getTf();
        RewardFunction optionRF = new GoalBasedRF(specificGoal);
        TerminalFunction optionTF = new GoalConditionTF(specificGoal);
        domainGenerator.setRf(optionRF);
        domainGenerator.setTf(optionTF);
        SADomain optionDomain = (SADomain) domainGenerator.generateDomain();
        domainGenerator.setRf(originalRF);
        domainGenerator.setTf(originalTF);
        double optionGamma = 0.95;
        HashableStateFactory optionHashingFactory = hashingFactory;
        double maxDelta = 0.0001;
        int maxIterations = 10000;
        ValueIteration vi = new ValueIteration(optionDomain, optionGamma, optionHashingFactory, maxDelta, maxIterations);
        return vi;
//		optionPlanner.setDomain(optionDomain);
//		optionPlanner.setHashingFactory(optionHashingFactory);
//		optionPlanner.resetSolver();
//		return optionPlanner;
//		Planner planner = new Planner(optionPlanner);
//		Planner planner = null;
//		try {
//			planner = (Planner) BeanUtils.cloneBean(optionPlanner);
//			planner.setDomain(optionDomain);
//			planner.setHashingFactory(optionHashingFactory);
//			planner.resetSolver();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return planner;
    }

}
