package com.marginallyclever.robotOverlord.dl4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.junit.Test;

public class DL4JTest {
	//@Test
	public void testDL4J() throws Exception {
		final int numRows = 28; // The number of rows of a matrix.
	    final int numColumns = 28; // The number of columns of a matrix.
	    int outputNum = 10; // Number of possible outcomes (e.g. labels 0 through 9).
	    int batchSize = 128; // How many examples to fetch with each step.
	    int rngSeed = 123; // This random-number generator applies a seed to ensure that the same initial weights are used when training. We’ll explain why this matters later.
	    int numEpochs = 15; // An epoch is a complete pass through a given dataset.
	    
		DataSetIterator mnistTrain = new MnistDataSetIterator(batchSize, true, rngSeed);
		DataSetIterator mnistTest = new MnistDataSetIterator(batchSize, false, rngSeed);
		
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
	            .seed(rngSeed)
	            .l2(1e-4)
	            .weightInit(WeightInit.XAVIER)
	            .updater(new Nesterovs(0.006,0.9))
	            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
	            .list()
	            .layer(0, new DenseLayer.Builder()
	                    .nIn(numRows * numColumns) // Number of input data points.
	                    .nOut(1000) // Number of output data points.
	                    .activation(Activation.IDENTITY) // Activation function.
	                    .weightInit(WeightInit.XAVIER) // Weight initialization.
	                    .build())
	            .layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
	                    .nIn(1000)
	                    .nOut(outputNum)
	                    .activation(Activation.SOFTMAX)
	                    .weightInit(WeightInit.XAVIER)
	                    .build())
	            .pretrain(false)
	            .backprop(true)
	            .build();
		
		MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        //print the score with every 1 iteration
        model.setListeners(new ScoreIterationListener(1));

        System.out.println("Train model....");
        model.setListeners(new ScoreIterationListener(10)); //Print score every 10 iterations
        for (int i=0; i <numEpochs; ++i) {
            model.fit(mnistTrain);
            System.out.println("*** Completed epoch "+i+" ***");
        }

        System.out.println("Evaluate model....");
        Evaluation eval = model.evaluate(mnistTest);
        System.out.println(eval.stats());
        System.out.println("****************Example finished********************");
	}

	//@Test
	public void testDL4JRead3Lines() throws Exception {
		try {
		    BufferedReader br = new BufferedReader(new FileReader(new File("FK2IK.csv")));
		    System.out.println(br.readLine());
		    System.out.println(br.readLine());
		    System.out.println(br.readLine());
		    br.close();
		} catch(Exception e) {}
	}
	
	// attempt to train network to understand FK (6 values) -> IK (6 values)
	// https://www.opencodez.com/java/deeplearaning4j.htm ?
	@Test
	public void testFirstDL4J() throws Exception {
		final int numInputs = 6;
	    int numOutputs = 6; // Number of possible outcomes (e.g. labels 0 through 9).
	    int batchSize = 128; // How many examples to fetch with each step.
	    int rngSeed = 123; // This random-number generator applies a seed to ensure that the same initial weights are used when training. We’ll explain why this matters later.
	    int numEpochs = 15; // An epoch is a complete pass through a given dataset.
	    
		//DataSetIterator mnistTrain = new MnistDataSetIterator(batchSize, true, rngSeed);
		//DataSetIterator mnistTest = new MnistDataSetIterator(batchSize, false, rngSeed);

	    
	    int numLinesToSkip = 0;
	    char delimiter = ',';
	    RecordReader recordReader = new CSVRecordReader(numLinesToSkip,delimiter);
	    recordReader.initialize(new FileSplit(new File("FK2IK.csv")));
	    
        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
	    // DR our data has 6 inputs and then 6 outputs 
        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader,6,11,batchSize,false);
        
        DataSet allData = iterator.next();
        allData.shuffle();
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.85); //Use 85% of data for training

        DataSet trainingData = testAndTrain.getTrain();
        DataSet testData = testAndTrain.getTest();

        //We need to normalize our data. We'll use NormalizeStandardize (which gives us mean 0, unit variance):
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        normalizer.transform(trainingData);     //Apply normalization to the training data
        normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set
		
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
	            .seed(rngSeed)
	            .l2(1e-4)
	            .weightInit(WeightInit.XAVIER)
	            .activation(Activation.TANH)
	            .updater(new Nesterovs(0.006,0.9))
	            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
	            .list()
	            .layer(0, new DenseLayer.Builder()
	                    .nIn(numInputs) // Number of input data points.
	                    .nOut(12) // Number of output data points.
	                    .activation(Activation.IDENTITY) // Activation function.
	                    .weightInit(WeightInit.XAVIER) // Weight initialization.
	                    .build())
	            .layer(1, new DenseLayer.Builder().nIn(12).nOut(12).build() )
	            .layer(2, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
	                    .nIn(12)
	                    .nOut(numOutputs)
	                    .activation(Activation.SOFTMAX)
	                    .weightInit(WeightInit.XAVIER)
	                    .build())
	            .pretrain(false)
	            .backprop(true)
	            .build();
		
		MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        System.out.println("Train model....");
        model.setListeners(new ScoreIterationListener(100)); //Print score every 10 iterations
        for (int i=0; i <numEpochs; ++i) {
            model.fit(trainingData);
            System.out.println("*** Completed epoch "+i+" ***");
        }

        System.out.println("Evaluate model....");
        Evaluation eval = new Evaluation(3);
        INDArray output = model.output(testData.getFeatures());
        model.save(new File("FK2IK.nn"));
        eval.eval(testData.getLabels(), output);
        System.out.println(eval.stats());
        System.out.println("****************Example finished********************");
	}
}
