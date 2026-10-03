package org.firstinspires.ftc.teamcode.kernel.errors;

import org.firstinspires.ftc.teamcode.kernel.init.InitLog;

import java.util.List;

public class InitFailedPanic extends KernelPanic {
    public InitFailedPanic(List<InitLog.Entry> failed) {
        super(describe(failed));
    }

    private static String describe(List<InitLog.Entry> failed) {
        StringBuilder message = new StringBuilder("Critical init task failed:");
        for (InitLog.Entry entry : failed) {
            message.append("\n  ").append(entry.name).append(": ").append(entry.result.detail);
        }
        return message.toString();
    }
}
