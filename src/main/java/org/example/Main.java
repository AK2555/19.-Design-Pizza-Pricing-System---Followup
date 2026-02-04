package org.example;

public class Main {
    public static void main(String[] args) {

                // Step 1: Initialize pizza
                PizzaPricing p = new PizzaPricing(200, 10, "small");

                int price1 = p.getFinalPrice();   // expected 220
                System.out.println("PRICE 1 (no toppings): " + price1);

                // Step 2: Add 1 mushroom
                boolean m1 = p.addTopping("mushroom", 1);  // expected true
                System.out.println("Add mushroom(1): " + m1);

                int price2 = p.getFinalPrice();   // expected 262
                System.out.println("PRICE 2 (after mushroom): " + price2);

                // Step 3: Try adding cheeseburst (should fail)
                boolean cb = p.addTopping("cheeseburst", 1);  // expected false
                System.out.println("Add cheeseburst(1): " + cb);

                int price3 = p.getFinalPrice();   // expected 262 (unchanged)
                System.out.println("PRICE 3 (after failed cheeseburst): " + price3);

                // Step 4: Add 3 onions
                boolean o1 = p.addTopping("onion", 3);  // expected true
                System.out.println("Add onion(3): " + o1);

                int price4 = p.getFinalPrice();   // expected 360
                System.out.println("PRICE 4 (after onion): " + price4);

                // Step 5: Add 2 more mushrooms
                boolean m2 = p.addTopping("mushroom", 2);  // expected true
                System.out.println("Add mushroom(2): " + m2);

                int price5 = p.getFinalPrice();   // expected 447
                System.out.println("PRICE 5 (final): " + price5);

    }
}