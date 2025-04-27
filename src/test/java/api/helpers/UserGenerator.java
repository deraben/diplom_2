package api.helpers;

import api.models.User;
import org.apache.commons.lang3.RandomStringUtils;

public class UserGenerator {

    public static User randomUser() {
        String email = RandomStringUtils.randomAlphanumeric(10) + "@test.com";
        String password = RandomStringUtils.randomAlphanumeric(10);
        String name = RandomStringUtils.randomAlphabetic(10);
        return new User(email, password, name);
    }

    public static User userWithoutEmail() {
        String password = RandomStringUtils.randomAlphanumeric(10);
        String name = RandomStringUtils.randomAlphabetic(10);
        return new User(null, password, name);
    }

    public static User userWithoutPassword() {
        String email = RandomStringUtils.randomAlphanumeric(10) + "@test.com";
        String name = RandomStringUtils.randomAlphabetic(10);
        return new User(email, null, name);
    }

    public static User userWithoutName() {
        String email = RandomStringUtils.randomAlphanumeric(10) + "@test.com";
        String password = RandomStringUtils.randomAlphanumeric(10);
        return new User(email, password, null);
    }
}