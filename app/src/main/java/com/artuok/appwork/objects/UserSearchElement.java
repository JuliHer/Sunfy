package com.artuok.appwork.objects;

import android.graphics.Bitmap;

public class UserSearchElement {

    private String name;
    private String desc;
    private String uId;
    private String imageName;
    private Bitmap image;
    private String code;

    private String followers;
    private String following;


    public UserSearchElement(String uId, String name, String desc, String followers, String following, String imageName, Bitmap image) {
        this.name = name;
        this.desc = desc;
        this.uId = uId;
        this.imageName = imageName;
        this.image = image;
        this.followers = followers;
        this.following = following;
    }

    public String getImageName() {
        return imageName;
    }

    public String getFollowers() {
        return followers;
    }

    public String getFollowing() {
        return following;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getId() {
        return uId;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image){
        this.image = image;
    }
}
