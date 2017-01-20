package org.cs4j.core.collections;

import org.cs4j.core.algorithms.SearchResultImpl;

import java.util.*;

/**
 * Created by Daniel on 08/01/2016.
 */
public class GH_heap<E extends SearchQueueElement> implements SearchQueue<E> {

    private final int key;
    private double IsFocalizedPrecision = Math.pow(10,-13);
    private boolean debug = false;
    private HashMap<Double, Double> countF = new HashMap<>();
    private HashMap<Double, Double> countD = new HashMap<>();
    //    private BinHeap<E> heap;
    private TreeMap<gh_node,ArrayList<E>> tree;
    private TreeMap<gh_node,ArrayList<E>> outOfFocalTree;
    private TreeMap<gh_node,ArrayList<E>> treeF;
    private boolean isFocalized;
    private double fmin;
    private double dmin;
    private boolean isOptimal;
    private double w;
    private gh_node bestNode;
    private ArrayList<E> bestList;
    private ghNodeComparator comparator;
    private int GH_heapSize;
    private Comparator<E> NodePackedComparator;
    private SearchResultImpl result;
    private boolean useD;
    private boolean useFR = false;

//    private boolean withFComparator;

    public GH_heap(double w, int key, double fmin, Comparator<E> NodePackedComparator, SearchResultImpl result, boolean useD, boolean isFocalized) {
        this.w = w;
        this.key = key;
        this.comparator = new ghNodeComparator();
        this.tree = new TreeMap<>(this.comparator);
        if(isFocalized) this.outOfFocalTree = new TreeMap<>(this.comparator);
        if(useFR) this.treeF = new TreeMap<>(new Fcomparator());
        this.isFocalized = isFocalized;
        this.NodePackedComparator = NodePackedComparator;
        this.result =result;
        this.useD = useD;
//        this.heap = new BinHeap<>(new FComparator(), this.key);
//        this.withFComparator = true;
    }

    public void setOptimal(double fmin){
        this.isOptimal = true;
    }

    public double getFmin(){
//        testHat();
        return fmin;
    }

    public double getFminCount(){
        return countF.get(fmin);
    }

    public void add(E e) {
//    testHat();
        if(debug) debugNode(e,"add");
//        if(result.generated % 10000 == 0) System.out.print("\rgenerated(add):"+result.generated);

        count_add(e);
        gh_node node = new gh_node(e);
        TreeMap<gh_node,ArrayList<E>> treeOfNode = (node.inTree ? tree : outOfFocalTree);
/*        if(!node.inTree){
            System.out.println("[INFO] add Node not in Focal!");
        }*/

        ArrayList<E> list;

        if(treeOfNode.containsKey(node)){
            list = treeOfNode.get(node);
        }
        else{
            list = new ArrayList<>();
        }
        e.setIndex(this.key,list.size());
        list.add(e);
        treeOfNode.put(node,list);

        if(useFR) {
            ArrayList<E> listF;
            if (treeF.containsKey(node)) {
                listF = treeF.get(node);
            } else {
                listF = new ArrayList<>();
            }
            listF.add(e);
            treeF.put(node, listF);
        }

        if(node.inTree) {
            if (this.bestNode != null) {
                if (this.comparator.compare(bestNode, node) > 0) {
                    if(debug) debugNode(e,"add2");
                    bestNode = node;
                    bestList = list;
                }
            } else {
                if(debug) debugNode(e,"add3");
                bestNode = node;
                bestList = list;
            }
        }
//    testHat();
    }

    private void count_add(E e) {
        double f = e.getF();
        GH_heapSize++;
        if(countF.containsKey(f))
            countF.put(f,countF.get(f)+1);
        else {
            countF.put(f, 1.0);
            if(!isOptimal) {//fmin might change/decrease
                if(fmin == 0.0) fmin = f;
/*                if (tree.size()+outOfFocalTree.size() == 0) {//tree is empty
                    fmin = f;
                }
                if (fmin > f+ IsFocalizedPrecision) {//might occur due to rounding of f;
                    throw new IllegalArgumentException("[ERROR] fmin:"+fmin+" > f:"+f+", IsFocalizedPrecision:"+ IsFocalizedPrecision +" heuristic might be inconsistent OR adding doubles round error");
                }*/
            }
        }
        if(useD) {
            double fd = e.getD() + e.getDepth();
            if (countD.containsKey(fd))
                countD.put(fd, countD.get(fd) + 1);
            else {
                countD.put(fd, 1.0);
/*                if (!isOptimal) {//fmin might change/decrease
                    if (tree.size() + outOfFocalTree.size() == 0) {//tree is empty
                        dmin = fd;
                    }
                    if (dmin - fd > 0) {//might occur due to rounding of d??? - this should never happen;
                        throw new IllegalArgumentException("[ERROR] dmin>d");
                    }
                }*/
            }
        }
    }

    @Override
    public E poll() {
        E e = bestList.get(0);
        return remove(e);
    }

    @Override
    public E peek() {
        if(bestList == null){
            System.out.println("GH_heap peek error");
            System.out.println(fmin);
            System.out.println(countF);
            System.out.println(countD);
        }
        if(bestList.get(0) == null){
            System.out.println("GH_heap peek error");
        }
        E e = bestList.get(0);
        if(debug) debugNode(e,"peek");
        if(e.getF() > w*fmin){
/*            System.out.println(e);
            System.out.println("\u001B[32m"+"[INFO] This Node is out of focal"+ "\u001B[0m");*/
        }
        return e;
    }

    public E peekF() {
        if(!useFR) throw new IllegalArgumentException("[ERROR] peekF can only be used when useFR = true");
        ArrayList<E> listF = treeF.get(0);
        E e = listF.get(0);
        return e;
    }

    @Override
    public void update(E e) {
        throw new UnsupportedOperationException("Invalid operation for GH_heap, use remove and add instead");
    }

    @Override
    public boolean isEmpty() {
        if(isFocalized) return tree.isEmpty() && outOfFocalTree.isEmpty();
        else return tree.isEmpty();
    }

    public void updateF(E oldNode, E newNode) {
//        test();
/*        gh_node oldPos = new gh_node(oldG,oldH);
        ArrayList<E> oldList = tree.get(oldPos);
        E toRemove = null;
        for (E e:oldList) {
            if(NodePackedComparator.compare(updatedNode,e)==0){
                toRemove = e;
            }
        }
        if(toRemove != null){
            remove(toRemove);
        }
        else{
            System.out.println("can not remove");
        }
        add(updatedNode);*/
//        test();

    }



    private void reorder(){
//        int buckets = tree.size();//for paper debug
//        int nodes = 0;//for paper debug
        TreeMap<gh_node,ArrayList<E>> tempTree = new TreeMap<>(comparator);

        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            gh_node node = entry.getKey();
            ArrayList<E> list = entry.getValue();
//            nodes +=list.size();
            it.remove();
            node.calcPotential();
            tempTree.put(node,list);
        }

        if(isFocalized) {
            TreeMap<gh_node, ArrayList<E>> tempOutOfFocalTreeTree = new TreeMap<>(comparator);
            for (Iterator<Map.Entry<gh_node, ArrayList<E>>> it = outOfFocalTree.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<gh_node, ArrayList<E>> entry = it.next();
                gh_node node = entry.getKey();
                ArrayList<E> list = entry.getValue();
                it.remove();
                if (debug) debugNode(node, "reorder");
                node.calcPotential();
                if (debug) debugNode(node, "reorder1");
                if (node.inTree)
                    tempTree.put(node, list);
                else
                    tempOutOfFocalTreeTree.put(node, list);
            }
            outOfFocalTree = tempOutOfFocalTreeTree;
            if(bestNode == null){
                bestNode = tree.firstKey();
                bestList = tree.get(bestNode);
            }
        }
        tree = tempTree;
    }

    @Override
    public int size() {
        return GH_heapSize;
    }

    @Override
    public void clear() {
        countF.clear();
        countD.clear();
        tree.clear();
        outOfFocalTree.clear();
        bestNode = null;
        bestList.clear();
    }

    @Override
    public E remove(E e) {
//        testHat();
        if(debug) debugNode(e,"remove");
        gh_node node = new gh_node(e);
        TreeMap<gh_node,ArrayList<E>> treeOfNode = (node.inTree ? tree : outOfFocalTree);
        ArrayList<E> list = treeOfNode.get(node);

/*        if(!node.inTree){
            System.out.println("[INFO] remove Node not in Focal!");
        }*/

        if(list == null) {
            TreeMap<gh_node,ArrayList<E>> treeOfNode2 = (!node.inTree ? tree : outOfFocalTree);
            ArrayList<E> list2 = treeOfNode2.get(node);
            if(list2 == null){
                System.out.println("\u001B[31m"+"[WARNING] This Node is in NO list"+ "\u001B[0m");
            }
            else{
                System.out.println("\u001B[31m"+"[WARNING] This Node is in the WRONG list"+ "\u001B[0m");
            }
            System.out.println("\ne:"+e);
            System.out.println("tree size:"+treeOfNode.size());
        }
        if(list.isEmpty())
            System.out.println("\u001B[31m"+"[WARNING] list is empty, can not remove"+ "\u001B[0m");
        list.remove(e);
        e.setIndex(this.key,-1);

        if(list.isEmpty()){
            treeOfNode.remove(node);
            if(this.comparator.compare(node,bestNode) <= 0 && node.inTree){
                if(tree.isEmpty()){
                    if(debug) debugNode(e,"remove2");
                    bestNode = null;
                    bestList = null;
//                    System.out.println("[WARNING] Tree is empty");
                }
                else {
                    if(debug) debugNode(e,"remove3");
                    bestNode = tree.firstKey();
                    bestList = tree.get(bestNode);
                    if(debug) debugNode(e,"remove4");
                }
            }
        }
        else{
            treeOfNode.put(node,list);
        }

        if(useFR) {
            ArrayList<E> listF = treeF.get(node);
            if (listF.isEmpty())
                System.out.println("\u001B[31m" + "[WARNING] listF is empty, can not remove" + "\u001B[0m");
            listF.remove(e);
            if (listF.isEmpty())
                treeF.remove(node);
        }

        count_remove(e);
        if(bestNode == null) {
            System.out.println("[WARNING] bestNode == null");
            reorder();
        }
//        testHat();
        return e;
    }

    private void count_remove(E e){
        double f = e.getF();
        double fd = e.getD()+e.getDepth();
        GH_heapSize--;
        if(!countF.containsKey(f)){
            countF.put(f,0.0);
        }
        countF.put(f,countF.get(f)-1);
        if(countF.get(f)==0){
            countF.remove(f);
            if(!isOptimal) {//fmin might change/increase
                if (f == fmin && (tree.size()> 0 || outOfFocalTree.size() > 0)) {//find next lowest
                    fmin = Integer.MAX_VALUE;
                    Iterator it = countF.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        double key = (double) pair.getKey();
                        if (fmin >= key) {
                            fmin = key;
                        }
                    }
                    reorder();
                }
            }
        }
        if(useD) {
            if (!countD.containsKey(fd)) {
                countD.put(fd, 0.0);
            }
            countD.put(fd, countD.get(fd) - 1);
            if (countD.get(fd) == 0) {
                countD.remove(fd);
                if (!isOptimal) {//fmin might change/increase
                    if (fd == dmin && tree.size() + outOfFocalTree.size() > 0) {//find next lowest
                        dmin = Integer.MAX_VALUE;
                        Iterator it = countD.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            double key = (double) pair.getKey();
                            if (dmin >= key) {
                                dmin = key;
                            }
                        }
                        reorder();
                    }
                }
            }
        }
    }

    @Override
    public int getKey() {
        return this.key;
    }

    public void testHat(){
/*        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            ArrayList<E> list = entry.getValue();
            for(int i=list.size()-1 ; i>=0 ; i--){
                double Val = list.get(i).getF();
                if(Val < fmin){
                    System.out.println("test Failed! Val < fmin");
                }
                countF.put(Val,countF.get(Val)-1);
                if(countF.get(Val)<0){
                    System.out.println("test failed! countF.get("+Val+")<0");
                }
            }
        }

        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            ArrayList<E> list = entry.getValue();
            for(int i=list.size()-1 ; i>=0 ; i--){
                double Val = list.get(i).getF();
                if(Val < fmin){
                    System.out.println("test Failed! Val < fmin");
                }
                countF.put(Val,countF.get(Val)+list.size());
            }
        }*/
    }

    private void debugNode(E e, String from){
        gh_node node = new gh_node(e);
        debugNode(node,from);
    }
    private void debugNode(gh_node node, String from){
        if(node.f + IsFocalizedPrecision < fmin){
            System.out.println("++++++++++++++++++++++");
            System.out.println("[INFO] fmin decreased:"+from);
            System.out.println(fmin);
            System.out.println(node.f);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }
        if((node.d == 15.2 && node.h == 3.6734)
                || (node.d == 15.1 && node.h == 3.6702))
        {
            System.out.println("++++++++++++++++++++++");
            System.out.println("[INFO] "+from+":");
            System.out.println(node);
            System.out.println(fmin);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }

        if(bestNode != null && bestNode.inTree == false && bestList != null){
            System.out.println("++++++++++++++++++++++");
            System.out.println("[INFO] Best node not in Tree, from:"+from);
            System.out.println(bestNode);
            System.out.println(fmin);
            System.out.println(bestNode.f-w*fmin);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }

        String whatWentWrong = "";
//        if(bestList == null) whatWentWrong+="bestList == null,";
//        if(bestNode == null) whatWentWrong+="bestNode == null,";
//        if(tree.isEmpty()) whatWentWrong+="tree.isEmpty(),";
//        if(outOfFocalTree.isEmpty()) whatWentWrong+="outOfFocalTree.isEmpty(),";
        if(whatWentWrong != ""){
            System.out.println("++++++++++++++++++++++");
            System.out.println("SOMETHING WENT WRONG WITH THIS NODE");
            System.out.println("What went wrong?:"+whatWentWrong);
            System.out.println("[INFO] "+from+":");
            System.out.println(node);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }
    }

    private void test(){
        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            ArrayList<E> list = entry.getValue();
            double Val = list.get(0).getF();
            if(Val < fmin){
                System.out.println("test Failed! Val < fmin:tree");
            }
            countF.put(Val,countF.get(Val)-list.size());
            if(countF.get(Val)<0){
                System.out.println("test failed! countF.get("+Val+")<0");
            }
        }

        if(isFocalized) {
            for (Iterator<Map.Entry<gh_node, ArrayList<E>>> it = outOfFocalTree.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<gh_node, ArrayList<E>> entry = it.next();
                ArrayList<E> list = entry.getValue();
                double Val = list.get(0).getF();
                if (Val < fmin) {
                    System.out.println("test Failed! Val < fmin:outOfFocalTree");
                }
                countF.put(Val, countF.get(Val) - list.size());
                if (countF.get(Val) < 0) {
                    System.out.println("test failed! countF.get(" + Val + ")<0 : outOfFocalTree");
                }
            }
        }

        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            ArrayList<E> list = entry.getValue();
            double Val = list.get(0).getF();
            if(Val < fmin){
                System.out.println("test Failed! Val < fmin");
            }
            countF.put(Val,countF.get(Val)+list.size());
        }
    }


    private final class gh_node{
        double g;
        double h;
        double potential;

        double depth;
        double hHat;
        double d;
        double dHat;

        double f;

        boolean inTree = true;
        double cost;
        double divisor;

        @Override
        public String toString() {
            return "gh_node{" +
                    "g=" + g +
                    ", h=" + h +
                    ", depth=" + depth +
                    ", d=" + d +
                    ", f=" + f +
                    ", inTree=" + inTree +
                    ", potential=" + potential +
                    ", hHat=" + hHat +
                    ", dHat=" + dHat +
                    '}';
        }

        public gh_node(E e) {
            this.f = e.getF();

            this.g = e.getG();
            this.h = e.getH();
            this.depth = e.getDepth();
            this.hHat = e.getHhat();
            this.d = e.getD();
            this.dHat = e.getDhat();

            calcPotential();
        }

        public void calcPotential(){

            if(isFocalized) inTree = (this.f+ IsFocalizedPrecision <= w*fmin ? true : false);

            if(this.h == 0 || this.d == 0){
                this.potential = Double.MAX_VALUE;
            }
            else{
                if(useD){
                    this.potential = (w*dmin-this.depth)/this.d;
                    this.cost = this.depth;
                    this.divisor = this.d;
                }
                else{
                    this.potential = (w*fmin-this.g)/this.h;
                    this.cost = this.g;
                    this.divisor = this.h;
                }
            }
        }

    }

    /**
     * The nodes comparator class
     */
    protected final class ghNodeComparator implements Comparator<gh_node> {

        @Override
        public int compare(final gh_node a, final gh_node b) {
            // First compare by potential (bigger is preferred), then by f (smaller is preferred), then by g (smaller is preferred)
            if (a.potential > b.potential) return -1;
            if (a.potential < b.potential) return 1;

            if (a.cost < b.cost) return -1;
            if (a.cost > b.cost) return 1;

            if (a.divisor < b.divisor) return -1;
            if (a.divisor > b.divisor) return 1;

            // From here on it is a tiebreak for cases where we have focal and non-focal nodes
            if (a.f < b.f) return -1;
            if (a.f > b.f) return 1;

            if (a.h < b.h) return -1;
            if (a.h > b.h) return 1;

            if (a.g < b.g) return -1;
            if (a.g > b.g) return 1;

            if (a.inTree != b.inTree) return 1;

            return 0;
        }
    }

    /**
     * The nodes comparator class based of F value
     */
    protected final class Fcomparator implements Comparator<gh_node> {

        @Override
        public int compare(final gh_node a, final gh_node b) {
            if (a.f < b.f) return -1;
            if (a.f > b.f) return 1;

            if (a.h < b.h) return -1;
            if (a.h > b.h) return 1;

            return 0;
        }
    }

}
