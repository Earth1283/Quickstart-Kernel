package org.firstinspires.ftc.teamcode.kernel.init;

import java.util.function.Supplier;

public interface InitTask {
    String name();

    InitResult run() throws Exception;

    // A failed critical task aborts new Robot() after every task has had its say.
    default boolean critical() {
        return false;
    }

    static InitTask of(String name, Supplier<InitResult> body) {
        return named(name, false, body);
    }

    static InitTask critical(String name, Supplier<InitResult> body) {
        return named(name, true, body);
    }

    static InitTask named(String name, boolean critical, Supplier<InitResult> body) {
        return new InitTask() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public InitResult run() {
                return body.get();
            }

            @Override
            public boolean critical() {
                return critical;
            }
        };
    }
}
