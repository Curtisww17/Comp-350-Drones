package menu;

import java.io.File;
import java.util.*;

public class DefaultFood {
	
	//Data Structures to hold each collection of food, meals
	private Set<FoodItem> foods;
	private List<Meal> meals;
	private ArrayList<Destination> map;
	
	
	/**
	 * DefaultFood():
	 * generates a default list of foods and meals according to project description, 
	 * adding them to respective data structures.
	 */
	public DefaultFood() {
		foods = new HashSet<FoodItem>();
		meals = new ArrayList<Meal>();
		createMap("GCCMap.txt");

		
		//Creates three default food items, and adds them to the foods set
		FoodItem hamburger = new FoodItem("Hamburger", 6);
		FoodItem fries = new FoodItem("Fries", 4);
		FoodItem drink = new FoodItem("12 oz. drink", 14);
		foods.add(hamburger);
		foods.add(fries);
		foods.add(drink);
		
		
		//A meal with a hamburger, fries, and drink
		Meal combo1 = new Meal("Combo 1", 0);
		combo1.addFoodItem(hamburger);
		combo1.addFoodItem(fries);
		combo1.addFoodItem(drink);
		meals.add(combo1);
		combo1.setDistribution(.55f);
		
		
		//A meal with two hamburgers, fries, and drink
		Meal combo2 = new Meal("Combo 2", 1);
		combo2.addFoodItem(hamburger, 2);
		combo2.addFoodItem(fries);
		combo2.addFoodItem(drink);
		meals.add(combo2);
		combo2.setDistribution(.10f);
		
		
		//A meal with a burger and fries
		Meal combo3 = new Meal("Combo 3", 2);
		combo3.addFoodItem(hamburger);
		combo3.addFoodItem(fries);
		//combo3.addFoodItem(drink);
		meals.add(combo3);
		combo3.setDistribution(.20f);
		
		//A meal with 2 burgers and fries
		Meal combo4 = new Meal("Combo 4", 3);
		combo4.addFoodItem(hamburger, 2);
		combo4.addFoodItem(fries);
		//combo4.addFoodItem(drink);
		meals.add(combo4);
		combo4.setDistribution(.15f);
		
	}
	
	public Set<FoodItem> getFoods() {
		return foods;
	}
	
	public List<Meal> getMeals() {
		return meals;
	}
	
	public boolean createMap(String fileName) {
		map = new ArrayList<Destination>();
		try {
			File mapFile = new File(fileName);
			Scanner s = new Scanner(mapFile);

			String name;
			int x, y;
			double dist;


			while (s.hasNextLine()) {
				Scanner line = new Scanner(s.nextLine());
				name = line.next();
				x = line.nextInt();
				y = line.nextInt();
				dist = line.nextDouble();
				map.add(new Destination(name, x, y, dist));
				line.close();
			}
			s.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
		return true;
	}

	public ArrayList<Destination> getMap() {
		return map;
	}

	public String mapToString() {
		String output = "";
		for (int i = 0; i < map.size(); i++) {
			output += map.get(i) + "\n";
		}
		return output;
	}

}
