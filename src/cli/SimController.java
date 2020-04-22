package cli;

import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menu.Destination;
import menu.Meal;
import napsack.Knapsack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import simulation.Fifo;
import simulation.Settings;
import simulation.Results;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SimController {
    File nameFile; //File that contains a list of names
    ArrayList<String> names; //Stores the list of names
    File ordersFile; //xml file that saves the orders
    private static Settings settings; //stores the settings
    int MINUTES_IN_SIM = 240; //The number of minutes in the simulation
    private static ArrayList<Results> aggregatedResultsFIFO; //The results from all 50 simulations
    private static ArrayList<Results> aggregatedResultsKnapsack;
    int NUMBER_OF_SIMULATIONS = 50; //The number of simulations
    public static boolean simInProgress = false; //If the simulation is in progress
    public static boolean simRan; //If a simulation has ran
    static final FileChooser fileChooser = new FileChooser(); //Used for choosing a file
    private static SimulationThread simThread; //The thread that runs the simulation
    private static Button btn; //Button associated with running the simulation


    //pointer to the single instance of SimController
    private static SimController single_instance = null;


    private SimController() {
        //Results for the two algorithms
        aggregatedResultsFIFO = new ArrayList<>();
        aggregatedResultsKnapsack = new ArrayList<>();

        names = new ArrayList<>(); //List of names
        simRan = false; //The simulation hasn't been run yet
        try {
            //Names.txt was generated by Dominic Tarr
            nameFile = new File("Names.txt"); //open the names file

            //Seed the arraylist with names in the file
            Scanner s = new Scanner(nameFile);
            while (s.hasNext()) {
                names.add(s.next());
            }
            s.close();

        } catch (Exception e) {
            System.out.println((e.getMessage()));
        }

        settings = settings.getInstance(); //Get the settings class
    }

    //Singleton creator
    public static SimController getInstance() {
        if (single_instance == null) {
            single_instance = new SimController();
        }
        return single_instance;
    }

    /**
     * Generates the orders for the four hour simulation
     * The orders are stored in Orders.xml
     */
    public void generateOrders() {
        Random random = new Random(); //new random generator

        int minutesInSim = 240; //The number of minutes in the simulation
        int curMin = 0; //The current minute the simulation is in
        int randName; //random integer for what name to choose the ArrayList
        ArrayList<Integer> ordersPerHour = Settings.getOrderDistribution(); //Get the ordersPerHour
        ArrayList<Destination> map = Settings.getMap(); //Map of the given campus
        Meal m; //Current meal being ordered
        Destination d; //The destination to be delivered to


        //open the orders file
        try {
            ordersFile = new File("Orders.xml");
            FileWriter fileWriter = new FileWriter(ordersFile, false); //clear out the orders file
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("<listOfOrders>");
            printWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //Get the adjusted probability of an order coming in
        double chanceOfOrderPerM = adjustProbability(ordersPerHour.get(0));

        //For each minute in the simulation
        while (curMin < MINUTES_IN_SIM) {

            if (random.nextDouble() < chanceOfOrderPerM) { //If the probability is low enough, generate an order
                //Get a random name
                randName = random.nextInt(names.size());

                //Get a random meal based on the distribution
                m = randomMeal(random.nextDouble());

                //Get a random destination
                d = map.get(random.nextInt(map.size()));

                if (m != null) {
                    //Create a new order
                    PlacedOrder ord = new PlacedOrder(curMin, d, names.get(randName), m);

                    //Add the order to the xml file
                    ord.addToXML(ordersFile);
                } else {
                    System.out.println("randomMeal is broken\n");
                }//m!=null

            } else { //If an order is not generated
                curMin++;

                //Adjust the probability if we shift to a new hour
                if (curMin % 60 == 0 && curMin < MINUTES_IN_SIM) {
                    chanceOfOrderPerM = adjustProbability(ordersPerHour.get(curMin / 60));
                }
            }


        }

        //Add the xml closing for the orders file
        try {
            ordersFile = new File("Orders.xml"); //open the orders file
            FileWriter fileWriter = new FileWriter(ordersFile, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("</listOfOrders>");
            printWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Run the FIFO and Knapsack algorithms
     */
    public void runAlgorithms() {
        ArrayList<PlacedOrder> allOrders = getXMLOrders(); //All the xml orders placed
        //System.out.println("Total number of orders: " + allOrders.size());
        Results results = new Results(); //Save the results
        TSPResult tspResult;
        ArrayList<Destination> deliveryOrder;

        //Initialize the knapsack and FIFO algorithms
        Knapsack n = new Knapsack(allOrders);
        Fifo f = new Fifo(allOrders);

        int loadMealTime = 0; //In case, the loadMealTime gets adjusted
        double elapsedTime = 2.5; //how far into the simulation are we
        boolean ordersStillToProcess = true; //If there are orders still to process
        double droneSpeed = 25 * 5280 / 60; //Flight speed of the drone in ft/minute
        int droneDeliveryNumber = 1; //Keeps track of what delivery it is
        PlacedOrder currentOrder;

        try {


            //Knapsack
            while (ordersStillToProcess && droneDeliveryNumber < 100) {
                ArrayList<PlacedOrder> droneRun = n.packDrone(elapsedTime); //Get what is on the current drone
                if (droneRun == null) { //Finished delivering orders
                    ordersStillToProcess = false;
                } else {
                    elapsedTime += n.getTimeSkipped() + loadMealTime; //Calculate the current time

                    //Find how long the delivery takes
                    tspResult = TSP(droneRun);
                    deliveryOrder = tspResult.getDeliveryOrder();

                    //Deliver the orders and process the results
                    for (int i = 0; i < deliveryOrder.size(); i++) {
                        currentOrder = findOrderOnDrone(droneRun, deliveryOrder.get(i));
                        elapsedTime += (deliveryOrder.get(i).getDistToTravelTo() / droneSpeed) + .5;
                        results.processSingleDelivery(elapsedTime, currentOrder);

                    }

                    //Return home
                    elapsedTime += deliveryOrder.get(0).getDist()/droneSpeed;

                    //System.out.println("Time that delivery " + droneDeliveryNumber+ " arrived with " + droneRun.size() + " deliveries: " + elapsedTime);

                    //Turnaround time
                    elapsedTime += 2.5;
                }
                droneDeliveryNumber++;
            }
            results.getFinalResults("Knapsack");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        aggregatedResultsKnapsack.add(results); //store the results

        try {
            //Reset the variables for FIFO
            results = new Results();
            elapsedTime = 2.5;
            ordersStillToProcess = true;
            droneDeliveryNumber = 1;

            //FIFO
            while (ordersStillToProcess) {
                ArrayList<PlacedOrder> droneRun = f.packDrone(elapsedTime); //Get what is on the current drone

                if (droneRun == null) { //No more orders to be delivered
                    ordersStillToProcess = false;
                } else {
                    elapsedTime += f.getTimeSkipped() + loadMealTime; //Calculate the current time

                    //Find how long the delivery takes
                    tspResult = TSP(droneRun);
                    deliveryOrder = tspResult.getDeliveryOrder();

                    //Deliver the orders and process the results
                    for (int i = 0; i < deliveryOrder.size(); i++) {

                        currentOrder = findOrderOnDrone(droneRun, deliveryOrder.get(i));
                        elapsedTime += (deliveryOrder.get(i).getDistToTravelTo() / droneSpeed) + .5;
                        results.processSingleDelivery(elapsedTime, currentOrder);

                    }
                    //System.out.println(elapsedTime);

                    //Return home
                    elapsedTime += deliveryOrder.get(0).getDist()/droneSpeed;

                    //System.out.println("Time that delivery " + droneDeliveryNumber+ " arrived with " + droneRun.size() + " deliveries: " + elapsedTime);

                    //Turnaround time
                    elapsedTime += 2.5;
                }
                droneDeliveryNumber++;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        results.getFinalResults("FIFO");
        aggregatedResultsFIFO.add(results); //Store the results

        simRan = true;

    }

    /**
     * Get all of the order in the xml file
     *
     * @return ArrayList of orders that were placed
     */
    private ArrayList<PlacedOrder> getXMLOrders() {
        ArrayList<PlacedOrder> placedOrders = new ArrayList<>(); //all the placed orders

            //List of potential meals so that to compare the name in the xml file with
            List<Meal> meals = Settings.getMeals();

            try {
                //XML file reading initialization
                File orderFile = new File("Orders.xml");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newDefaultInstance();
                DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
                Document document = documentBuilder.parse(orderFile);
                document.getDocumentElement().normalize();

                //Create a node list of all the elements
                NodeList nodeList = document.getElementsByTagName("order");

                //Variables that go in a placed order
                String cname, dname, mealString, mname;
                int ordTime;
                Destination dest;
                Meal chosenMeal = new Meal("Temp", 0);
                PlacedOrder oneOrder;

                //For each element in the file
                for (int temp = 0; temp < nodeList.getLength(); temp++) {

                    Node node = nodeList.item(temp);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        //Get the customer's name
                        cname = e.getElementsByTagName("cname").item(0).getTextContent();

                        //Get the time of the order
                        ordTime = Integer.parseInt(e.getElementsByTagName("ordTime").item(0).getTextContent());

                        //Get the destination
                        dname = e.getElementsByTagName("dest").item(0).getTextContent();
                        Scanner destScanner = new Scanner(dname);
                        destScanner.useDelimiter("\t");
                        dest = new Destination(destScanner.next(), destScanner.nextInt(), destScanner.nextInt());
                        destScanner.close();

                        //Get the name of the meal in the file and check it against the potential meals and find
                        //the right meal
                        mealString = e.getElementsByTagName("meal").item(0).getTextContent();
                        Scanner mealScanner = new Scanner(mealString);
                        mealScanner.useDelimiter(":");
                        mname = mealScanner.next();
                        mealScanner.close();
                        for (int m = 0; m < meals.size(); m++) {
                            if (mname.equals(meals.get(m).getName())) {
                                chosenMeal = meals.get(m);
                                break;
                            }
                        }


                        //Create the order and it to the arrayList of all the orders
                        oneOrder = new PlacedOrder(ordTime, dest, cname, chosenMeal);
                        placedOrders.add(oneOrder);
                    }
                }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
        return placedOrders;
    }


    /**
     * Returns a random meal based on the distribution
     *
     * @param rand A random double between 0 and 1
     * @return The meal that was randomly selected
     */
    private Meal randomMeal(double rand) {
        double counter = 0; //Keeps track of the distribution through the loop

        //Store the list of meals
        List<Meal> lm = Settings.getMeals();

        //Iterate to where the random number points to in the distribution
        for (int i = 0; i < lm.size(); i++) {
            counter += lm.get(i).getDistribution();
            if (rand < counter) {
                return lm.get(i); //return the meal selected
            }
        }

        //If it returns null the distribution invalid
        return null;
    }

    /**
     * Calculates the least cost distance to complete the delivery cycle
     *
     * @param orders ArrayList of placed orders to travel and deliver food to
     * @return The least cost distance to complete the delivery cycle
     */
    public TSPResult TSP(ArrayList<PlacedOrder> orders) {
        ArrayList<Destination> locations = new ArrayList<>(); //The destination of each order

        //The place where the drone leaves and returns to
        Destination home = new Destination("Home", 0, 0, 0);


        //Seed the location ArrayList with each destination from the order list
        for (int i = 0; i < orders.size(); i++) {
            locations.add(orders.get(i).getDest());
        }

        //Call the recursive function which does the brunt work of the algorithm
        return recursiveTSP(locations, home);
    }

    /**
     * Does the brunt work of the TSP. Should run in O(n^2 * 2^n) which is much better than O(n!)
     *
     * @param locations The locations yet to be visited
     * @param lastDest  The last destination the algorithm visited
     * @return The least cost distance to visit all the locations and return
     */
    private TSPResult recursiveTSP(ArrayList<Destination> locations, Destination lastDest) {
        TSPResult finalResult = new TSPResult();
        if (locations.size() == 0) { //base case
            return new TSPResult(lastDest.getDist()); //return to home
        } else {
            double min = Double.MAX_VALUE; //minimum travel distance for given depth in the recursion tree

            //For each possible set of locations
            for (int d = 0; d < locations.size(); d++) {
                Destination newDest = locations.remove(0); //remove the destination

                double distanceBetween = lastDest.distanceBetween(newDest);
                TSPResult newResult = recursiveTSP(locations, newDest);

                newResult.addDistance(distanceBetween);
                //recurse: it finds the fastest route in the subset and then adds the distance between the current
                //          point and the subset. It then takes the minimum at the level so to help in the recursion in
                //          the level above it
                if (newResult.getTotalDistance() < min) {
                    min = newResult.getTotalDistance();
                    Destination temp = new Destination(newDest);
                    temp.setDistToTravelTo(distanceBetween);
                    newResult.addStop(temp);
                    finalResult = newResult;


                }

                //Add back the location because each iteration of the loop on the same level should have the same
                //number of locations to check
                locations.add(newDest);
            }
            //return the shortest distance found in the given level
            return finalResult;
        }

    }

    /**
     * Adjust the probability so that generateOrders produces the correct number of orders
     * even though multiple orders can come in a single minute
     *
     * @param ordersPerHour The number of orders per this hour
     * @return The adjusted probability
     */
    private double adjustProbability(int ordersPerHour) {
        //The probability that we would be shooting for if we were not allowing multiple orders to
        //come in the same minute
        double idealProbability = ordersPerHour / 60.0;

        double newProbability = idealProbability; //The new probability that we will use

        //The sum of the geometric series that is used to find what the input probability will correspond
        //to given that we can have multiple orders per minutes
        double extrapolatedProbability = Double.MAX_VALUE;

        //Keep looping until we lower the newProbability enough so that it will result in the correct
        //number of orders per hours
        while (extrapolatedProbability > idealProbability) {
            newProbability -= .01;
            extrapolatedProbability = newProbability / (1 - newProbability); //Sum of a geometric series
        }

        return newProbability;
    }

    public static ArrayList<Results> getAggregatedResultsFIFO() {
        return aggregatedResultsFIFO;
    }

    public static ArrayList<Results> getAggregatedResultsKnapsack() {
        return aggregatedResultsKnapsack;
    }

    /*public static Settings getSettings() {
        if (single_instance == null) {
            single_instance = new Settings();
        }
        return settings;
    }*/

    public int getNUMBER_OF_SIMULATIONS() {
        return NUMBER_OF_SIMULATIONS;
    }

    /**
     * Calculate the average time among all the runs
     * @param aggregatedResults ArrayList of Results
     * @return the average time
     */
    public static double getAggregatedAvgTime(ArrayList<Results> aggregatedResults) {
        double sum = 0;
        for (int i = 0; i < aggregatedResults.size(); i++) {
            sum += aggregatedResults.get(i).getAvgTime();
        }
        return sum / aggregatedResults.size();
    }

    /**
     * Calculate the worst time among all the runs
     * @param aggregatedResults ArrayList of Results
     * @return The worst time
     */
    public static double getAggregatedWorstTime(ArrayList<Results> aggregatedResults) {
        double worst = Double.MIN_VALUE;
        for (int i = 0; i < aggregatedResults.size(); i++) {
            if (aggregatedResults.get(i).getWorstTime() > worst) {
                worst = aggregatedResults.get(i).getWorstTime();
            }

        }
        return worst;
    }

    public static String exportResults(ArrayList<Results> resultsFifo, ArrayList<Results> resultsKnapsack) {
        String out = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" +
                "<data-set>\n\t" + "<record>\n\t\t" + 
                "<Simulation_Number>1</Simulation_Number>\n\t\t" +
                "<Fifo_Average_Time>6.663998369015784</Fifo_Average_Time>\n\t\t" +
                "<Fifo_Worst_Time>12.402527638089623</Fifo_Worst_Time>\n\t\t" +
                "<Knapsack_Average_Time>5.6679585828477075</Knapsack_Average_Time>\n\t\t" +
                "<Knapsack_Worst_Time>12.12398691955238</Knapsack_Worst_Time>\n\t" +
                "</record>";
        for (int i = 0; i < resultsFifo.size(); i++) {
            out += "\n\t<record>\n\t\t";
            out += "<Simulation_Number>" + (i + 1) + "</Simulation_Number>\n\t\t";
            out += "<Fifo_Average_Time>" + resultsFifo.get(i).getAvgTime() + "</Fifo_Average_Time>\n\t\t";
            out += "<Fifo_Worst_Time>" + resultsFifo.get(i).getWorstTime() + "</Fifo_Worst_Time>\n\t\t";
            out += "<Knapsack_Average_Time>" + resultsKnapsack.get(i).getAvgTime() + "</Knapsack_Average_Time>\n\t\t";
            out += "<Knapsack_Worst_Time>" + resultsKnapsack.get(i).getWorstTime() + "</Knapsack_Worst_Time>\n\t";
            out += "</record>";
        }
        out += "\n</data-set>";
        return out;
    }

    public boolean hasResults() {
        return simRan;
    }

    public static boolean exportResults(Stage stage) {
        fileChooser.setTitle("Export Settings");
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            //TODO: pipe this string into the file
            try {
                FileWriter fw = new FileWriter(file, false);
                PrintWriter pw = new PrintWriter(fw);
                pw.println(exportResults(getAggregatedResultsFIFO(), getAggregatedResultsKnapsack()));

                fw.close();
                pw.close();
            } catch (IOException exception) {
                exception.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    public static void setCurrentButton(Button button) {
        btn = button;
    }

    public static Button getCurrentButton() {
        return btn;
    }

    public static void runSimulations() {
        simThread = new SimulationThread();
        simThread.run();
    }

    private static PlacedOrder findOrderOnDrone(ArrayList<PlacedOrder> drone, Destination destination) {
        for (int i = 0; i < drone.size(); i++) {
            if (drone.get(i).getDest().equals(destination)) {
                return drone.remove(i);
            }
        }
        System.out.println(drone.get(0).getDest().getDestName());
        System.out.println("ERROR in findOrderOnDrone");
        return null;
    }

}
