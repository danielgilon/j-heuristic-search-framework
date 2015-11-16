package org.cs4j.core.mains;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.domains.*;

import java.io.*;

/**
 * Created by sepetnit on 11/10/2015.
 *
 */
public class DomainsCreation {

    /*******************************************************************************************************************
     * Private  static methods : Domains creation
     ******************************************************************************************************************/

    public static SearchDomain createGridPathFindingInstanceFromAutomaticallyGenerated(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("input/gridpathfinding/generated/maze512-1-6.map/" + instance));
        return new GridPathFinding(is);
    }

    public static SearchDomain createGridPathFindingInstanceFromAutomaticallyGeneratedWithTDH(
            String instance, int pivotsCount) throws IOException {
        //String mapFileName = "input/gridpathfinding/generated/brc202d.map";
        String mapFileName = "input/gridpathfinding/generated/maze512-1-6.map";
        //String pivotsFileName = "input/gridpathfinding/raw/maps/" + new File(mapFileName).getName() + ".pivots.pdb";
        String pivotsFileName = "input/gridpathfinding/raw/mazes/maze1/_maze512-1-6-100.map.pivots.pdb";
        InputStream is = new FileInputStream(new File(mapFileName + "/" + instance));
        GridPathFinding problem = new GridPathFinding(is);
        problem.setAdditionalParameter("heuristic", "tdh-furthest");
        //problem.setAdditionalParameter("heuristic", "tdh-furthest-md-prob-50");
        problem.setAdditionalParameter("pivots-distances-db-file", pivotsFileName);
        problem.setAdditionalParameter("pivots-count", pivotsCount + "");
        return problem;
    }

    public static SearchDomain createGridPathFindingInstanceFromAutomaticallyGeneratedWithTDH(SearchDomain previous,
                                                                                              String instance,
                                                                                              int pivotsCount)
            throws IOException {
        //String mapFileName = "input/gridpathfinding/generated/brc202d.map";
        String mapFileName = "input/gridpathfinding/generated/maze512-1-6.map";
        InputStream is = new FileInputStream(new File(mapFileName, instance));
        GridPathFinding problem = new GridPathFinding((GridPathFinding)previous, is);
        // Change the number of pivots
        problem.setAdditionalParameter("pivots-count", pivotsCount + "");
        return problem;
    }

    // The k is for GAP-k heuristic setting
    public static SearchDomain createPancakesInstanceFromAutomaticallyGenerated(int size, String instance, int k) throws FileNotFoundException {
        Pancakes toReturn = null;
        String filename = "input/pancakes/generated-" + size + "/" + instance;
        try {
            InputStream is = new FileInputStream(new File(filename));
            toReturn = new Pancakes(is);
            toReturn.setAdditionalParameter("GAP-k", k + "");

        } catch (FileNotFoundException e) {
            System.out.println("[WARNING] File " + filename + " not found");
        }
        return toReturn;
    }

    public static SearchDomain createPancakesInstanceFromAutomaticallyGenerated(String instance) throws FileNotFoundException {
        String filename = "input/pancakes/generated-10/" + instance;
        try {
            InputStream is = new FileInputStream(new File(filename));
            return new Pancakes(is);
        } catch (FileNotFoundException e) {
            System.out.println("[WARNING] File " + filename + " not found");
            return null;
        }
    }

    public static SearchDomain createVacuumRobotInstanceFromAutomaticallyGenerated(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("input/vacuumrobot/generated/" + instance));
        return new VacuumRobot(is);
    }

    public static SearchDomain create15PuzzleInstanceFromKorfInstances(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("input/fifteenpuzzle/korf100/" + instance));
        return new FifteenPuzzle(is);
    }

    public static SearchDomain createDockyardRobotInstanceFromAutomaticallyGenerated(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("input/dockyardrobot/generated-max-edge-2-out-of-place-30/" + instance));
        return new DockyardRobot(is);
    }
}
