package com.application.monsterjourney;

import android.view.View;

import java.util.List;

public class RanchContainer {
    /**
     * contains the imageview and position information for the animation
     */
    private View imageView;
    private float width,height;
    private boolean rightface, downface, isoverlapping, selected;
    private float imagewidth, imageheight;

    RanchContainer(View imageView, float imagewidth, float imageheight){
        width = 0;
        height = 0;
        this.imageView = imageView;
        rightface = true;
        downface = true;
        this.imagewidth = imagewidth;
        this.imageheight = imageheight;
        isoverlapping = false;
        selected = false;
    }

    public View getImageView(){
        return  imageView;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height){
        this.height = height;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public void setRightface(boolean rightface) {
        this.rightface = rightface;
    }

    public void setDownface(boolean downface) {
        this.downface = downface;
    }

    public boolean isDownface() {
        return downface;
    }

    public boolean isRightface() {
        return rightface;
    }

    public float getImagewidth(){
        return imagewidth;
    }

    public float getImageheight(){
        return  imageheight;
    }

    public boolean isIsoverlapping(){return isoverlapping;}

    public void setOverlapping(boolean isoverlapping){this.isoverlapping = isoverlapping;}

    public void setSelected(boolean selected){
        this.selected = selected;
    }
    public  boolean isSelected(){return selected;}

    /**
     * compares the view with the other position and reverses direction if needed
     * @param othercontainer the view we are comparing to
     */
    public void compareView(List<RanchContainer> othercontainer){
        boolean check = false;
        for(RanchContainer ranchContainer: othercontainer){
            check = check || checkOverlap(ranchContainer);
        }
        if(check){
            rightface = !rightface;
            downface = !downface;
        }
    }

    public boolean checkOverlap(RanchContainer othercontainer){
        if(othercontainer.getImageView().equals(imageView)){
            return false;
        }
        if((width >= othercontainer.getWidth()+othercontainer.getImagewidth()) ||
                (othercontainer.getWidth() >= width + imagewidth)){
            isoverlapping = false;
        }
        if((height >= othercontainer.getHeight() + othercontainer.getImageheight()) ||
                (othercontainer.getHeight() >= height + imageheight)){
            isoverlapping = false;
        }
        if((width >= othercontainer.getWidth()+othercontainer.getImagewidth()/2) ||
                (othercontainer.getWidth() >= width + imagewidth/2)){
            return false;
        }
        if((height >= othercontainer.getHeight() + othercontainer.getImageheight()/2) ||
                (othercontainer.getHeight() >= height + imageheight/2)){
            return false;
        }
        isoverlapping = true;
        othercontainer.setOverlapping(true);
        return true;
    }




}
