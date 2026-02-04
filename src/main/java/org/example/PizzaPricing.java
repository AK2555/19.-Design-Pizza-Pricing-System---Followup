package org.example;


import java.util.*;
import java.util.concurrent.*;

public class PizzaPricing {
    Pizza pizza;
    TaxEngine taxEngine;
    ValidatorEngine validatorEngine;
    int baseTax;
    Map<String, Integer> map = new HashMap<String, Integer>() {{
        put("cheeseburst", 100);
        put("corn", 50);
        put("onion", 30);
        put("capsicum", 50);
        put("pineapple", 60);
        put("mushroom", 40);
    }};

    public PizzaPricing(int basePrice, int taxPercentage, String size) {
        this.pizza = new BasePizza(basePrice, size);
        taxEngine = new TaxEngine(Arrays.asList(new CheeseBurstTaxRule(), new MushroomTaxRule()));
        validatorEngine = new ValidatorEngine(Arrays.asList(
                new CheeseBurstValidator(),
                new MushroomValidator(),
                new PineappleValidator()
        ));
        baseTax = taxPercentage;
    }

    public boolean addTopping(String topping, int servingsCount) {
        if (!validatorEngine.validate(pizza, topping, servingsCount)) {
            return false;
        }

        int previousCount = pizza.getToppingCount(topping);
        pizza.addTopping(topping, servingsCount);

        if ("corn".equals(topping)) {
            pizza = new CornDecorator(pizza, servingsCount, previousCount);
        } else {
            pizza = new RestToppingDecorator(pizza, map.get(topping), servingsCount);
        }

        return true;
    }

    public int getFinalPrice() {
        int subTotal = pizza.findSubTotal();
        double tax = taxEngine.calculateTax(baseTax, pizza);
        double totalPrice = subTotal + (subTotal * tax / 100.0);
        return (int) (totalPrice + 0.5); // round-half-up
    }
}

// ----------------- Validator Engine -----------------
class ValidatorEngine {
    List<ToppingValidator> list;

    public ValidatorEngine(List<ToppingValidator> list) {
        this.list = list;
    }

    public boolean validate(Pizza pizza, String topping, int servingsCount) {
        for (ToppingValidator validator : list) {
            if (!validator.validate(pizza, topping, servingsCount)) return false;
        }
        return true;
    }
}

interface ToppingValidator {
    boolean validate(Pizza pizza, String currentTopping, int servingsCount);
}

class PineappleValidator implements ToppingValidator {
    @Override
    public boolean validate(Pizza pizza, String currentTopping, int servingsCount) {
        if (!"pineapple".equals(currentTopping)) return true;
        return !"small".equals(pizza.getPizzaSize());
    }
}

class MushroomValidator implements ToppingValidator {
    @Override
    public boolean validate(Pizza pizza, String currentTopping, int servingsCount) {
        if (!"mushroom".equals(currentTopping)) return true;
        return !pizza.isToppingPresent("cheeseburst");
    }
}

class CheeseBurstValidator implements ToppingValidator {
    @Override
    public boolean validate(Pizza pizza, String currentTopping, int servingsCount) {
        if (!"cheeseburst".equals(currentTopping)) return true;
        if (pizza.isToppingPresent("mushroom")) return false;

        int maxAllowed;
        if ("small".equals(pizza.getPizzaSize())) maxAllowed = 1;
        else if ("medium".equals(pizza.getPizzaSize())) maxAllowed = 2;
        else maxAllowed = Integer.MAX_VALUE;

        int currentCount = pizza.getToppingCount("cheeseburst");
        return currentCount + servingsCount <= maxAllowed;
    }
}

// ----------------- Tax Engine -----------------
interface TaxRule {
    double calculateTax(Pizza pizza, double tax);
}

class TaxEngine {
    List<TaxRule> rules;

    public TaxEngine(List<TaxRule> rules) {
        this.rules = rules;
    }

    public double calculateTax(int baseTax, Pizza pizza) {
        double finalTax = baseTax;
        for (TaxRule rule : rules) {
            finalTax = rule.calculateTax(pizza, finalTax);
        }
        return finalTax;
    }
}

class MushroomTaxRule implements TaxRule {
    @Override
    public double calculateTax(Pizza pizza, double tax) {
        if (pizza.isToppingPresent("mushroom")) {
            return tax - (tax * 10 / 100.0);
        }
        return tax;
    }
}

class CheeseBurstTaxRule implements TaxRule {
    @Override
    public double calculateTax(Pizza pizza, double tax) {
        if (pizza.isToppingPresent("cheeseburst")) {
            return tax + (tax * 30 / 100.0);
        }
        return tax;
    }
}

// ----------------- Pizza and Decorators -----------------
interface Pizza {
    int findSubTotal();
    boolean isToppingPresent(String topping);
    void addTopping(String topping, int servingCount);
    String getPizzaSize();
    int getToppingCount(String topping);
}

class BasePizza implements Pizza {
    private int basePrice;
    private String size;
    private Map<String, Integer> toppings;

    public BasePizza(int basePrice, String size) {
        this.basePrice = basePrice;
        this.size = size;
        this.toppings = new ConcurrentHashMap<>();
    }

    @Override
    public int findSubTotal() {
        return basePrice;
    }

    @Override
    public boolean isToppingPresent(String topping) {
        return toppings.containsKey(topping);
    }

    @Override
    public void addTopping(String topping, int servingCount) {
        toppings.put(topping, toppings.getOrDefault(topping, 0) + servingCount);
    }

    @Override
    public String getPizzaSize() {
        return size;
    }

    @Override
    public int getToppingCount(String topping) {
        return toppings.getOrDefault(topping, 0);
    }
}

abstract class ToppingDecorator implements Pizza {
    protected Pizza pizza;
    protected ToppingDecorator(Pizza pizza) {
        this.pizza = pizza;
    }
}

class RestToppingDecorator extends ToppingDecorator {
    private final int costToAdd;

    public RestToppingDecorator(Pizza pizza, int perServingCost, int servingsAdded) {
        super(pizza);
        this.costToAdd = perServingCost * servingsAdded;
    }

    @Override
    public int findSubTotal() {
        return pizza.findSubTotal() + costToAdd;
    }

    @Override
    public boolean isToppingPresent(String topping) {
        return pizza.isToppingPresent(topping);
    }

    @Override
    public void addTopping(String topping, int servingCount) {
        pizza.addTopping(topping, servingCount);
    }

    @Override
    public String getPizzaSize() {
        return pizza.getPizzaSize();
    }

    @Override
    public int getToppingCount(String topping) {
        return pizza.getToppingCount(topping);
    }
}

class CornDecorator extends ToppingDecorator {
    private final int costToAdd;

    public CornDecorator(Pizza pizza, int newServings, int previousCorn) {
        super(pizza);
        String size = pizza.getPizzaSize();
        if ("medium".equals(size)) {
            if (previousCorn == 0) {
                costToAdd = newServings == 1 ? 50 : 50 + (newServings - 1) * 40;
            } else {
                costToAdd = newServings * 40;
            }
        } else if ("large".equals(size)) {
            costToAdd = newServings * 20;
        } else { // small
            costToAdd = newServings * 50;
        }
    }

    @Override
    public int findSubTotal() {
        return pizza.findSubTotal() + costToAdd;
    }

    @Override
    public boolean isToppingPresent(String topping) {
        return pizza.isToppingPresent(topping);
    }

    @Override
    public void addTopping(String topping, int servingCount) {
        pizza.addTopping(topping, servingCount);
    }

    @Override
    public String getPizzaSize() {
        return pizza.getPizzaSize();
    }

    @Override
    public int getToppingCount(String topping) {
        return pizza.getToppingCount(topping);
    }
}
