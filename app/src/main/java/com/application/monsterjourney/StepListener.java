package com.application.monsterjourney;


public interface StepListener {
    /**
     * Listens to step alerts
     * @param timeNs time in Nanoseconds of the step
     */

    public void step(long timeNs);

}
