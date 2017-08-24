package opoptions;

import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.MDPSolverInterface;
import burlap.behavior.singleagent.auxiliary.performance.PerformancePlotter;
import burlap.debugtools.RandomFactory;
import burlap.mdp.auxiliary.DomainGenerator;
import burlap.mdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.oo.propositional.PropositionalFunction;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.common.VisualActionObserver;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.selected.SelectedConfig;
import burlap.statehashing.selected.SelectedHashableStateFactory;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import utils.Simulation;
import utils.SimulationConfig;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.*;

public abstract class OPOTrainer extends SimulationConfig {

    // by default, use 0 (BURLAP default)
    // to use different index, subclasses should override the getIndexForRandomFactory method
    public static final int DEFAULT_RNG_INDEX = 0;

    protected Classifier classifier;
    protected boolean identifierIndependentHashing = true;
    protected boolean includePFs;
    protected String trainerName = "unsetTrainer";
    protected String domainName = "unsetDomain";
//    protected RewardFunction rf;
//    protected TerminalFunction tf;
    protected HashableStateFactory hashingFactory;
    protected Environment env;

    protected String episodeOutputPathEvaluation;
    protected String lastSeedTimestampTraining = "unsetSeedTimestamp";
    protected String lastSeedTimestampEvaluation = "unsetSeedTimestamp";

    protected OPOption opoption = new OPOption();

    public String getEpisodeOutputPathEvaluation() {
        return episodeOutputPathEvaluation;
    }

    public void setEpisodeOutputPathEvaluation(String episodeOutputPathEvaluation) {
        this.episodeOutputPathEvaluation = episodeOutputPathEvaluation;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public void setTrainerName(String trainerName) {
        this.trainerName = trainerName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public boolean isIdentifierIndependentHashing() {
        return identifierIndependentHashing;
    }

    public void setIdentifierIndependentHashing(boolean identifierIndependentHashing) {
        this.identifierIndependentHashing = identifierIndependentHashing;
    }

    public abstract PropositionalFunction getTrainingGoalPF();

    public boolean satisfiesTrainingGoal(OOState s) {
        return getTrainingGoalPF().someGroundingIsTrue(s);
    }

//    public RewardFunction getRf() {
//        return rf;
//    }
//
//    public void setRf(RewardFunction rf) {
//        this.rf = rf;
//    }
//
//    public TerminalFunction getTf() {
//        return tf;
//    }
//
//    public void setTf(TerminalFunction tf) {
//        this.tf = tf;
//    }

    public HashableStateFactory getHashingFactory() {
        return hashingFactory;
    }

    public void setHashingFactory(HashableStateFactory hashingFactory) {
        this.hashingFactory = hashingFactory;
    }

    public String getLastSeedTimestampTraining() {
        return lastSeedTimestampTraining;
    }

    public String getLastSeedTimestampEvaluation() {
        return lastSeedTimestampEvaluation;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
        RandomFactory.seedMapped(getIndexForRandomFactory(), this.seed);
    }

    public Classifier getClassifier() throws Exception {
        return AbstractClassifier.makeCopy(classifier);
    }

    public void setClassifier(Classifier classifier) {
        this.classifier = classifier;
    }

    private int getIndexForRandomFactory() {
        return DEFAULT_RNG_INDEX;
    }

    public abstract OOState setupStateTraining();

    public abstract OOState setupStateEvaluation();

    public abstract OOSADomain setupDomainTraining();

    public abstract OOSADomain setupDomainNoRFTF();

    public abstract OOSADomain setupDomainEvaluation();

    public MDPSolver setupAgent() {
        agent.resetSolver();
        agent.setDomain(domain);
        hashingFactory = new SimpleHashableStateFactory(identifierIndependentHashing);
        agent.setHashingFactory(hashingFactory);
        return agent;
    }

    public String planAndRollout(PerformancePlotter plotter) {
        String seedTimestamp = Simulation.plan(this);
        return seedTimestamp;
    }

    public abstract void runEpisodeVisualizer(String filePrefix);

    public abstract void runEpisodeVisualizer(List<Episode> episodes);

    public void runTraining(PerformancePlotter plotter) {

        // 1. setup the state
        // 2. setup the domain
        // 3. setup the agent
        // 4. run the simulation

        // 1. setup the state
        setupStateTraining();

        // 2. setup the domain
        setupDomainTraining();

        // 3. setup the agent
        setupAgent();

        // 4. run the simulation
        String seedTimestamp = planAndRollout(plotter);
        lastSeedTimestampTraining = seedTimestamp;

    }

    public void addLearnedStateTest(LearnedStateTest test) {
        opoption.addLearnedStateTest(test);
    }

    public void runEvaluation(PerformancePlotter plotter) {

        setupStateEvaluation();

        setupDomainEvaluation();

        setupAgent();



    }

    public boolean getIncludePFs() {
        return includePFs;
    }

    public void setIncludePFs(boolean includePFs) {
        this.includePFs = includePFs;
    }

    public abstract MDPSolverInterface initializeOptionPlanner(StateConditionTest specificGoal);

    public void initializeTypeSignature(List<String> attributeWhitelist) {
        SelectedHashableStateFactory shsf = new SelectedHashableStateFactory();
        shsf.setSelection(attributeWhitelist);
        opoption.setTypeSignature(shsf);

        // create the OPOGoalPF
        String name = "pf_" + domainName + "_" + trainerName;
        // get the object counts
        Map<String, Integer> objectClassCounts = SelectedConfig.getObjectCounts(attributeWhitelist);
        String[] parameterClasses = SelectedConfig.getParameterClasses(objectClassCounts);
//        String[] parameterOrders = ((OOSADomain)domain).stateClassNames().toArray(new String[0]);
        String [] parameterOrderGroup = new String[parameterClasses.length];
        int groupNumber = 0;
        String lastClass = "default/unset";
        Map<String, Integer> objectClassToOrderNumber = new HashMap<String,Integer>();
        for (String parameterClass : parameterClasses) {
            if (!objectClassToOrderNumber.containsKey(parameterClass)) {
                objectClassToOrderNumber.put(parameterClass, groupNumber);
                groupNumber++;
            }
        }
        for(int i = 0; i < parameterOrderGroup.length; i++){
            parameterOrderGroup[i] = name + ".P" + objectClassToOrderNumber.get(parameterClasses[i]);
        }
        OPOLearnedPF opoGoalPF = new OPOLearnedPF(name+"_goal", parameterClasses, parameterOrderGroup);
        opoption.setOPOGoalPF(opoGoalPF);
        OPOLearnedPF opoInitiationPF = new OPOLearnedPF(name+"_initiation", parameterClasses, parameterOrderGroup);
        opoption.setOPOInitiationPF(opoInitiationPF);

    }

    public OPOption getOpoption() {
        return opoption;
    }

    public abstract DomainGenerator getDomainGenerator();

    @Override
    public SADomain getDomain() {
        return setupDomainNoRFTF();
    }


}
