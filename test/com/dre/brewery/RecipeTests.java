package com.dre.brewery;

import com.dre.brewery.api.BreweryApi;
import com.dre.brewery.recipe.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RecipeTests {

	public static void testCauldronRecipe() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta itemMeta = item.getItemMeta();
		List<String> l = new ArrayList<>();
		l.add("Eine Tür");
		l.add("§6Besonders gut geschützt");
		itemMeta.setLore(l);
		itemMeta.setDisplayName("Mauer");
		item.setItemMeta(itemMeta);

		BRecipe recipe = BRecipe.get("Beispiel");
		int x = recipe.amountOf(item);
		int y = recipe.amountOf(new ItemStack(Material.NETHER_BRICK));


		List<Ingredient> list = new ArrayList<>();
		Ingredient ing = new SimpleItem(Material.DIAMOND_HOE);
		ing.setAmount(3);
		list.add(ing);
		ing = new SimpleItem(Material.RED_MUSHROOM);
		list.add(ing);
		for (int i = 1; i < 20; i++) {
			list.get(0).setAmount(i + 3);
			list.get(1).setAmount(i);
			BCauldronRecipe best = null;
			float bestMatch = 0;
			float match;
			for (BCauldronRecipe r : BCauldronRecipe.recipes) {
				match = r.getIngredientMatch(list);
				if (match >= 10) {
					P.p.debugLog("Found match 10 Recipe: " + r);
					return;
				}
				if (match > bestMatch) {
					best = r;
					bestMatch = match;
				}
			}
			P.p.debugLog("Found best for i:" + i + " " + best);
		}

		item = new ItemStack(Material.BARRIER);
		itemMeta = item.getItemMeta();
		l = new ArrayList<>();
		l.add("Eine Tür");
		l.add("§6Besonders gut geschützt");
		itemMeta.setLore(l);
		itemMeta.setDisplayName("Mauer");
		item.setItemMeta(itemMeta);

		RecipeItem.getMatchingRecipeItem(item, false);
	}

	public static void testCustomRecipe() {
		BreweryApi.removeRecipe("Good Build");
		BRecipe recipe = BreweryApi.recipeBuilder("Bad Build", "Good Build", "Uber Build")
			.color(PotionColor.PINK)
			.addIngredient(new ItemStack(Material.FLOWER_POT))
			.alcohol(32)
			.cook(3)
			.difficulty(4)
			.age(3, (byte) 0)
			.get();
		BreweryApi.addRecipe(recipe, false);

		P.p.log(BRecipe.getConfigRecipes().size() + "");

		BreweryApi.removeRecipe("Bier");

		P.p.log(BRecipe.getConfigRecipes().size() + "");

		BCauldronRecipe r = BreweryApi.cauldronRecipeBuilder("Cooler Trank")
			.color(PotionColor.PINK)
			.addIngredient(new SimpleItem(Material.FLOWER_POT))
			.addLore("Schmeckt nAcH TOn?!")
			.get();
		BreweryApi.addCauldronRecipe(r, false);
	}
}