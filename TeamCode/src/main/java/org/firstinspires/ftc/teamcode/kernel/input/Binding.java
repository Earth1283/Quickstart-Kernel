package org.firstinspires.ftc.teamcode.kernel.input;

final class Binding {
    interface Step {
        void evaluate(long nowNanos) throws Exception;
    }

    final String description;
    final Step step;

    Binding(String description, Step step) {
        this.description = description;
        this.step = step;
    }
}
