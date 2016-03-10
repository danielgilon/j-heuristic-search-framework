package org.cs4j.core.mains;

import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.*;
import org.cs4j.core.data.Weights;
import org.cs4j.core.domains.FifteenPuzzle;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainDaniel {

    private static String fileEnd;
    private static String filePrefix;
    private static SearchAlgorithm alg;
    private static String relPath = "C:/Users/Daniel/Documents/gilond/Master/ResearchData/";
    private static String inputPath;
    private static String outputPath;
    private static String summaryPath;
    private static Weights.SingleWeight w;
    private static String domainName;
    private static Weights weights = new Weights();
    private static boolean reopen = true;
    private static boolean overwriteSummary;
    private static boolean overwriteFile;
    private static boolean appendToFile;
    private static boolean useBestFR;
    private static double totalWeight;
    private static int instancesNum;
    private static SearchAlgorithm[] SearchAlgorithmArr;
    private static HashMap<String,String> domainParams;

    private static double[] searchSave100FR(boolean save){
//        boolean reopen = true;
        double retArray[] = {0,0,0};//solved,generated,expanded
        OutputResult output = null;
        Constructor<?> cons = null;
        try {
            Class<?> cl = Class.forName("org.cs4j.core.domains."+domainName);
            cons = cl.getConstructor(InputStream.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            String str;
            String[] lines = new String[0];
            String fname = outputPath+alg.getName()+"_"+(int)w.wg+"_"+(int)w.wh+"_"+fileEnd+".csv";
            File file = new File(fname);
            if(appendToFile && save && file.exists()){
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                str = new String(data, "UTF-8");
                lines = str.split("\n");
//                System.out.println(lines);
            }
            if(save){
                output = new OutputResult(outputPath+alg.getName()+"_"+(int)w.wg+"_"+(int)w.wh+"_"+fileEnd, null, -1, -1, null, false, overwriteFile);
                output.writeln("InstanceID,Found,Depth,Generated,Expanded,Cpu Time,Wall Time");
            }
            System.out.println("Solving "+domainName + "\tAlg: " + alg.getName() + "_" + fileEnd + "\tweight: wg : " + w.wg + " wh: " + w.wh);
            //set algo total weight
            alg.setAdditionalParameter("weight", totalWeight + "");
            //search on this domain and algo and weight the 100 instances
            for (int i = 1; i <= instancesNum; ++i) {
                try {
                    double d[] = new double[]{i, 0, 0, 0, 0,0,0};
                    if(appendToFile && save && lines.length > i && lines[i].split(",").length == d.length){
                        output.writeln(lines[i]);

                        String[] lineSplit = lines[i].split(",");
                        retArray[0] += 1;
                        retArray[1] += Double.parseDouble(lineSplit[3]);
                        retArray[2] += Double.parseDouble(lineSplit[4]);
                    }
                    else {
                        System.out.println("Solving " + domainName + " instance " + i + "\tAlg: " + alg.getName() + "_" + fileEnd + "\tweight: wg : " + w.wg + " wh: " + w.wh);
                        InputStream is = new FileInputStream(new File(inputPath + "/" + i + ".in"));
                        SearchDomain domain;
                        domain = (SearchDomain) cons.newInstance(is);
                        for(Map.Entry<String, String> entry : domainParams.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            domain.setAdditionalParameter(key,value);
                        }
                        SearchResult result = alg.search(domain);
                        if (result.hasSolution()) {
                            d[0] = i;
                            d[1] = 1;
                            d[2] = result.getSolutions().get(0).getLength();
                            d[3] = result.getGenerated();
                            d[4] = result.getExpanded();
                            d[5] = result.getCpuTimeMillis();
                            d[6] = result.getWallTimeMillis();
                            retArray[0] += 1;
                            retArray[1] += result.getGenerated();
                            retArray[2] += result.getSolutions().get(0).getLength();
                        }
                        if(save) {
                            output.appendNewResult(d);
                            output.newline();
                        }
                    }
                } catch (OutOfMemoryError e) {
                    System.out.println("[INFO] MainDaniel OutOfMemory :-( "+e);
                    System.out.println("[INFO] OutOfMemory in:"+alg.getName()+" on:"+ domainName);
                }
                catch (FileNotFoundException e) {
                    System.out.println("[INFO] FileNotFoundException At inputPath:"+inputPath);
                    e.printStackTrace();
                }
                catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }  catch (IOException e) {
            System.out.println("[INFO] IOException At outputPath:"+outputPath);
            e.printStackTrace();
        }
        if(save) {
            output.close();
        }
        return retArray;
    }

    private static double[] findBestFR() {
        double[] resultSummary = new double[4];
        int lowerLimit,upperLimit;
        double lg,mg=0,rg,bestG = Integer.MAX_VALUE, noiseLimit = 1.1;
        int lfr,mfr,rfr,bestFR;

        lowerLimit = (int)Math.pow(2,6);
        upperLimit = (int)Math.pow(2,20);

        bestFR = mfr = upperLimit;

        while (mfr > lowerLimit && mg <= bestG * noiseLimit) {//start decreasing
            alg.setAdditionalParameter("FR",mfr+"");
            mg = searchSave100FR(false)[1];
            if(mg < bestG){
                bestG = mg;
                bestFR=mfr;
            }
            System.out.println("scanning: FR:" + mfr + ", Generated:" + mg);
            mfr=mfr/2;
        }

/*        rfr=bestFR*2;
        alg.setAdditionalParameter("FR",rfr+"");
        rg = searchSave100FR(false)[1];

        lfr=bestFR/2;
        alg.setAdditionalParameter("FR",lfr+"");
        lg = searchSave100FR(false)[1];

        System.out.println("shrink [" + lfr + "," + rfr + "] ; {" + lg+ ","+ bestG + ","+ rg +"}");

        while (rfr - lfr > 1) {//start shrink
            mfr = (rfr+lfr)/2;
            alg.setAdditionalParameter("FR",mfr+"");
            mg = searchSave100FR(false)[1];
            bestG = Math.min(mg,bestG);
            if((mg == rg) || (lg < mg && mg < rg)){
                rg=mg;
                rfr=mfr;
            }
            else if((mg == lg) || (lg > mg && mg > rg)){
                lg=mg;
                lfr=mfr;
            }
            else if(mg<rg && mg<lg){
                int mrfr=(mfr+rfr)/2;//median right FR
                int lmfr=(lfr+mfr)/2;//left median FR

                alg.setAdditionalParameter("FR",mrfr+"");
                double mrg = searchSave100FR(false)[1];
                bestG = Math.min(mrg,bestG);

                alg.setAdditionalParameter("FR",lmfr+"");
                double lmg = searchSave100FR(false)[1];
                bestG = Math.min(lmg,bestG);

                if(mg <= mrg && mg <= lmg){
                    //mg is best from right, update r to mr
                    rg=mrg;
                    rfr=mrfr;
                    //mg is best from left, update l to ml
                    lg=lmg;
                    lfr=lmfr;
                } else if(lmg > mrg){//mrg is best, update l to m
                    lg=mg;
                    lfr=mfr;
                } else {//lmg is best, update r to m
                    rg=mg;
                    rfr=mfr;
                }
//                System.out.println("mg is best");
            }
            else{
//                System.out.println("mg is biggest!");
                if(lg<rg){
                    rg=mg;
                    rfr=mfr;
                }
                else if (rg<mg){
                    lg=mg;
                    lfr=mfr;
                }
                else
                    System.out.println("all same");
            }
            System.out.println("shrink [" + lfr + "," + rfr + "] ; {" + lg+ ","+ mg+ ","+ rg +"}");
        }
//        System.out.println("Best FR:" + rfr + ", Generated:" + rg);*/

        alg.setAdditionalParameter("FR",bestFR+"");
        double[] tempArray = searchSave100FR(true);//solved,generated,expanded
        if(bestG < tempArray[1]){
            System.out.println("Bad search");
        }
        System.arraycopy( tempArray, 0, resultSummary, 0, tempArray.length );
        resultSummary[3] = bestFR;
        return resultSummary;
    }

       private static void manipulateFiles(){
        try {
            String gridName = "brc202d.map";
//            String path = relPath + "input/GridPathFinding/"+gridName;
            for(int i=1;i<=100;i++){
/*                InputStream stream = new FileInputStream(new File(path+"/"+i+".in"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = reader.readLine();
                String sz[] = line.trim().split(" ");
                System.out.println(sz[1]);
                sz[1] = path;
                String joined = String.join(" ", sz);
                System.out.println(joined);*/

                Path path = Paths.get(relPath + "input/GridPathFinding/"+gridName+"/"+i+".in");
                Charset charset = StandardCharsets.UTF_8;
                String toReplace = "input/gridpathfinding/raw/maps/brc202d.map";
                String replaceWith = relPath + "input/GridPathFinding/"+gridName;
                String content = new String(Files.readAllBytes(path), charset);
                System.out.println(content);
                content = content.replaceAll(toReplace, replaceWith);
                System.out.println(content);
                Files.write(path, content.getBytes(charset));
 /*               String line = reader.readLine();
                String[] ary = line.split(" ");
                PrintWriter writer = new PrintWriter("C:/Users/Daniel/Documents/gilond/Master/Research Data/input/fifteenpuzzle/states15/"+i+".in", "UTF-8");
                writer.println("15");
                writer.println();
                for(int j=0;j<16;j++){
                    writer.println(ary[j]);
                }
                writer.println();
                for(int j=0;j<16;j++){
                    writer.println(j);
                }
                writer.close();*/
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createSummary()throws IOException{
        int algoNum = SearchAlgorithmArr.length;
        try {
            OutputResult output = new OutputResult(summaryPath+"_"+fileEnd, null, -1, -1, null, false, overwriteSummary);//never overwrite
            ArrayList<String> toPrintArrLIst = new ArrayList<>();
            String toPrint;
            int resultColNum = 6;
            toPrintArrLIst.add("WG");
            toPrintArrLIst.add("WH");
            toPrintArrLIst.add("All Solved");

            for (int i = 0; i < algoNum; i++) {
                alg = SearchAlgorithmArr[i];
//                toPrintArrLIst.add(alg.getName()+" Solved");
//                toPrintArrLIst.add(alg.getName()+" Depth");
                toPrintArrLIst.add(alg.getName()+" Generated");
//                toPrintArrLIst.add(alg.getName()+" Expanded");
                toPrintArrLIst.add(alg.getName()+" Cpu Time");
//                toPrintArrLIst.add(alg.getName()+" Wall Time");
            }
            toPrint = toPrintArrLIst.toString();
            toPrint = toPrint.substring(1, toPrint.length()-1);
            output.writeln(toPrint);
            for ( Weights.SingleWeight ws :weights.NATURAL_WEIGHTS) {
                w = ws;
                totalWeight = w.wh / w.wg;
                String resultsAlgoColumn[][] = new String[algoNum][];
                System.out.println("Summary "+domainName + "\tweight: wg : " + w.wg + " wh: " + w.wh);
                //read the files to resultsAlgoColumn[i] where i is the algorithm
                for (int i = 0; i < algoNum; i++) {
                    alg = SearchAlgorithmArr[i];
                    String fileName = outputPath + alg.getName() + "_" + (int) w.wg + "_" + (int) w.wh + "_" + fileEnd + ".csv";
                    File file = new File(fileName);
                    if (file.exists()) {
                        byte[] data = new byte[(int) file.length()];
                        FileInputStream fis = new FileInputStream(file);
                        fis.read(data);
                        fis.close();
                        String str = new String(data, "UTF-8");
                        resultsAlgoColumn[i] = str.split("\n");
                    }
                }

                double[] resultArr = new double[toPrintArrLIst.size()];
                for (int j = 1; j <= instancesNum; j++) {
                    boolean allExist = true;
                    String[] line = new String[algoNum];
                    double[][] tempRes = new double[algoNum][];
                    double[] tempS = new double[algoNum];
                    double[] tempD = new double[algoNum];
                    double[] tempG = new double[algoNum];
                    double[] tempE = new double[algoNum];
                    double[] tempC = new double[algoNum];
                    double[] tempW = new double[algoNum];
                    for (int i = 0; i < algoNum; i++) {//check if all algo solved successfully
                        line[i] = resultsAlgoColumn[i][j];
                        String[] lineSplit = line[i].split(",");
/*                        for(int k=0;k<resultColNum;k++){
                            tempRes[i][k] = Double.parseDouble(lineSplit[k]);
                        }*/
                        tempS[i] = Double.parseDouble(lineSplit[1]);//found-solved
                        tempD[i] = Double.parseDouble(lineSplit[2]);//Depth
                        tempG[i] = Double.parseDouble(lineSplit[3]);//Generated
                        tempE[i] = Double.parseDouble(lineSplit[4]);//Expanded
                        tempC[i] = Double.parseDouble(lineSplit[5]);//Cpu Time
                        tempW[i] = Double.parseDouble(lineSplit[6]);//Wall Time
                        if (tempS[i] != 1.0) {
                            allExist = false;
                        }
                    }
                    if (allExist) {//save to summary
                        int pos = 0;
                        resultArr[pos++] =w.wg;//WG
                        resultArr[pos++] =w.wh;//WH
                        resultArr[pos++] +=1;//found
/*                        for(int k=0;k<resultColNum;k++){
                            for (int i = 0; i < algoNum; i++) {
                                resultArr[pos++] = tempRes[i][k];
                            }
                        }*/
                        for (int i = 0; i < algoNum; i++) {
//                            resultArr[pos++] += tempS[i];//solved
//                            resultArr[pos++] += tempD[i];//Depth
                            resultArr[pos++] += tempG[i];//Generated
//                            resultArr[pos++] += tempE[i];//Expanded
                            resultArr[pos++] += tempC[i];//Cpu Time
//                            resultArr[pos++] += tempW[i];//Wall Time
                        }
                    }
                }

                //Calculate Average skip WG,WH,found
                toPrintArrLIst = new ArrayList<>();
                toPrintArrLIst.add(Double.toString(resultArr[0]));
                toPrintArrLIst.add(Double.toString(resultArr[1]));
                toPrintArrLIst.add(Double.toString(resultArr[2]));
                for (int k = 3; k < resultArr.length; k++) {
                    resultArr[k] =resultArr[k]/resultArr[2];// value/found
                    DecimalFormat formatter = new DecimalFormat("#,###");
                    String s = formatter.format(resultArr[k]).replace(",","'");
                    toPrintArrLIst.add(s);
                }
                //print result to file
                toPrint = toPrintArrLIst.toString();
                toPrint = toPrint.substring(1, toPrint.length()-1);
                output.writeln(toPrint);
            }
            output.close();
        } catch (IOException e) {
            throw new IOException("File " + summaryPath + " already Exist, or Folder is missing");
        }
    }

    private static void afterSetDomain() throws IOException{
        //search over algo and weight
        for ( Weights.SingleWeight ws :weights.NATURAL_WEIGHTS) {
            w = ws;
            totalWeight = w.wh / w.wg;
            for (SearchAlgorithm sa : SearchAlgorithmArr) {
                alg = sa;
                if (useBestFR) {
                    double resultArray[] = findBestFR();//solved,generated,expanded,bestFR
                }
                else{
                    searchSave100FR(true);
                }
            }
        }
        //create summary over algo and weight
        createSummary();
    }

    public static void main(String[] args) throws IOException {
//        manipulateFiles();
//        domainName = "GridPathFinding";

        overwriteFile = true;
        overwriteSummary = true;
        appendToFile = true;//do not calculate again if already exist
        useBestFR = false;
        instancesNum = 100;
        String summaryFolder = "7.3.2016";

        String[] domains = {
                "Pancakes",
//            "VacuumRobot",
            "FifteenPuzzle",
//            "DockyardRobot",
//            "GridPathFinding"
        };

        SearchAlgorithm[] AlgoArr = {
//            new EES2(),
                new DP(),
                new EES(1),
                new WAStar(),
        };
        SearchAlgorithmArr = AlgoArr;




        if(useBestFR)fileEnd = "BestFr";
        else fileEnd = "NoFr";

        for (String dN : domains) {
            domainName = dN;
            domainParams = new HashMap<>();
            switch (domainName) {
                case "FifteenPuzzle": {
                    System.out.println("Solving FifteenPuzzle");
//                    filePrefix = "heavy";
                    filePrefix = "invr";
//                    filePrefix = "sqrt";
//                    filePrefix = "unit";
                    domainParams.put("cost-function", filePrefix);
                    inputPath = relPath + "input/FifteenPuzzle/states15";
                    outputPath = relPath + "results/FifteenPuzzle/results_7.3.2016/"+filePrefix+"_";
                    summaryPath = relPath + "results/summary/"+summaryFolder+"/"+filePrefix+"_FifteenPuzzle";
                    afterSetDomain();
                    break;
                }
                case "Pancakes": {
                    int[] pancakesNum = new int[]{10, 12, 16, 20, 40};
//                    int[] pancakesNum = new int[]{20,40};
//                    int[] pancakesNum = new int[]{20};
                    int GAPK = 2;
                    domainParams.put("GAP-k", GAPK+"");
                    int num;
                    for (int j = 0; j < pancakesNum.length; j++) {
                        num = pancakesNum[j];
                        System.out.println("Solving Pancakes " + num);
                        filePrefix = num + "_GAP-"+GAPK+"_";
                        inputPath = relPath + "input/pancakes/generated-" + num;
                        outputPath = relPath + "results/pancakes/" + num + "/" + filePrefix;
                        summaryPath = relPath + "results/summary/"+summaryFolder+"/"+ filePrefix +"pancakes";
                        afterSetDomain();
                    }
                    break;
                }
                case "VacuumRobot": {
//                    int[] dirts = new int[]{10};
                    int[] dirts = new int[]{5, 10};
                    for (int j = 0; j < dirts.length; j++) {
                        System.out.println("Solving VacuumRobot");
                        filePrefix = "";
                        inputPath = relPath + "input/VacuumRobot/generated-" + dirts[j] + "-dirt";
                        outputPath = relPath + "results/VacuumRobot/" + dirts[j] + "-dirt/"+filePrefix;
                        summaryPath = relPath + "results/summary/"+summaryFolder+"/VacuumRobot-" + dirts[j] + "-dirt";
                        afterSetDomain();
                    }
                    break;
                }
                case "DockyardRobot": {
                    System.out.println("Solving DockyardRobot");
                    filePrefix = "";
                    inputPath = relPath + "input/dockyard-robot-max-edge-2-out-of-place-30/";
                    outputPath = relPath + "results/dockyard-robot-max-edge-2-out-of-place-30/"+filePrefix;
                    summaryPath = relPath + "results/summary/"+summaryFolder+"/dockyard-robot-max-edge-2-out-of-place-30";
                    afterSetDomain();
                    break;
                }
                case "GridPathFinding": {
                    String gridName = "brc202d.map";
                    System.out.println("Solving VacuumRobot");
                    inputPath = relPath + "input/GridPathFinding/" + gridName;
                    outputPath = relPath + "results/GridPathFinding/" + gridName;
                    summaryPath = relPath + "results/summary/GridPathFinding/" + gridName + "/";
                    filePrefix = "";
                    afterSetDomain();
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
        }
    }
}
