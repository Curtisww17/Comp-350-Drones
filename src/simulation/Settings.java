package simulation;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menu.Destination;
import menu.FoodItem;
import menu.Meal;

import java.io.*;
import java.util.*;

public class Settings {

    private static ArrayList<Integer> orderDistribution;
    private static ArrayList<Destination> map;
    private static Set<FoodItem> foods;
    private static List<Meal> meals;
    private File settingsFile;
    private static final String defaultSettingsPath = "src/simulation/DefaultSettings.txt";
    static final FileChooser fileChooser = new FileChooser();
    private static Settings instance = new Settings();

    private static int droneCapacity = 192;

    private Settings() {
        map = new ArrayList<>();
        foods = new HashSet<>();
        meals = new ArrayList<>();
        orderDistribution = new ArrayList<>();

        // imports the default settings
        parseSettings(new File(defaultSettingsPath));
    }

    // Getters ------------------------------------------------------------------
    public static Settings getInstance() {
        return instance;
    }

    public static Set<FoodItem> getFoods() {
        return foods;
    }

    public static List<Meal> getMeals() {
        return meals;
    }

    public static ArrayList<Integer> getOrderDistribution() {
        return orderDistribution;
    }

    public static ArrayList<Destination> getMap() { return map; }

    // Food Items----------------------------------------------------------------

    public static String verifySettings() {
        if (!verifyFoodItems()) {
            return "Incorrect Food Items";
        }
        if (!verifyMeals()) {
            return "Incorrect Meal Items";
        }
        if (!verifyDistribution()) {
            return "Incorect Order Distribution";
        }
        return "";
    }


    /**
     * Adds the provided food item
     *
     * @param fooditem new food item
     * @return true if settings are still valid
     */
    public static boolean addFoodItem(FoodItem fooditem) {
        foods.add(fooditem);
        return verifyFoodItems();
    }

    /**
     * Edits food item by removing the old object and adding the new object.
     *
     * @param updatedItem Modified Food Item
     * @return true if foodItems are valid. false otherwise
     */
    public static boolean editFoodItem(FoodItem updatedItem) {
        foods.remove(updatedItem);
        foods.add(updatedItem);
        return verifyFoodItems();
    }

    /**
     * removes foodItem
     *
     * @param foodItem object being removed
     * @return true if settings are still valid. false otherwise
     */
    public static boolean removeFoodItem(FoodItem foodItem) {
        foods.remove(foodItem);
        for (Meal meal : meals) {
            meal.removeFoodItem(foodItem);
        }
        return !foods.isEmpty();
    }

    public static void updateMeals(FoodItem foodItem) {
        for (Meal meal : meals) {
            meal.calcWeight();
        }
    }

    /**
     * TODO: finish this method
     * checks foodItems for correctness
     *
     * @return true if settings are valid. false otherwise
     */
    private static boolean verifyFoodItems() {
        if (foods.size() == 0) {
            return false;
        }
        for (FoodItem item : foods) {
             if (item.getWeight() > droneCapacity) {
                 System.out.println("over weight");
                 return false;
             }
        }
        return true;
    }

    // Meal Items----------------------------------------------------------------

    /**
     * adds provided meal
     *
     * @param meal meal being added
     * @return true if meals are all valid
     */
    public static boolean addMeal(Meal meal) {
        meals.add(meal);
        return verifyMeals();
    }

    /**
     * edits a meal by removing the old and adding the edited meal object
     *
     * @param editedMeal modified meal
     * @return true if meals are all valid
     */
    public static boolean editMeal(Meal editedMeal) {
        meals.remove(editedMeal);
        meals.add(editedMeal);
        return verifyMeals();
    }

    /**
     * TODO: write this method
     * removes meal.
     *
     * @param meal meal being removed
     * @return true if meal settings are valid
     */
    public static boolean removeMeal(Meal meal) {
        meals.remove(meal);
        return verifyMeals();
    }

    /**
     * TODO: write this method
     * checks meal settings for a valid simulation
     *
     * @return true if meal settings are correct
     */
    public static boolean verifyMeals() {
        double totalDist = 0.0;
        if (meals.isEmpty()) {
            return false;
        }
        for (Meal meal : meals) {
            if (meal.getWeight() > droneCapacity) {
                return false;
            }
            if (meal.getFoodItems().isEmpty()) {
                return false;
            }
            totalDist += meal.getDistribution();
        }
        if (totalDist != 1) {
            return false;
        }
        return true;
    }

    // Order Distributions ------------------------------------------------------

    /**
     * TODO: write this method
     * edits the distribution of meals
     *
     * @param index        index of distribution
     * @param distribution number of meals to be generated in that hour
     * @return true if valid settings
     */
    public static  boolean editDistribution(int index, int distribution) {
        if (index >= orderDistribution.size()) {
            return false;
        }
        orderDistribution.set(index, distribution);
        return true;
    }

    /**
     * TODO: write this method
     * verifies the order distribution
     *
     * @return true if valid settings
     */
    public static boolean verifyDistribution() {
        return true;
    }

    // Map ----------------------------------------------------------------------

    /**
     * TODO: write this method
     * adds map point
     *
     * @param x
     * @param y
     * @param dist
     */
    private void addMapPoint(int x, int y, float dist) {

    }

    /**
     * TODO: write ths method
     * removes map point
     *
     * @param x
     * @param y
     */
    private void removeMapPoint(int x, int y) {

    }

    /**
     * TODO: write this method
     * edits map point
     *
     * @param x
     * @param y
     * @param dist
     */
    private void editMapPoint(int x, int y, float dist) {

    }

    /**
     * TODO: write this method
     * exports map
     */
    private void exportMap() {

    }

    /**
     * TODO: write this method
     * imports map
     */
    private void importMap() {

    }

    /**
     * TODO: write this method
     * verifies the map
     *
     * @return
     */
    private boolean verifyMap() {

        return false;
    }

    // IO -----------------------------------------------------------------------

    /**
     * TODO: write method
     * parses settings and returns string representation of the object to be printed to a file
     *
     * @return string representation of object
     */
    private static String unparseSettings() {
        return "testing";
    }

    private static boolean validateFile(File file) {
        boolean validFile = true;

        try {
            Scanner s = new Scanner(file);
            while (s.hasNextLine()) {
                Scanner line = new Scanner(s.nextLine());
                String tag = line.next();

                if (tag.equals("<d>")) {
                    if (!line.hasNext()) {
                        validFile = false;
                    }
                    line.next();
                    if (!line.hasNextInt()) {
                        validFile = false;
                    }
                    line.next();
                    if (!line.hasNextInt()) {
                        validFile = false;
                    }
                    line.next();
                    if (!line.hasNextDouble()) {
                        validFile = false;
                    }
                    line.next();
                    if (line.hasNext()) {
                        validFile = false;
                    }
                } else if (tag.equals("<f>")) {
                    if (!line.hasNext()) {
                        validFile = false;
                    }
                    line.next();
                    if (!line.hasNextInt()) {
                        validFile = false;
                    }
                    line.next();
                    if (line.hasNext()) {
                        validFile = false;
                    }
                } else if (tag.equals("<m>")) {
                    if (!line.hasNext()) {
                        validFile = false;
                    }
                    line.next();
                    if (!line.hasNextInt()) {
                        validFile = false;
                    }
                    line.next();
                    if (!line.hasNextFloat()) {
                        validFile = false;
                    }
                    line.next();
                    while (line.hasNext()) {
                        line.next();
                        if (!line.hasNextInt()) {
                            validFile = false;
                        }
                        line.next();
                    }
                } else {
                    validFile = false;
                    break;
                }
            }
            s.close();
        } catch (Exception e) {
            System.out.println((e.getMessage()));
        }
        return validFile;
    }

    /**
     * parses the given file and updates settings objects accordingly
     *
     * @param settingsFile settings file
     * @return true if successful, false otherwise
     */
    private static boolean parseSettings(File settingsFile) {
//        if (!validateFile(settingsFile)) {
//            parseSettings(new File(defaultSettingsPath));
//            return false;
//        }

        try {
            //Names.txt was generated by Dominic Tarr
            Scanner s = new Scanner(settingsFile);

            //For each line in the file
            while (s.hasNextLine()) {
                Scanner line = new Scanner(s.nextLine()); //Scanner for the given line
                line.useDelimiter("\t");
                String tag = line.next();
                if (tag.equals("<d>")) {
                    String name;
                    int x, y;
                    double dist;

                    //Get the attributes from the file
                    name = line.next();
                    x = line.nextInt();
                    y = line.nextInt();
                    dist = line.nextDouble();

                    //Create a new destination
                    map.add(new Destination(name, x, y, dist));
                } else if (tag.equals("<f>")) {
                    String name;
                    float weight;

                    //Get the attributes from the file
                    name = line.next();
                    weight = line.nextInt();

                    //Create a new food item
                    foods.add(new FoodItem(name, weight));
                } else if (tag.equals("<m>")) {

                    String name;
                    int id;
                    float distribution;

                    //gets the attributes from the file
                    name = line.next();
                    id = line.nextInt();
                    distribution = line.nextFloat();

                    //creates a meal item
                    Meal combo = new Meal(name, id);
                    combo.setDistribution(distribution);
                    while (line.hasNext()) {
                        String food = line.next();
                        int quantity = line.nextInt();
                        //finds food so it can add it to the combo
                        for (Iterator<FoodItem> it = foods.iterator(); it.hasNext(); ) {
                            FoodItem f = it.next();
                            if (f.getName().toLowerCase().equals(food.toLowerCase())) {
                                combo.incrementFoodItem(f, quantity);
                            }
                        }
                    }
                    meals.add(combo);
                } else if(tag.equals("<o>")){
                    while(line.hasNextInt())
                        orderDistribution.add(line.nextInt());
                }


                line.close();
            }
            s.close();
            return true;
        } catch (Exception e) {
            System.out.println((e.getMessage()));
            return false;
        }
    }

    /**
     * opens the file browser and returns the file chosen by the user
     *
     * @param stage current stage of the application
     * @return true if imported file. false otherwise
     */
    public static boolean importSettings(Stage stage) {
        fileChooser.setTitle("Import Settings");
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            // TODO: call method to parse the file and import into settings
            return parseSettings(file);
        }
        return false;
    }

    /**
     * exports the current settings to the chosen file by the user
     *
     * @param stage current stage of the application
     * @return true if saved file. false otherwise
     */
    public static boolean exportSettings(Stage stage) {
        fileChooser.setTitle("Export Settings");
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            //TODO: pipe this string into the file
            try {
                FileWriter fw = new FileWriter(file, false);
                PrintWriter pw = new PrintWriter(fw);
                pw.println(unparseSettings());

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
}