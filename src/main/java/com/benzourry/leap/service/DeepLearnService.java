//package com.benzourry.leap.service;
//
//
//import org.datavec.api.records.reader.RecordReader;
//import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
//import org.datavec.api.split.FileSplit;
//import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
//import org.deeplearning4j.eval.Evaluation;
//import org.deeplearning4j.nn.conf.BackpropType;
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
//import org.deeplearning4j.nn.conf.layers.DenseLayer;
//import org.deeplearning4j.nn.conf.layers.OutputLayer;
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
//import org.deeplearning4j.nn.weights.WeightInit;
//import org.nd4j.linalg.activations.Activation;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.dataset.DataSet;
//import org.nd4j.linalg.dataset.SplitTestAndTrain;
//import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
//import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
//import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
//import org.nd4j.linalg.factory.Nd4j;
//import org.nd4j.linalg.io.ClassPathResource;
//import org.nd4j.linalg.learning.config.Nesterovs;
//import org.nd4j.linalg.lossfunctions.LossFunctions;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.IOException;
//
//@Service
//public class DeepLearnService {
//
//    private static final String MNIST_DATASET_ROOT_FOLDER = "/home/vincenzo/dl4j/mnist_png/";
//    //Height and widht in pixel of each image
//    private static final int HEIGHT = 28;
//    private static final int WIDTH = 28;
//    //The total number of images into the training and testing set
//    private static final int N_SAMPLES_TRAINING = 60000;
//    private static final int N_SAMPLES_TESTING = 10000;
//    //The number of possible outcomes of the network for each input,
////correspondent to the 0..9 digit classification
//    private static final int N_OUTCOMES = 10;
//
//    MultiLayerNetwork model;
//
//
//
//    private static final int CLASSES_COUNT = 3;
//    private static final int FEATURES_COUNT = 4;
//
//    public String createNet() throws IOException, InterruptedException {
//
//        DataSet allData;
//        try (RecordReader recordReader = new CSVRecordReader(0, ',')) {
//            recordReader.initialize(new FileSplit(new ClassPathResource("iris.txt").getFile()));
//
//            DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, 150, FEATURES_COUNT, CLASSES_COUNT);
//            allData = iterator.next();
//        }
//
//        allData.shuffle(42);
//
//        DataNormalization normalizer = new NormalizerStandardize();
//        normalizer.fit(allData);
//        normalizer.transform(allData);
//
//        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.98);
//        DataSet trainingData = testAndTrain.getTrain();
//        DataSet testData = testAndTrain.getTest();
//
//        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
//                .iterations(1000)
//                .activation(Activation.TANH)
//                .weightInit(WeightInit.XAVIER)
//                .regularization(true)
//                .learningRate(0.1).l2(0.0001)
//                .list()
//                .layer(0, new DenseLayer.Builder().nIn(FEATURES_COUNT).nOut(3)
//                        .build())
//                .layer(1, new DenseLayer.Builder().nIn(3).nOut(3)
//                        .build())
//                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
//                        .activation(Activation.SOFTMAX)
//                        .nIn(3).nOut(CLASSES_COUNT).build())
//                .backpropType(BackpropType.Standard).pretrain(false)
//                .build();
//
//        model = new MultiLayerNetwork(configuration);
//        model.init();
//        model.fit(trainingData);
//
//
//
//        INDArray output = model.output(testData.getFeatures());
//
//        Evaluation eval = new Evaluation(CLASSES_COUNT);
//        eval.eval(testData.getLabels(), output);
//        return eval.stats();
//
//    }
//
//    public Object predict(double a, double b, double c, double d){
////        DataSet ds = new DataSet(Nd4j.create(new double[]{a,b,c,d});
////        ds.
//        INDArray features = Nd4j.create(new double[]{a,b,c,d});
////        d.setFeatures(features);
//        return model.predict(features);
//    }
//
//    public void word2vec(){
//        SentenceIterator iter = new LineSentenceIterator(new File("/your/absolute/file/path/here.txt"));
//    }
//}
