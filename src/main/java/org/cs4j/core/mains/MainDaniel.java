package org.cs4j.core.mains;

import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.*;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.*;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.data.Weights;
import org.cs4j.core.domains.VacuumRobot;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MainDaniel {

    private static String fileEnd;
    private static String filePrefix;
    private static SearchAlgorithm alg;
    private static String relPath = "C:/Users/Daniel/Documents/gilond/Master/ResearchData/";
    private static String inputPath;
    private static String outputPath;
    private static String summarySheetName;
    private static String summaryName;
    private static Weights.SingleWeight w;
    private static String domainName;
    private static Weights weights = new Weights();
    private static boolean reopen = true;
    private static boolean overwriteSummary;
    private static boolean overwriteFile;
    private static boolean appendToFile;
    private static boolean useBestFR;
    private static boolean useOracle;
    private static double totalWeight;
    private static int startInstance;
    private static int stopInstance;
    private static boolean saveSolutionPath;
    private static SearchAlgorithm[] SearchAlgorithmArr;
    private static HashMap<String,String> domainParams;

    private static double[] searchSave100FR(boolean save){
//        boolean reopen = true;
        double retArray[] = {0,0,0};//solved,generated,expanded
        String[] resultColumnNames = {"InstanceID", "Found", "Depth", "Cost" , "Generated", "Expanded", "Cpu Time", "Wall Time"};
        if(alg.getName() == "DP"){
            resultColumnNames = new String[]{"InstanceID", "Found", "Depth", "Cost" ,"Generated", "Expanded", "Cpu Time", "Wall Time","times reordered","max Buckets reordered","max nodes on reorder"};
        }
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
                String toPrint = String.join(",", resultColumnNames);
                output.writeln(toPrint);
            }
            System.out.println("Solving "+domainName + "\tAlg: " + alg.getName() + "_" + fileEnd + "\tweight: wg : " + w.wg + " wh: " + w.wh);
            InputStream optimalIS = new FileInputStream(new File(inputPath + "/optimalSolutions.in"));
            BufferedReader optimalReader = new BufferedReader(new InputStreamReader(optimalIS));
            //set algo total weight
//            alg.setAdditionalParameter("weight", totalWeight + "");
            double h0 = 0;
            int found = 0;
            //search on this domain and algo and weight the 100 instances
            for (int i = startInstance; i <= stopInstance; ++i) {
                try {
                    double d[] = new double[resultColumnNames.length];

                    if(useOracle) {
                        String optimalLine = optimalReader.readLine();
                        String[] optArr = optimalLine.split(",");
                        int optInstance = Integer.parseInt(optArr[0]);
                        int optimalSolution = Integer.parseInt(optArr[1]);
                        double optimalBounded = optimalSolution * totalWeight;
                        if (optInstance != i) System.out.println("[WARNING] Wrong optimal solution set");
                        else alg.setAdditionalParameter("max-cost", optimalBounded + "");
//                        else alg.setAdditionalParameter("optimalSolution", optimalSolution + "");
                    }
                    InputStream is = new FileInputStream(new File(inputPath + "/" + i + ".in"));
                    SearchDomain domain;
                    domain = (SearchDomain) cons.newInstance(is);
                    for(Map.Entry<String, String> entry : domainParams.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        domain.setAdditionalParameter(key,value);
                    }

                    if(appendToFile && save && lines.length > i && lines[i].split(",").length == d.length){
                        output.writeln(lines[i]);
                        String[] lineSplit = lines[i].split(",");
                        String f = lineSplit[1];
                        double fd = Double.parseDouble(f);
                        int intf = (int)fd;
                        if(intf == 1){
                            found++;
                            h0+=domain.initialState().getH();
                        }
/*                        retArray[0] += 1;
                        retArray[1] += Double.parseDouble(lineSplit[3]);
                        retArray[2] += Double.parseDouble(lineSplit[4]);*/
                    }
                    else {
                        System.out.print("\rSolving " + domainName + " instance " + (found+1) +"/"+ i +"\tAlg: " + alg.getName() + "_" + fileEnd + "\tweight: wg : " + w.wg + " wh: " + w.wh);
                        SearchResult result = alg.search(domain);
                        if (result.hasSolution()) {
                            if(totalWeight == 1) saveOptimalSolution(result,i,domain);
//                            saveSolutionPathAsInstances(result,i);
                            d[1] = 1;
                            found++;
                            h0+=domain.initialState().getH();
                            d[2] = result.getSolutions().get(0).getLength();
                            d[3] = result.getSolutions().get(0).getCost();
                            d[4] = result.getGenerated();
                            d[5] = result.getExpanded();
                            d[6] = result.getCpuTimeMillis();
                            d[7] = result.getWallTimeMillis();
//                            d[8] = domain.initialState().getH();
                            if(alg.getName() == "DP"){
                                TreeMap extras = result.getExtras();
                                d[8] = extras.size();
                                if(d[8] != 0){
                                    int maxBuckets = Integer.parseInt(extras.lastKey().toString());
                                    d[9] = (double) maxBuckets;
                                    d[10] = Double.parseDouble(extras.get(maxBuckets+"").toString());
                                }
                            }

/*                            retArray[0] += 1;
                            retArray[1] += result.getGenerated();
                            retArray[2] += result.getSolutions().get(0).getLength();*/
                        }
                        if(save) {
                            d[0] = i;
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
                    i=stopInstance;
//                    e.printStackTrace();
                }
                catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            double avgh0 = h0/found;
//            System.out.println("averageH="+avgh0+"\n");
        }  catch (IOException e) {
            System.out.println("[INFO] IOException At outputPath:"+outputPath);
            e.printStackTrace();
        }
        finally {
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

    private static void saveOptimalSolution(SearchResult result, int instance, SearchDomain domain){
//        InputStream is = new FileInputStream(new File(inputPath + "/" + i + ".in"));
        String savePath = inputPath+"/optimalSolutions.in";
        File saveFile = new File(savePath);
        StringBuilder sb = new StringBuilder();
        int lineCounter = 0;
        if(saveFile.exists()){
            try {
                InputStream stream = new FileInputStream(saveFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = reader.readLine();
                while(line != null){
                    sb.append(line);
                    sb.append(System.getProperty("line.separator"));

                    line = reader.readLine();
                    lineCounter++;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(lineCounter < instance){
            int dOpt = result.getSolutions().get(0).getLength();
            sb.append(instance + ","+dOpt);
//            System.out.println("[INFO] Optimal solution found:\tinstance: "+instance+"\tdepth: "+dOpt);
            sb.append(System.getProperty("line.separator"));

            try {
                List<SearchDomain.Operator> operators = result.getSolutions().get(0).getOperators();
                PrintWriter writer = new PrintWriter(inputPath + "/optimalOperators" + instance + ".in", "UTF-8");
                SearchDomain.State parentState = domain.initialState();
                SearchDomain.State childState = null;
                for (int i = 0 ; i <=operators.size()-1; i++) {
                    boolean appended=false;
                    SearchDomain.Operator iop = operators.get(i);
                    childState = domain.applyOperator(parentState,iop);
                    PackedElement childPacked = domain.pack(childState);
                    int operatorsNum = domain.getNumOperators(parentState);
                    for(int j=0 ; j < operatorsNum;j++){
                        SearchDomain.Operator op = domain.getOperator(parentState,j);
                        SearchDomain.State stateJ = domain.applyOperator(parentState,op);
                        PackedElement packedJ = domain.pack(stateJ);
                        if(childPacked.equals(packedJ) || stateJ.equals(childState)){
                            appended=true;
//                            System.out.println(parentState.dumpStateShort()+" ->op("+j+")-> "+childState.dumpStateShort());
                            parentState = childState;
                            String toSave = j+"";
                            writer.println(toSave);
//                            System.out.print(toSave+" ");
                            break;
                        }
                    }
                    if(!appended){
                        System.out.println("[WARNING] state not added");
                    }
                }
                if(!domain.isGoal(childState)){
                    System.out.println("[WARNING] Goal NOT Reached");
                }
                writer.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        try {
            PrintWriter writer = new PrintWriter(savePath, "UTF-8");
            writer.print(sb);
//            System.out.println(sb);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static void saveSolutionPathAsInstances(SearchResult result, int instance){
/*           String gridName = "brc202d.map";
            String path = relPath + "input/GridPathFinding/"+gridName;
        for(int i=1;i<=100;i++){
                InputStream stream = new FileInputStream(new File(path+"/"+i+".in"));
               BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
               String line = reader.readLine();
               String sz[] = line.trim().split(" ");
               System.out.println(sz[1]);
               sz[1] = path;
               String joined = String.join(" ", sz);
               System.out.println(joined);

               Path path = Paths.get(relPath + "input/GridPathFinding/"+gridName+"/"+i+".in");
               Charset charset = StandardCharsets.UTF_8;
               String toReplace = "input/gridpathfinding/raw/maps/brc202d.map";
               String replaceWith = relPath + "input/GridPathFinding/"+gridName;
               String content = new String(Files.readAllBytes(path), charset);
               System.out.println(content);
               content = content.replaceAll(toReplace, replaceWith);
               System.out.println(content);
               Files.write(path, content.getBytes(charset));
               String line = reader.readLine();
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
               writer.close();
        }*/

/*        try {
            InputStream stream = new FileInputStream(new File(path+"/.txt"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            int counter = 1;
            while(line != null){
                String[] ary = line.split(" ");
                PrintWriter writer = new PrintWriter(path+"/"+counter+".in", "UTF-8");
                writer.println("15");
                writer.println();
                for(int j=0;j<16;j++){
                    writer.println(ary[j]);
                }
                writer.println();
                for(int j=0;j<16;j++){
                    writer.println(j);
                }
                writer.close();
                counter++;
                line = reader.readLine();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        String path = relPath + "input/pancakes/generated-40byInstanceStep/Instance"+instance;

        File theDir = new File(path);
        boolean dirExist = theDir.exists();
        // if the directory does not exist, create it
        if (!dirExist) {
//            System.out.println("creating directory: " + path);
            try{
                theDir.mkdir();
                dirExist = true;
            }
            catch(SecurityException se){
                //handle it
            }
        }
        if(dirExist) {
            List<SearchResult.Solution> solutions = result.getSolutions();
            SearchResult.Solution solution = solutions.get(0);
            List<SearchDomain.State> states = solution.getStates();
            List<SearchDomain.Operator> operators = solution.getOperators();

            try {
                PrintWriter writer = new PrintWriter(path+"/operators.in", "UTF-8");
                for(int i=0;i<operators.size();i++){
                    String toSave = operators.get(i).toString();
                    writer.println(toSave);
                    System.out.println(toSave);
                }
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

/*            for(int i = states.size()-1 ; i > 0 ; i--){
                String toSave = "40\n"+states.get(i).dumpStateShort();
                System.out.println(toSave);
                int pos = states.size() - i;
                try {
                    PrintWriter writer = new PrintWriter(path+"/"+pos+".in", "UTF-8");
                    writer.println(toSave);
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }

    private static void createSummary()throws IOException{
        int algoNum = SearchAlgorithmArr.length;
        try {
            int num = 0;
            String path = relPath +"results/summary/"+summaryName+".xls";
            File summaryFile = new File(path);
            WritableWorkbook writableWorkbook;
            NumberFormat numberFormat1000 = new NumberFormat("#,###");
            WritableCellFormat cellFormat1000 = new WritableCellFormat(numberFormat1000);
            WritableCellFormat cellFormatDecimal = new WritableCellFormat(new NumberFormat("#,###.00"));
            if(summaryFile.exists()){
                Workbook existingWorkbook = Workbook.getWorkbook(summaryFile);
                num = existingWorkbook.getNumberOfSheets();
                writableWorkbook = Workbook.createWorkbook(summaryFile,existingWorkbook);
            }
            else{
                writableWorkbook = Workbook.createWorkbook(summaryFile);
            }

            WritableSheet writableSheet;
            WritableSheet existingSheet = writableWorkbook.getSheet(summarySheetName);
            if(existingSheet != null){
                writableSheet = existingSheet;
            }
            else{
                writableSheet = writableWorkbook.createSheet(summarySheetName,num);
            }

            Label label;
            int currentCol = 0;
            int currentRow = 0;

            String[] resultAllColsNames = {"WG","WH","Weight","All Solved"};
            String[] resultAlgoColsNames = {"Success Rate","Depth","Cost","Generated","Expanded","Cpu Time","Wall Time"};
            int allColNum = resultAllColsNames.length;
            int algoColNum = resultAlgoColsNames.length;

            for(int r = 0 ; r < allColNum ; r++){
                label = new Label(currentCol++, currentRow, resultAllColsNames[r]);
                writableSheet.addCell(label);
            }
            for(int r = 0 ; r < algoColNum ; r++){
                for (int i = 0 ; i < algoNum ; i++) {
                    label = new Label(currentCol++, currentRow, SearchAlgorithmArr[i].getName()+" "+resultAlgoColsNames[r]);
                    writableSheet.addCell(label);
                }
            }

            for ( Weights.SingleWeight ws :weights.NATURAL_WEIGHTS) {
                w = ws;
                totalWeight = w.wh / w.wg;
                currentRow++;

                String resultsAlgoColumn[][] = new String[algoNum][];
                System.out.println("Summary "+domainName + "\tweight: wg : " + w.wg + " wh: " + w.wh);
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

                double[] resultArr = new double[currentCol];
                resultArr[0] = w.wg;//WG
                resultArr[1] = w.wh;//WH
                resultArr[2] = totalWeight;// (WH/WG)
                for (int j = 1; j <= stopInstance; j++) {
                    boolean allExist = true;
                    String[] line = new String[algoNum];
                    double[][] tempRes = new double[algoNum][algoColNum];
                    for (int i = 0; i < algoNum; i++) {//check if all algo solved successfully
                        line[i] = resultsAlgoColumn[i][j];
                        String[] lineSplit = line[i].split(",");
                        for (int r = 0; r < algoColNum; r++) {
                            tempRes[i][r] = Double.parseDouble(lineSplit[r + 1]);//from 1 to skip instance number
                        }
                        if (tempRes[i][0] != 1.0) {
                            allExist = false;
                        }
                    }
                    if (allExist) {//save to summary
                        resultArr[allColNum-1] += 1;//found-solved by all
                    }
                    for (int i = 0; i < algoNum; i++) {
                        resultArr[allColNum + i] += tempRes[i][0];//found-solved, success-rate
                    }
                    if (allExist) {//save to summary
                        for (int r = 1; r < algoColNum; r++) {//skip each algo solved
                            for (int i = 0; i < algoNum; i++) {
                                resultArr[allColNum + r * algoNum + i] += tempRes[i][r];//column-value, r+1:skip each algo solved
                            }
                        }
                    }
                }
                // calculate Average
                for (int k = allColNum + algoNum; k < resultArr.length; k++) {
                    resultArr[k] =resultArr[k]/resultArr[allColNum-1];// value/found
                }

                for(int k = 0 ; k < resultArr.length; k++){
                    WritableCellFormat format = cellFormat1000;
                    if(k== allColNum-2) format = cellFormatDecimal;//weight
                    WritableCell cell = new Number(k, currentRow, resultArr[k],format);
                    writableSheet.addCell(cell);
                }
            }

            writableWorkbook.write();
            writableWorkbook.close();
        } catch (IOException e) {
            throw new IOException("File " + summarySheetName + " already Exist, or Folder is missing");
        } catch (RowsExceededException e) {
            e.printStackTrace();
        } catch (WriteException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    private static void EESwalkPath(String savename){
        System.out.println("EESwalkPath " + filePrefix);
        OutputResult output=null;
        try {
            output = new OutputResult(relPath + "results/"+savename + fileEnd, null, -1, -1, null, false, true);
            String headers = "Instance,d,dHat,h,hHat,d*,h*,Weight,EES Generated,DPS Generated,EES Cost,DPS Cost";
            output.writeln(headers);
            System.out.println(headers);

/*            String alpha = outputPath.split("alpha")[1];
            alpha = alpha.split("_")[0];*/

            for ( Weights.SingleWeight ws :weights.NATURAL_WEIGHTS) {
                w = ws;
                String dpPath = outputPath+"DP_" + (int) w.wg + "_" + (int) w.wh + "_"+fileEnd+".csv";
                String eesPath = outputPath+"ees_" + (int) w.wg + "_" + (int) w.wh + "_"+fileEnd+".csv";
                InputStream dpStream = new FileInputStream(new File(dpPath));
                InputStream eesStream = new FileInputStream(new File(eesPath));
                BufferedReader dpReader = new BufferedReader(new InputStreamReader(dpStream));
                BufferedReader eesReader = new BufferedReader(new InputStreamReader(eesStream));
                String dpLine = dpReader.readLine();
                String eesLine = eesReader.readLine();

                for (int instance = startInstance; instance <= stopInstance; instance++) {
//                    System.out.println("Walk path instance "+instance);
                    String instancePath = inputPath + "/optimalOperators"+instance+".in";
                    File optFile = new File(instancePath);
                    if(!optFile.exists()) {
                        System.out.println("[INFO] optimal operator not not found for instance "+instance);
                        continue;
                    }
                    FileInputStream optimalOperatorsStream = new FileInputStream(new File(instancePath));
                    BufferedReader optimalOperatorsReader = new BufferedReader(new InputStreamReader(optimalOperatorsStream));

                    dpLine = dpReader.readLine();
                    eesLine = eesReader.readLine();
                    String dpCost = dpLine.split(",")[3];
                    String eesCost = eesLine.split(",")[3];
                    String dpGenerated = dpLine.split(",")[4];
                    String eesGenerated = eesLine.split(",")[4];
                    double hOPt = Double.parseDouble(dpCost);

                    InputStream is = new FileInputStream(new File(inputPath + "/" + instance + ".in"));

                    Class<?> cl = Class.forName("org.cs4j.core.domains."+domainName);
                    Constructor<?> cons = cl.getConstructor(InputStream.class);
                    SearchDomain domain = (SearchDomain) cons.newInstance(is);
                    for(Map.Entry<String, String> entry : domainParams.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        domain.setAdditionalParameter(key,value);
                    }

//                    SearchDomain domain = new Pancakes(is);
                    EES ees = new EES(domain);

                    SearchDomain.State parentState = domain.initialState();
                    SearchDomain.State childState = null;
                    EES.Node parentNode = ees.createNode(parentState, null, null, null, null);;

                    int dOpt = -1;
                    String optimalOperatorsLine = optimalOperatorsReader.readLine();
                    while (optimalOperatorsLine != null) {
                        dOpt++;
                        optimalOperatorsLine = optimalOperatorsReader.readLine();
                    }

                    optimalOperatorsStream.getChannel().position(0);
                    optimalOperatorsReader = new BufferedReader(new InputStreamReader(optimalOperatorsStream));
                    optimalOperatorsLine = optimalOperatorsReader.readLine();
                    while (dOpt > 0) {
//                    System.out.println(parentState.dumpStateShort());
                        int opPos = Integer.parseInt(optimalOperatorsLine);
                        SearchDomain.Operator op = domain.getOperator(parentState, opPos);
                        childState = domain.applyOperator(parentState, op);
                        hOPt -= op.getCost(childState,parentState);
                        EES.Node childNode = ees.createNode(childState, parentNode, parentState, op, op.reverse(parentState));

                        StringBuilder sb = new StringBuilder();
                        sb.append(instance);
                        sb.append(",");
                        sb.append(childNode.d);
                        sb.append(",");
                        sb.append(childNode.dHat);
                        sb.append(",");
                        sb.append(childNode.h);
                        sb.append(",");
                        sb.append(childNode.hHat);
                        sb.append(",");
                        sb.append(dOpt);
                        sb.append(",");
                        sb.append(hOPt);
                        sb.append(",");
/*                        sb.append(w.wg);
                        sb.append(",");
                        sb.append(w.wh);
                        sb.append(",");*/
                        sb.append(w.wh/w.wg);
                        sb.append(",");
/*                        sb.append(alpha);
                        sb.append(",");*/
                        sb.append(eesGenerated);
                        sb.append(",");
                        sb.append(dpGenerated);
                        sb.append(",");
                        sb.append(eesCost);
                        sb.append(",");
                        sb.append(dpCost);

                        String toPrint = String.valueOf(sb);
                        output.writeln(toPrint);
//                        System.out.println(toPrint);

//                        System.out.println(parentState.dumpStateShort()+" ("+opPos+") "+childState.dumpStateShort());

                        parentState = childState;
                        parentNode = childNode;
                        optimalOperatorsLine = optimalOperatorsReader.readLine();
                        dOpt--;
                    }

                    int opPos = Integer.parseInt(optimalOperatorsLine);
                    SearchDomain.Operator op = domain.getOperator(parentState, opPos);
                    childState = domain.applyOperator(parentState, op);
                    if(!domain.isGoal(childState)){
                        System.out.println("[WARNING] Last state is NOT Goal");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        finally {
            output.close();
        }

    }

    private static void afterSetDomain() throws IOException{
        EESwalkPath("depth-pancakes");
        if(true) return;

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

    public static void main(String[] args) throws IOException, WriteException, BiffException {
//        saveSolutionPathAsInstances();
//        domainName = "GridPathFinding";

        overwriteFile = true;//if false throws error if file exists already
        overwriteSummary = true;
        appendToFile = true;//true: do not calculate instances again if already exist
        useBestFR = false;
        useOracle = false;
        saveSolutionPath = false;
        startInstance = 1;
        stopInstance = 87;
//        summaryName = "unit cost";
//        summaryName = "GAP+W-MD";
        summaryName = "testing";
//        summaryName = "Oracle";
//        summaryName = "Alpha";
//        summaryName = "heavy pancakes";
//        summaryName = "heavy Vacuum";
//        summaryName = "optimal";

        String globalPrefix;
        if(useOracle) globalPrefix = "ORACLE_";
        else globalPrefix = "server_test";

        if(useBestFR)fileEnd = "bestFR";
        else fileEnd = "NoFr";

        String[] domains = {
//            "DockyardRobot",
//            "VacuumRobot",
            "Pancakes",
//            "FifteenPuzzle",
//            "GridPathFinding"
        };

        SearchAlgorithm[] AlgoArr = {
//            new EES2(),
//                new IDAstar(),
//                new BEES(),
//                new EES(1),

//                new WAStar(),
//                new EES(1),
                new DP(),
        };
        SearchAlgorithmArr = AlgoArr;

        for (String dN : domains) {
            domainName = dN;
            domainParams = new HashMap<>();
            switch (domainName) {
                case "FifteenPuzzle": {
                    for(int i = 0 ; i <= 0 ; i++) {
                        double alpha = (double)i;
                        domainParams.put("cost-function", alpha+"");
                        filePrefix = globalPrefix+"alpha" + alpha + "_";  //for cost-function
//                    filePrefix = "";  //for unit costs
                        System.out.println("Solving FifteenPuzzle " + filePrefix);
                        inputPath = relPath + "input/FifteenPuzzle/states15";
//                    inputPath = relPath + "input/FifteenPuzzle/states15InstanceByStep/43";
                        outputPath = relPath + "results/FifteenPuzzle/" + filePrefix;
//                        outputPath = relPath + "results/tests/"+filePrefix;
                        summarySheetName = filePrefix + "FifteenPuzzle";
                        afterSetDomain();
                    }
                    break;
                }
                case "Pancakes": {
                    int[] pancakesNum;
//                    pancakesNum = new int[]{10, 12, 16, 20, 40};
//                    pancakesNum = new int[]{10, 12, 16};
//                    pancakesNum = new int[]{16,20,40};
//                    pancakesNum = new int[]{40};
//                    pancakesNum = new int[]{101};
//                    pancakesNum = new int[]{10};
//                    pancakesNum = new int[]{40};
                    for(int gap=0 ; gap <=0  ; gap++) {
//                        double GAPK = ((double)gap/2);
                        double GAPK = (double)gap;
                        for (int j = 0; j < pancakesNum.length; j++) {
                            int num = pancakesNum[j];
                            filePrefix = globalPrefix+num+"_";

                            domainParams.put("GAP-k", GAPK + "");
                            filePrefix += "GAP-" + GAPK + "_";

/*                            domainParams.put("cost-function", "heavy");
                            filePrefix += "heavy_";*/

                            System.out.println("Solving Pancakes " + num + " " + filePrefix);
                            inputPath = relPath + "input/pancakes/generated-" + num;
                            outputPath = relPath + "results/pancakes/" + num + "/" + filePrefix;
                            summarySheetName = filePrefix + "pancakes";
                            afterSetDomain();
                        }
                    }
                    break;
                }
                case "VacuumRobot": {
                    int[] dirts;
                    dirts = new int[]{10};
//                    dirts = new int[]{5};
//                    int[] dirts = new int[]{5, 10};
                    for(int alpha=0 ; alpha < 2 ; alpha+=2) {
                        for (int j = 0; j < dirts.length; j++) {
                            for(int shrinkTo = dirts[j] ; shrinkTo <= dirts[j] ; shrinkTo+=1){
                                filePrefix = globalPrefix + "";

                                domainParams.put("cost-function", alpha+"");
                                filePrefix += "alpha"+alpha+"_";

                                domainParams.put("shrinkTo", shrinkTo+"");
                                filePrefix += "shrinkTo"+shrinkTo+"_";

                                System.out.println("Solving VacuumRobot "+filePrefix);
                                inputPath = relPath + "input/VacuumRobot/generated-" + dirts[j] + "-dirt";
                                outputPath = relPath + "results/VacuumRobot/" + dirts[j] + "-dirt/" + filePrefix;
                                summarySheetName = "Vacuum-" + dirts[j] + "-a"+alpha+"-s"+shrinkTo;
                                afterSetDomain();
                            }
                        }
                    }
                    break;
                }
                case "DockyardRobot": {
                    filePrefix = "";
                    System.out.println("Solving DockyardRobot "+filePrefix);
                    inputPath = relPath + "input/dockyard-robot-max-edge-2-out-of-place-30/";
                    outputPath = relPath + "results/dockyard-robot-max-edge-2-out-of-place-30/"+filePrefix;
                    summarySheetName = "dockyard-robot";
                    afterSetDomain();
                    break;
                }
                case "GridPathFinding": {
                    String gridName = "brc202d.map";
                    System.out.println("Solving VacuumRobot");
                    inputPath = relPath + "input/GridPathFinding/" + gridName;
                    outputPath = relPath + "results/GridPathFinding/" + gridName;
                    summarySheetName = gridName;
                    filePrefix = globalPrefix+"";
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
