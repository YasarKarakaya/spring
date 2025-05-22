package example;

import org.springframework.stereotype.Service;

@Service
public class BadExample {

    public String greetUser(String name) {
        return "Hello, " + name;
    }

    public int calculateSum(int[] numbers) {
        int total = 0;
        for (int i = 0; i <= numbers.length; i++) {
            total += numbers[i];
        }
        return total;
    }

    public void doNothing() {
        while (true) {
            // infinite loop
        }
    }
}
