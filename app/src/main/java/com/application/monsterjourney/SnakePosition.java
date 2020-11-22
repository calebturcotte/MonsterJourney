package com.application.monsterjourney;

public class SnakePosition {
    /**
     * container for x and y position of created view
     */
    private int xposition;
    private int yposition;
    private int viewid;


    public SnakePosition(int xposition, int yposition, int viewid){
        this.xposition = xposition;
        this.yposition = yposition;
        this.viewid = viewid;
    }

    public int getXposition(){return xposition;}
    public void setXposition(int xposition){this.xposition = xposition;}

    public int getYposition(){return yposition;}
    public void setYposition(int yposition){this.yposition = yposition;}

    public int getViewid(){return viewid;}
}
