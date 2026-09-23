package org.firstinspires.ftc.teamcode.kernel;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.kernel.errors.NoActiveOpModePanic;

final class ActiveOpMode {
    private ActiveOpMode() {}

    static OpModeManagerImpl manager() {
        OpModeManagerImpl manager = OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().getActivity());
        if (manager == null) throw new NoActiveOpModePanic();
        return manager;
    }

    static OpMode require() {
        OpModeManagerImpl manager = manager();
        OpMode opMode = manager.getActiveOpMode();
        if (opMode == null || OpModeManagerImpl.DEFAULT_OP_MODE_NAME.equals(manager.getActiveOpModeName())) {
            throw new NoActiveOpModePanic();
        }
        return opMode;
    }
}
