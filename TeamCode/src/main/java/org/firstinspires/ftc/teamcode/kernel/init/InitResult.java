package org.firstinspires.ftc.teamcode.kernel.init;

public final class InitResult {
    public enum Status { OK, WARN, FAIL }

    private static final InitResult PLAIN_OK = new InitResult(Status.OK, "");

    public final Status status;
    public final String detail;

    private InitResult(Status status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public static InitResult ok() {
        return PLAIN_OK;
    }

    public static InitResult ok(String detail) {
        return new InitResult(Status.OK, detail);
    }

    public static InitResult warn(String detail) {
        return new InitResult(Status.WARN, detail);
    }

    public static InitResult fail(String detail) {
        return new InitResult(Status.FAIL, detail);
    }
}
