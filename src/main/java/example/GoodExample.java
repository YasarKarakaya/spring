package example;

import org.springframework.stereotype.Service;

@Service
public class GoodExample {

    public String greetUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, Guest!";
        }
        return "Hello, " + name.trim() + "!";
    }

    public int calculateSum(int[] numbers) {
        if (numbers == null || numbers.length == 0) return 0;

        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return sum;
    }
}

