package com.cozy.gateway.dto;

public class InviteCodeValidationResult {

    private boolean valid;
    private String inviterNickname;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getInviterNickname() {
        return inviterNickname;
    }

    public void setInviterNickname(String inviterNickname) {
        this.inviterNickname = inviterNickname;
    }
}
