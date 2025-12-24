package model;
import com.google.gson.Gson;

import java.util.Date;

public class User {
    private int id; // 会员ID
    private String loginName; // 登录名（手机号码或邮箱）
    private String passwordHash; // 经过哈希处理的密码
    private String email; // 邮箱
    private String phoneNumber; // 手机号码
    private String nickname; // 用户昵称
    private String invitationCode; // 邀请码
    private String avatar;
    private String memberLevel;
    private int totalPoints;
    private int currentPoints;
    private Date levelExpireTime;
    private Date lastSigninDate;

    public String toJson() {
        return new Gson().toJson(this);
    }


    // 无参构造函数
    public User() {
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(String memberLevel) {
        this.memberLevel = memberLevel;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }

    public Date getLevelExpireTime() {
        return levelExpireTime;
    }

    public void setLevelExpireTime(Date levelExpireTime) {
        this.levelExpireTime = levelExpireTime;
    }

    public Date getLastSigninDate() {
        return lastSigninDate;
    }

    public void setLastSigninDate(Date lastSigninDate) {
        this.lastSigninDate = lastSigninDate;
    }
}